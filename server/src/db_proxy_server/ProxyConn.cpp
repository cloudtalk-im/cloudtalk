/*
 * ProxyConn.cpp
 *
 *  Created on: 2014年7月25日
 *      Author: ziteng
 */

#include "ProxyConn.h"
#include "ProxyTask.h"
#include "HandlerMap.h"
#include "atomic.h"
#include "IM.Other.pb.h"
#include "IM.BaseDefine.pb.h"
#include "IM.Server.pb.h"
#include "ThreadPool.h"
#include "SyncCenter.h"
#include "CachePool.h"

using namespace IM::BaseDefine;
static ConnMap_t g_proxy_conn_map;
static UserMap_t g_uuid_conn_map;
static CHandlerMap* s_handler_map;

uint32_t CProxyConn::s_uuid_alloctor = 0;
CLock CProxyConn::s_list_lock;
list<ResponsePdu_t*> CProxyConn::s_response_pdu_list;
static CThreadPool g_thread_pool;
static ConnMap_t g_client_conn_map;
static ConnMap_t g_msg_serv_conn_map;
static uint32_t g_total_online_user_cnt = 0;	// 并发在线总人数
map<uint32_t, msg_serv_info_t*> g_msg_serv_info;




void proxy_timer_callback(void* callback_data, uint8_t msg, uint32_t handle, void* pParam)
{
	uint64_t cur_time = get_tick_count();
	for (ConnMap_t::iterator it = g_proxy_conn_map.begin(); it != g_proxy_conn_map.end(); ) {
		ConnMap_t::iterator it_old = it;
		it++;

		CProxyConn* pConn = (CProxyConn*)it_old->second;
		pConn->OnTimer(cur_time);
	}
}

//
void proxy_loop_callback(void* callback_data, uint8_t msg, uint32_t handle, void* pParam)
{
	CProxyConn::SendResponsePduList();
}

/*
 * 用于优雅的关闭连接：
 * 服务器收到SIGTERM信号后，发送CImPduStopReceivePacket数据包给每个连接，
 * 通知消息服务器不要往自己发送数据包请求，
 * 然后注册4s后调用的回调函数，回调时再退出进程
 */
void exit_callback(void* callback_data, uint8_t msg, uint32_t handle, void* pParam)
{
	log("exit_callback...");
	exit(0);
}

static void sig_handler(int sig_no)
{
	if (sig_no == SIGTERM) {
		log("receive SIGTERM, prepare for exit");
        CImPdu cPdu;
        IM::Server::IMStopReceivePacket msg;
        msg.set_result(0);
        cPdu.SetPBMsg(&msg);
        cPdu.SetServiceId(IM::BaseDefine::SID_OTHER);
        cPdu.SetCommandId(IM::BaseDefine::CID_OTHER_STOP_RECV_PACKET);
        for (ConnMap_t::iterator it = g_proxy_conn_map.begin(); it != g_proxy_conn_map.end(); it++) {
            CProxyConn* pConn = (CProxyConn*)it->second;
            pConn->SendPdu(&cPdu);
        }
        // Add By ZhangYuanhao
        // Before stop we need to stop the sync thread,otherwise maybe will not sync the internal data any more
        CSyncCenter::getInstance()->stopSync();
        
        // callback after 4 second to exit process;
		netlib_register_timer(exit_callback, NULL, 4000);
	}
}

int init_proxy_conn(uint32_t thread_num)
{
	s_handler_map = CHandlerMap::getInstance();
	g_thread_pool.Init(thread_num);

	netlib_add_loop(proxy_loop_callback, NULL);

	signal(SIGTERM, sig_handler);

	return netlib_register_timer(proxy_timer_callback, NULL, 1000);
}


CProxyConn* get_proxy_conn_by_uuid(uint32_t uuid)
{
	CProxyConn* pConn = NULL;
	UserMap_t::iterator it = g_uuid_conn_map.find(uuid);
	if (it != g_uuid_conn_map.end()) {
		pConn = (CProxyConn *)it->second;
	}

	return pConn;
}

