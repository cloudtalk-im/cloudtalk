/*
 * RouteServConn.cpp
 *
 *  Created on: 2013-7-8
 *      Author: ziteng@mogujie.com
 */
#include "EncDec.h"
#include "RouteServConn.h"
#include "DBServConn.h"
#include "HttpConn.h"
#include "HttpPdu.h"
#include "IM.Server.pb.h"
#include "IM.Other.pb.h"
#include "ImPduBase.h"
#include "AttachData.h"
#include "AttachData.h"
#include "IM.Buddy.pb.h"
#include "IM.Message.pb.h"
#include "IM.Group.pb.h"
#include "IM.SwitchService.pb.h"
#include "IM.File.pb.h"
#include "WebSocketServConn.h"
#include "ImUser.h"
#include "security.h"


using namespace IM::BaseDefine;
static ConnMap_t g_route_server_conn_map;

static serv_info_t* g_route_server_list;
static uint32_t g_route_server_count;
static CRouteServConn* g_master_rs_conn = NULL;

extern CAes *pAes;
void route_server_conn_timer_callback(void* callback_data, uint8_t msg, uint32_t handle, void* pParam)
{
	ConnMap_t::iterator it_old;
	CRouteServConn* pConn = NULL;
	uint64_t cur_time = get_tick_count();

	for (ConnMap_t::iterator it = g_route_server_conn_map.begin(); it != g_route_server_conn_map.end(); ) {
		it_old = it;
		it++;

		pConn = (CRouteServConn*)it_old->second;
		pConn->OnTimer(cur_time);
	}

	// reconnect RouteServer
	serv_check_reconnect<CRouteServConn>(g_route_server_list, g_route_server_count);
}

void init_route_serv_conn(serv_info_t* server_list, uint32_t server_count)
{
	g_route_server_list = server_list;
	g_route_server_count = server_count;

	serv_init<CRouteServConn>(g_route_server_list, g_route_server_count);

	netlib_register_timer(route_server_conn_timer_callback, NULL, 1000);
}

bool is_route_server_available()
{
	CRouteServConn* pConn = NULL;

	for (uint32_t i = 0; i < g_route_server_count; i++) {
		pConn = (CRouteServConn*)g_route_server_list[i].serv_conn;
		if (pConn && pConn->IsOpen()) {
			return true;
		}
	}

	return false;
}

void send_to_all_route_server(CImPdu* pPdu)
{
	CRouteServConn* pConn = NULL;

	for (uint32_t i = 0; i < g_route_server_count; i++) {
		pConn = (CRouteServConn*)g_route_server_list[i].serv_conn;
		if (pConn && pConn->IsOpen()) {
			pConn->SendPdu(pPdu);
		}
	}
}

// get the oldest route server connection
CRouteServConn* get_route_serv_conn()
{
	return g_master_rs_conn;
}

void update_master_route_serv_conn()
{
	uint64_t oldest_connect_time = (uint64_t)-1;
	CRouteServConn* pOldestConn = NULL;

	CRouteServConn* pConn = NULL;

	for (uint32_t i = 0; i < g_route_server_count; i++) {
		pConn = (CRouteServConn*)g_route_server_list[i].serv_conn;
		if (pConn && pConn->IsOpen() && (pConn->GetConnectTime() < oldest_connect_time) ){
			pOldestConn = pConn;
			oldest_connect_time = pConn->GetConnectTime();
		}
	}

	g_master_rs_conn =  pOldestConn;

	if (g_master_rs_conn) {
        IM::Server::IMRoleSet msg;
        msg.set_master(1);
        CImPdu pdu;
        pdu.SetPBMsg(&msg);
        pdu.SetServiceId(IM::BaseDefine::SID_OTHER);
        pdu.SetCommandId(IM::BaseDefine::CID_OTHER_ROLE_SET);
		g_master_rs_conn->SendPdu(&pdu);
	}
}


CRouteServConn::CRouteServConn()
{
	m_bOpen = false;
	m_serv_idx = 0;
}

CRouteServConn::~CRouteServConn()
{

}

void CRouteServConn::Connect(const char* server_ip, uint16_t server_port, uint32_t idx)
{
	log("Connecting to RouteServer %s:%d ", server_ip, server_port);

	m_serv_idx = idx;
	m_handle = netlib_connect(server_ip, server_port, imconn_callback, (void*)&g_route_server_conn_map);

	if (m_handle != NETLIB_INVALID_HANDLE) {
		g_route_server_conn_map.insert(make_pair(m_handle, this));
	}
}

void CRouteServConn::Close()
{
	serv_reset<CRouteServConn>(g_route_server_list, g_route_server_count, m_serv_idx);

	m_bOpen = false;
	if (m_handle != NETLIB_INVALID_HANDLE) {
		netlib_close(m_handle);
		g_route_server_conn_map.erase(m_handle);
	}

	ReleaseRef();

	if (g_master_rs_conn == this) {
		update_master_route_serv_conn();
	}
}

