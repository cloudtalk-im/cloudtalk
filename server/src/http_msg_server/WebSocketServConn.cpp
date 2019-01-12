/*
 * WEBSOCKETSERVCONN.cpp
 *
 */
#include "EncDec.h"
#include "WebSocketServConn.h"
#include "DBServConn.h"
#include "HttpConn.h"
#include "HttpPdu.h"
#include "IM.Server.pb.h"
#include "IM.Other.pb.h"
#include "ImPduBase.h"
#include "IM.Buddy.pb.h"
#include "IM.Group.pb.h"
#include "IM.Message.pb.h"
#include "IM.SwitchService.pb.h"
#include "IM.Login.pb.h"
#include "public_define.h"
#include "AttachData.h"
#include "ImUser.h"
#include "security.h"
using namespace IM::BaseDefine;

static ConnMap_t g_websocket_conn_map;
extern CAes *pAes;
CWebSocketServConn::CWebSocketServConn()
{
	m_bOpen = false;
	m_serv_idx = 0;
	m_bMaster = false;
    m_user_id = 0;
    m_bKickOff = false;
    m_last_seq_no = 0;
    m_msg_cnt_per_sec = 0;
    m_online_status = IM::BaseDefine::USER_STATUS_OFFLINE;
}

CWebSocketServConn::~CWebSocketServConn()
{

}

ConnMap_t get_websocket_serv_conn()
{
    return g_websocket_conn_map;
}

void websocket_serv_timer_callback(void* callback_data, uint8_t msg, uint32_t handle, void* pParam)
{
		uint64_t cur_time = get_tick_count();
		for (ConnMap_t::iterator it = g_websocket_conn_map.begin(); it != g_websocket_conn_map.end(); ) {
			ConnMap_t::iterator it_old = it;
			it++;

			CWebSocketServConn* pConn = (CWebSocketServConn*)it_old->second;
			pConn->OnTimer(cur_time);
		}
}

void init_websocket_timer_callback()
{
  netlib_register_timer(websocket_serv_timer_callback, NULL, 1000);
}

void CWebSocketServConn::OnConnect(net_handle_t handle)
{
		m_handle = handle;
		g_websocket_conn_map.insert(make_pair(handle, this));

		netlib_option(handle, NETLIB_OPT_SET_CALLBACK, (void*)imconn_callback);
		netlib_option(handle, NETLIB_OPT_SET_CALLBACK_DATA, (void*)&g_websocket_conn_map);
}

void CWebSocketServConn::Close()
{
		if (m_handle != NETLIB_INVALID_HANDLE) {
			netlib_close(m_handle);
			g_websocket_conn_map.erase(m_handle);
		}

		ReleaseRef();
}

void CWebSocketServConn::OnClose()
{
	Close();
}

void CWebSocketServConn::OnTimer(uint64_t curr_tick)
{
	if (curr_tick > m_last_send_tick + SERVER_HEARTBEAT_INTERVAL)
	{
		IM::Other::IMHeartBeat msg;
		CImPdu pdu;
		pdu.SetPBMsg(&msg);
		pdu.SetServiceId(SID_OTHER);
		pdu.SetCommandId(CID_OTHER_HEARTBEAT);
		SendPdu(&pdu);
	}

	if (curr_tick > m_last_recv_tick + SERVER_TIMEOUT) {
		log("message server timeout ");
		//Close();
	}
}

void CWebSocketServConn::HandlePdu(CImPdu* pPdu)
{
    try {
        if (pPdu->GetCommandId() != CID_LOGIN_REQ_USERLOGIN && !IsOpen() && IsKickOff()) {
            log("HandlePdu, wrong msg. ");
            throw CPduException(pPdu->GetServiceId(), pPdu->GetCommandId(), ERROR_CODE_WRONG_SERVICE_ID,
                                "HandlePdu error, user not login. ");
            return;
        }
        switch (pPdu->GetCommandId()) {
            case CID_OTHER_HEARTBEAT:
                break;
            case CID_LOGIN_REQ_USERLOGIN:
                _HandleLoginRequest(pPdu);
                break;
            case CID_MSG_DATA:
                _HandleClientMsgData(pPdu);
                break;
            default:
                log("wrong msg, cmd id=%d ", pPdu->GetCommandId());
                break;
        }
    }
    catch (std::exception& e){}
}