//////////////////////////
CProxyConn::CProxyConn()
{
	m_uuid = ++CProxyConn::s_uuid_alloctor;
	if (m_uuid == 0) {
		m_uuid = ++CProxyConn::s_uuid_alloctor;
	}

	g_uuid_conn_map.insert(make_pair(m_uuid, this));
}

CProxyConn::~CProxyConn()
{

}

void CProxyConn::Close()
{
	if (m_handle != NETLIB_INVALID_HANDLE) {
		netlib_close(m_handle);
		g_proxy_conn_map.erase(m_handle);
		g_uuid_conn_map.erase(m_uuid);
		g_msg_serv_conn_map.erase(m_handle);

		// remove all user count from this message server 当消息服务顺离线，将清空消息服务器的所有用户
		map<uint32_t, msg_serv_info_t*>::iterator it = g_msg_serv_info.find(m_handle);
		if (it != g_msg_serv_info.end()) {
			msg_serv_info_t* pMsgServInfo = it->second;
			g_total_online_user_cnt -= pMsgServInfo->cur_conn_cnt;
			log("onclose from MsgServer: %s:%u ", pMsgServInfo->hostname.c_str(), pMsgServInfo->port);

			CacheManager* pCacheManager = CacheManager::getInstance();
			CacheConn* pCacheConn = pCacheManager->GetCacheConn("msg_srv_list");
			if (pCacheConn)
			{
                string strKey = "msg_srv_list";
				pCacheConn->hdel(strKey,pMsgServInfo->ip_addr1+"|"+pMsgServInfo->ip_addr2+"|"+int2string(pMsgServInfo->port));
				pCacheManager->RelCacheConn(pCacheConn);
			}
			delete pMsgServInfo;
			g_msg_serv_info.erase(it);
		}
	}
	ReleaseRef();
}

//接收msg服务器发送过来的服务器信息列表
void CProxyConn::_HandledoRecMsgSrvInfo(CImPdu* pPdu)
{
	msg_serv_info_t* pMsgServInfo = new msg_serv_info_t;
	IM::Server::IMMsgServInfo msg;
	msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength());

	pMsgServInfo->ip_addr1 = msg.ip1();
	pMsgServInfo->ip_addr2 = msg.ip2();
	pMsgServInfo->port = msg.port();
	pMsgServInfo->max_conn_cnt = msg.max_conn_cnt();
	pMsgServInfo->cur_conn_cnt = msg.cur_conn_cnt();
	pMsgServInfo->hostname = msg.host_name();
	g_msg_serv_info.insert(make_pair(m_handle, pMsgServInfo));
	g_total_online_user_cnt += pMsgServInfo->cur_conn_cnt;
	log("MsgServInfo, ip_addr1=%s, ip_addr2=%s, port=%d, max_conn_cnt=%d, cur_conn_cnt=%d, "\
		"hostname: %s. ",
		pMsgServInfo->ip_addr1.c_str(), pMsgServInfo->ip_addr2.c_str(), pMsgServInfo->port,pMsgServInfo->max_conn_cnt,
		pMsgServInfo->cur_conn_cnt, pMsgServInfo->hostname.c_str());


	//将服务器相关信息。存到redis服务器中
	CacheManager* pCacheManager = CacheManager::getInstance();
	CacheConn* pCacheConn = pCacheManager->GetCacheConn("msg_srv_list");
	if (pCacheConn)
	{
        string strKey = "msg_srv_list";
		pCacheConn->hset(strKey, msg.ip1()+"|"+msg.ip2()+"|"+int2string(msg.port()), int2string(msg.cur_conn_cnt()));
		pCacheManager->RelCacheConn(pCacheConn);
	}

}