void CRouteServConn::OnConfirm()
{
	log("connect to route server success ");
	m_bOpen = true;
	m_connect_time = get_tick_count();
	g_route_server_list[m_serv_idx].reconnect_cnt = MIN_RECONNECT_CNT / 2;

	if (g_master_rs_conn == NULL) {
		update_master_route_serv_conn();
	}

}

void CRouteServConn::OnClose()
{
	log("onclose from route server handle=%d ", m_handle);
	Close();
}

void CRouteServConn::OnTimer(uint64_t curr_tick)
{
	if (curr_tick > m_last_send_tick + SERVER_HEARTBEAT_INTERVAL) {
        IM::Other::IMHeartBeat msg;
        CImPdu pdu;
        pdu.SetPBMsg(&msg);
        pdu.SetServiceId(IM::BaseDefine::SID_OTHER);
        pdu.SetCommandId(IM::BaseDefine::CID_OTHER_HEARTBEAT);
		SendPdu(&pdu);
	}

	if (curr_tick > m_last_recv_tick + SERVER_TIMEOUT) {
		log("conn to route server timeout ");
		Close();
	}
}

void CRouteServConn::HandlePdu(CImPdu* pPdu)
{

	try {
		switch (pPdu->GetCommandId()) {
			case CID_OTHER_HEARTBEAT:
				break;
			case CID_OTHER_SERVER_KICK_USER:
				//_HandleKickUser( pPdu );
				break;
			case CID_BUDDY_LIST_STATUS_NOTIFY:
				//_HandleStatusNotify( pPdu );
				break;
			case CID_BUDDY_LIST_USERS_STATUS_RESPONSE:
				//_HandleUsersStatusResponse( pPdu );
				break;
			case CID_MSG_READ_NOTIFY:
				//_HandleMsgReadNotify(pPdu);
				break;
			case CID_MSG_DATA:
				_HandleMsgData(pPdu);
				break;
			default:
				log("unknown cmd id=%d ", pPdu->GetCommandId());
				break;
		}
	}
	catch (std::exception& e){}

}
void CRouteServConn::_HandleGroupMessageData(CImPdu* pPdu)
{
	IM::Message::IMMsgData msg;
	CHECK_PB_PARSE_MSG(msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength()));

	uint32_t from_user_id = msg.from_user_id();
	uint32_t to_group_id = msg.to_session_id();
	string msg_data = msg.msg_data();
	uint32_t msg_id = msg.msg_id();
	log("HandleGroupMessageBroadcast, %u->%u, msg id=%u. ", from_user_id, to_group_id, msg_id);

	// 服务器没有群的信息，向DB服务器请求群信息，并带上消息作为附件，返回时在发送该消息给其他群成员
	//IM::BaseDefine::GroupVersionInfo group_version_info;
	CPduAttachData pduAttachData(ATTACH_TYPE_HANDLE, 0, pPdu->GetBodyLength(), pPdu->GetBodyData());

	IM::Group::IMGroupInfoListReq msg2;
	msg2.set_user_id(from_user_id);
	IM::BaseDefine::GroupVersionInfo* group_version_info = msg2.add_group_version_list();
	group_version_info->set_group_id(to_group_id);
	group_version_info->set_version(0);
	msg2.set_attach_data(pduAttachData.GetBuffer(), pduAttachData.GetLength());
	CImPdu pdu;
	pdu.SetPBMsg(&msg2);
	pdu.SetServiceId(SID_GROUP);
	pdu.SetCommandId(CID_GROUP_INFO_REQUEST);
	CDBServConn* pDbConn = get_db_serv_conn();
	if(pDbConn)
	{
		pDbConn->SendPdu(&pdu);
	}
}

void CRouteServConn::_HandleMsgData(CImPdu* pPdu)
{
		IM::Message::IMMsgData msg;
		CHECK_PB_PARSE_MSG(msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength()));
		if (CHECK_MSG_TYPE_GROUP(msg.msg_type())) {
			_HandleGroupMessageData(pPdu);
			return;
		}
		uint32_t from_user_id = msg.from_user_id();
		uint32_t to_user_id = msg.to_session_id();
		uint32_t msg_id = msg.msg_id();
		log("HandleMsgData, %u->%u, msg_id=%u. ", from_user_id, to_user_id, msg_id);

        CImUser* pUser = CImUserManager::GetInstance()->GetImUserById(to_user_id);
        if (pUser)
        {
            std::string msg_data=msg.msg_data();
            std::string pOutData="";
            char* pOutDataTemp = 0;
            uint32_t nOutLen = 0;
            int retCode =  pAes->Decrypt(msg_data.c_str(), msg_data.length(), &pOutDataTemp, nOutLen);
            if (retCode == 0 && nOutLen > 0 && pOutDataTemp != 0)
            {
                pOutData = std::string(pOutDataTemp,nOutLen);
                Free(pOutDataTemp);
            }
            else
            {
                pOutData = msg_data;
            }

            msg.set_msg_data(pOutData);
            msg.set_to_user_id(to_user_id);
            pPdu->SetPBMsg(&msg);
            WebSocketBroadcastMsg(pPdu);
        }
}