void CWebSocketServConn::_HandleClientMsgData(CImPdu* pPdu)
{
    IM::Message::IMMsgData msg;
    CHECK_PB_PARSE_MSG(msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength()));
    if (msg.msg_data().length() == 0) {
        return;
    }

    if (msg.from_user_id() == msg.to_session_id() && CHECK_MSG_TYPE_SINGLE(msg.msg_type()))
    {
        log("!!!from_user_id == to_user_id. ");
        return;
    }

    uint32_t to_session_id = msg.to_session_id();
    uint32_t msg_id = msg.msg_id();
    uint8_t msg_type = msg.msg_type();
    string msg_data = msg.msg_data();

    uint32_t cur_time = time(NULL);
    CDbAttachData attach_data(ATTACH_TYPE_HANDLE, m_handle, 0);
    msg.set_from_user_id(msg.from_user_id());
    msg.set_create_time(cur_time);
    msg.set_to_session_id(to_session_id);
    msg.set_attach_data(attach_data.GetBuffer(), attach_data.GetLength());


    //对内容进行加密操作.因为web无法进行加密。所以对期进行加密处理
    std::string pOutData="";
    char* pOutDataTemp = 0;
    uint32_t nOutLen = 0;
    int retCode =  pAes->Encrypt(msg_data.c_str(), msg_data.length(), &pOutDataTemp, nOutLen);
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


    pPdu->SetPBMsg(&msg);
    // send to DB storage server
    CDBServConn* pDbConn = get_db_serv_conn();
    if (pDbConn) {
        pDbConn->SendPdu(pPdu);
    }
}

void CWebSocketServConn::_HandleLoginRequest(CImPdu* pPdu)
{

// refuse second validate request
//    if (m_login_name.length() != 0) {
//        log("duplicate LoginRequest in the same conn ");
//        return;
//    }

    // check if all server connection are OK
    uint32_t result = 0;
    string result_string = "";

    CDBServConn *pConn = get_db_serv_conn();

    if (!pConn) {
        result = IM::BaseDefine::REFUSE_REASON_NO_DB_SERVER;
        result_string = "服务端异常";
    }
    if (result) {
        IM::Login::IMLoginRes msg;
        msg.set_server_time(time(NULL));
        msg.set_result_code((IM::BaseDefine::ResultType)result);
        msg.set_result_string(result_string);
        CImPdu pdu;
        pdu.SetPBMsg(&msg);
        pdu.SetServiceId(SID_LOGIN);
        pdu.SetCommandId(CID_LOGIN_RES_USERLOGIN);
        pdu.SetSeqNum(pPdu->GetSeqNum());
        SendPdu(&pdu);
        Close();
        return;
    }

    IM::Login::IMLoginReq msg;
    CHECK_PB_PARSE_MSG(msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength()));
    //假如是汉字，则转成拼音

    string loginname=msg.user_id();
    string password = msg.token();
    uint32_t online_status = msg.online_status();
    if (online_status < IM::BaseDefine::USER_STATUS_ONLINE || online_status > IM::BaseDefine::USER_STATUS_LEAVE) {
        log("HandleLoginReq, online status wrong: %u ", online_status);
        online_status = IM::BaseDefine::USER_STATUS_ONLINE;
    }

    CImUser* pImUser = CImUserManager::GetInstance()->GetImUserByLoginName(loginname);
    if (!pImUser) {
        pImUser = new CImUser(loginname);
        CImUserManager::GetInstance()->AddImUserByLoginName(loginname, pImUser);
    }

    CDbAttachData attach_data(ATTACH_TYPE_HANDLE, m_handle, 0);
    // continue to validate if the user is OK

    IM::Server::IMValidateReq msg2;
    msg2.set_user_name(msg.user_id());
    msg2.set_password(password);
    msg2.set_attach_data(attach_data.GetBuffer(), attach_data.GetLength());
    CImPdu pdu;
    pdu.SetPBMsg(&msg2);
    pdu.SetServiceId(SID_OTHER);
    pdu.SetCommandId(CID_OTHER_VALIDATE_REQ);
    pdu.SetSeqNum(pPdu->GetSeqNum());
    pConn->SendPdu(&pdu);
}

void WebSocketBroadcastMsg(CImPdu* pPdu)
{
		ConnMap_t::iterator it;
		for (it = g_websocket_conn_map.begin(); it != g_websocket_conn_map.end(); it++) {
            try {
                CWebSocketServConn *pRouteConn = (CWebSocketServConn *) it->second;
                pRouteConn->SendPdu(pPdu);
            }catch (std::exception& e){}
		}
}