void CProxyConn::_HandleUserCntUpdate(CImPdu* pPdu)
{
	map<uint32_t, msg_serv_info_t*>::iterator it = g_msg_serv_info.find(m_handle);
	if (it != g_msg_serv_info.end()) {
		msg_serv_info_t* pMsgServInfo = it->second;
		IM::Server::IMMsgServInfo msg;
		msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength());
		pMsgServInfo->cur_conn_cnt = msg.cur_conn_cnt();

		g_total_online_user_cnt += pMsgServInfo->cur_conn_cnt;

		CacheManager* pCacheManager = CacheManager::getInstance();
		CacheConn* pCacheConn = pCacheManager->GetCacheConn("msg_srv_list");
		if (pCacheConn && pMsgServInfo->cur_conn_cnt>=0)
		{
			string strKey = "msg_srv_list";
			pCacheConn->hset(strKey, pMsgServInfo->ip_addr1+"|"+pMsgServInfo->ip_addr2+"|"+int2string(pMsgServInfo->port), int2string(pMsgServInfo->cur_conn_cnt));
			pCacheManager->RelCacheConn(pCacheConn);
		}

		log("%s:%d, cur_cnt=%u, total_cnt=%u ", pMsgServInfo->hostname.c_str(),
			pMsgServInfo->port, pMsgServInfo->cur_conn_cnt, g_total_online_user_cnt);

/*
		IM::Server::IMUserCntUpdate msg;
		msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength());

		uint32_t action = msg.user_action();
		if (action == USER_CNT_INC) {
			pMsgServInfo->cur_conn_cnt++;
			g_total_online_user_cnt++;
		} else {
			pMsgServInfo->cur_conn_cnt--;
			g_total_online_user_cnt--;
		}

		//将服务器相关信息。存到redis服务器中  实现在线人数的更新
		CacheManager* pCacheManager = CacheManager::getInstance();
		CacheConn* pCacheConn = pCacheManager->GetCacheConn("msg_srv_list");
		if (pCacheConn && pMsgServInfo->cur_conn_cnt>=0)
		{
			string strKey = "msg_srv_list";
			pCacheConn->hset(strKey, pMsgServInfo->ip_addr1+"|"+pMsgServInfo->ip_addr2+"|"+int2string(pMsgServInfo->port), int2string(pMsgServInfo->cur_conn_cnt));
			pCacheManager->RelCacheConn(pCacheConn);
		}

		log("%s:%d, cur_cnt=%u, total_cnt=%u ", pMsgServInfo->hostname.c_str(),
			pMsgServInfo->port, pMsgServInfo->cur_conn_cnt, g_total_online_user_cnt);
	*/

	}


}

void CProxyConn::OnConnect(net_handle_t handle)
{
	m_handle = handle;

	g_proxy_conn_map.insert(make_pair(handle, this));

	netlib_option(handle, NETLIB_OPT_SET_CALLBACK, (void*)imconn_callback);
	netlib_option(handle, NETLIB_OPT_SET_CALLBACK_DATA, (void*)&g_proxy_conn_map);
	netlib_option(handle, NETLIB_OPT_GET_REMOTE_IP, (void*)&m_peer_ip);
	netlib_option(handle, NETLIB_OPT_GET_REMOTE_PORT, (void*)&m_peer_port);

	log("connect from %s:%d, handle=%d", m_peer_ip.c_str(), m_peer_port, m_handle);
}

// 由于数据包是在另一个线程处理的，所以不能在主线程delete数据包，所以需要Override这个方法
void CProxyConn::OnRead()
{
	for (;;) {
		uint32_t free_buf_len = m_in_buf.GetAllocSize() - m_in_buf.GetWriteOffset();
		if (free_buf_len < READ_BUF_SIZE)
			m_in_buf.Extend(READ_BUF_SIZE);

		int ret = netlib_recv(m_handle, m_in_buf.GetBuffer() + m_in_buf.GetWriteOffset(), READ_BUF_SIZE);
		if (ret <= 0)
			break;

		m_recv_bytes += ret;
		m_in_buf.IncWriteOffset(ret);
		m_last_recv_tick = get_tick_count();
	}

	uint32_t pdu_len = 0;
    try {
        while ( CImPdu::IsPduAvailable(m_in_buf.GetBuffer(), m_in_buf.GetWriteOffset(), pdu_len) ) {
            HandlePduBuf(m_in_buf.GetBuffer(), pdu_len);
            m_in_buf.Read(NULL, pdu_len);
        }
    } catch (CPduException& ex) {
        log("!!!catch exception, err_code=%u, err_msg=%s, close the connection ",
            ex.GetErrorCode(), ex.GetErrorMsg());
        OnClose();
    }
	
}


void CProxyConn::OnClose()
{
	Close();
}

void CProxyConn::OnTimer(uint64_t curr_tick)
{
	if (curr_tick > m_last_send_tick + SERVER_HEARTBEAT_INTERVAL) {
        
        CImPdu cPdu;
        IM::Other::IMHeartBeat msg;
        cPdu.SetPBMsg(&msg);
        cPdu.SetServiceId(IM::BaseDefine::SID_OTHER);
        cPdu.SetCommandId(IM::BaseDefine::CID_OTHER_HEARTBEAT);
		SendPdu(&cPdu);
	}

	if (curr_tick > m_last_recv_tick + SERVER_TIMEOUT) {
		log("proxy connection timeout %s:%d", m_peer_ip.c_str(), m_peer_port);
		Close();
	}
}

void CProxyConn::HandlePduBuf(uchar_t* pdu_buf, uint32_t pdu_len)
{
    CImPdu* pPdu = NULL;
    pPdu = CImPdu::ReadPdu(pdu_buf, pdu_len);
    if (pPdu->GetCommandId() == IM::BaseDefine::CID_OTHER_HEARTBEAT) {
        return;
    }
    
    pdu_handler_t handler = s_handler_map->GetHandler(pPdu->GetCommandId());
    
    if (handler) {
        CTask* pTask = new CProxyTask(m_uuid, handler, pPdu);
        g_thread_pool.AddTask(pTask);
    } else {

		switch (pPdu->GetCommandId()) {
			case CID_OTHER_MSG_SERV_INFO:
				_HandledoRecMsgSrvInfo(pPdu);
				break;
			case CID_OTHER_USER_CNT_UPDATE:
				_HandleUserCntUpdate(pPdu);
				break;
			default:
				log("wrong msg, cmd id=%d ", pPdu->GetCommandId());
				break;
		}

    }
}

/*
 * static method
 * add response pPdu to send list for another thread to send
 * if pPdu == NULL, it means you want to close connection with conn_uuid
 * e.g. parse packet failed
 */
void CProxyConn::AddResponsePdu(uint32_t conn_uuid, CImPdu* pPdu)
{
	ResponsePdu_t* pResp = new ResponsePdu_t;
	pResp->conn_uuid = conn_uuid;
	pResp->pPdu = pPdu;

	s_list_lock.lock();
	s_response_pdu_list.push_back(pResp);
	s_list_lock.unlock();
}

void CProxyConn::SendResponsePduList()
{
	s_list_lock.lock();
	while (!s_response_pdu_list.empty()) {
		ResponsePdu_t* pResp = s_response_pdu_list.front();
		s_response_pdu_list.pop_front();
		s_list_lock.unlock();

		CProxyConn* pConn = get_proxy_conn_by_uuid(pResp->conn_uuid);
		if (pConn) {
			if (pResp->pPdu) {
				pConn->SendPdu(pResp->pPdu);
			} else {
				log("close connection uuid=%d by parse pdu error\b", pResp->conn_uuid);
				//pConn->Close();
			}
		}

		if (pResp->pPdu)
			delete pResp->pPdu;
		delete pResp;

		s_list_lock.lock();
	}

	s_list_lock.unlock();
}
