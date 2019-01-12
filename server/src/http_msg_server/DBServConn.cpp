/*
 * DBServConn.cpp
 *
 *  Created on: 2013-7-8
 *      Author: ziteng@mogujie.com
 */

#include "DBServConn.h"
#include "RouteServConn.h"
#include "public_define.h"
#include "AttachData.h"
#include "HttpConn.h"
#include "HttpPdu.h"
#include "HttpQuery.h"
#include "IM.Other.pb.h"
#include "IM.Message.pb.h"
#include "IM.Buddy.pb.h"
#include "IM.Server.pb.h"
#include "IM.SwitchService.pb.h"
#include "IM.Group.pb.h"
#include <list>
#include "EncDec.h"
#include "RouteServConn.h"
#include "security.h"
#include "AttachData.h"
#include "IM.Login.pb.h"
#include "IM.Group.pb.h"
#include "ImPduBase.h"
#include "public_define.h"
#include "WebSocketServConn.h"
#include "ImUser.h"
using namespace std;

using namespace IM::BaseDefine;

extern hash_map<string, auth_struct*> g_hm_http_auth;
bool g_bOnSync = false;

uint64_t    g_last_recv_auth = 0;
uint32_t    g_latest_auth = 0;

    
#define SERVER_TIMEOUT				30000
    
static ConnMap_t g_db_server_conn_map;

static serv_info_t* g_db_server_list = NULL;
static uint32_t		g_db_server_count = 0;
static uint32_t		g_db_server_login_count = 0;	// 到进行登录处理的DBServer的总连接数


extern CAes *pAes;
static void db_server_conn_timer_callback(void* callback_data, uint8_t msg, uint32_t handle, void* pParam)
{
	ConnMap_t::iterator it_old;
	CDBServConn* pConn = NULL;
	uint64_t cur_time = get_tick_count();

	for (ConnMap_t::iterator it = g_db_server_conn_map.begin(); it != g_db_server_conn_map.end(); ) {
		it_old = it;
		it++;

		pConn = (CDBServConn*)it_old->second;
		if (pConn->IsOpen()) {
			pConn->OnTimer(cur_time);
		}
	}

	// reconnect DB Storage Server
	// will reconnect in 4s, 8s, 16s, 32s, 64s, 4s 8s ...
	serv_check_reconnect<CDBServConn>(g_db_server_list, g_db_server_count);
}

void init_db_serv_conn(serv_info_t* server_list, uint32_t server_count, uint32_t concur_conn_cnt)
{
	g_db_server_list = server_list;
	g_db_server_count = server_count;

	uint32_t total_db_instance = server_count / concur_conn_cnt;

	// 必须至少配置2个BusinessServer实例, 一个用于用户登录业务，一个用于其他业务
	// 这样当其他业务量非常繁忙时，也不会影响客服端的登录验证
	// 建议配置4个实例，这样更新BusinessServer时，不会影响业务
	if (total_db_instance < 2) {
		log("DBServerIP need 2 instance at lest");
		exit(1);
	}

	g_db_server_login_count = (total_db_instance / 2) * concur_conn_cnt;
	log("DB server connection index for login business: [0, %u), for other business: [%u, %u)",
			g_db_server_login_count, g_db_server_login_count, g_db_server_count);

	serv_init<CDBServConn>(g_db_server_list, g_db_server_count);

	netlib_register_timer(db_server_conn_timer_callback, NULL, 1000);
}

// get a random db server connection in the range [start_pos, stop_pos)
static CDBServConn* get_db_server_conn_in_range(uint32_t start_pos, uint32_t stop_pos)
{
	uint32_t i = 0;
	CDBServConn* pDbConn = NULL;

	// determine if there is a valid DB server connection
	for (i = start_pos; i < stop_pos; i++) {
		pDbConn = (CDBServConn*)g_db_server_list[i].serv_conn;
		if (pDbConn && pDbConn->IsOpen()) {
			break;
		}
	}

	// no valid DB server connection
	if (i == stop_pos) {
		return NULL;
	}

	// return a random valid DB server connection
	while (true) {
		int i = rand() % (stop_pos - start_pos) + start_pos;
		pDbConn = (CDBServConn*)g_db_server_list[i].serv_conn;
		if (pDbConn && pDbConn->IsOpen()) {
			break;
		}
	}

	return pDbConn;
}

CDBServConn* get_db_serv_conn_for_login()
{
	// 先获取login业务的实例，没有就去获取其他业务流程的实例
	CDBServConn* pDBConn = get_db_server_conn_in_range(0, g_db_server_login_count);
	if (!pDBConn) {
		pDBConn = get_db_server_conn_in_range(g_db_server_login_count, g_db_server_count);
	}

	return pDBConn;
}

CDBServConn* get_db_serv_conn()
{
	// 先获取其他业务流程的实例，没有就去获取login业务的实例
	CDBServConn* pDBConn = get_db_server_conn_in_range(g_db_server_login_count, g_db_server_count);
	if (!pDBConn) {
		pDBConn = get_db_server_conn_in_range(0, g_db_server_login_count);
	}

	return pDBConn;
}


CDBServConn::CDBServConn()
{
	m_bOpen = false;
}

CDBServConn::~CDBServConn()
{

}

void CDBServConn::Connect(const char* server_ip, uint16_t server_port, uint32_t serv_idx)
{
	log("Connecting to DB Storage Server %s:%d", server_ip, server_port);

	m_serv_idx = serv_idx;
	m_handle = netlib_connect(server_ip, server_port, imconn_callback, (void*)&g_db_server_conn_map);

	if (m_handle != NETLIB_INVALID_HANDLE) {
		g_db_server_conn_map.insert(make_pair(m_handle, this));
	}
}

void CDBServConn::Close()
{
	// reset server information for the next connect
	serv_reset<CDBServConn>(g_db_server_list, g_db_server_count, m_serv_idx);

	if (m_handle != NETLIB_INVALID_HANDLE) {
		netlib_close(m_handle);
		g_db_server_conn_map.erase(m_handle);
	}

	ReleaseRef();
}

void CDBServConn::OnConfirm()
{
	log("connect to db server success");
	m_bOpen = true;
	g_db_server_list[m_serv_idx].reconnect_cnt = MIN_RECONNECT_CNT / 2;
}

void CDBServConn::OnClose()
{
	log("onclose from db server handle=%d", m_handle);
	Close();
}

void CDBServConn::OnTimer(uint64_t curr_tick)
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
		log("conn to db server timeout");
		Close();
	}
}

void CDBServConn::HandlePdu(CImPdu* pPdu)
{
    try {
        switch (pPdu->GetCommandId()) {
            case CID_OTHER_HEARTBEAT:
                break;
            case CID_OTHER_VALIDATE_RSP:
                _HandleValidateResponse(pPdu);
                break;
            case CID_OTHER_STOP_RECV_PACKET:
                _HandleStopReceivePacket(pPdu);
                break;
            case CID_GROUP_CREATE_RESPONSE:
                _HandleCreateGroupRsp(pPdu);
                break;
            case CID_GROUP_CHANGE_MEMBER_RESPONSE:
                _HandleChangeMemberRsp(pPdu);
                break;
            case CID_MSG_DATA:
                _HandleMsgData(pPdu);
                break;
            case CID_GROUP_INFO_RESPONSE:
                HandleGroupInfoResponse(pPdu);
                break;
            default:
                log("db server, wrong cmd id=%d", pPdu->GetCommandId());
        }
    }
    catch (std::exception& e){}
}

void CDBServConn::HandleGroupInfoResponse(CImPdu* pPdu)
{
        IM::Group::IMGroupInfoListRsp msg;
        CHECK_PB_PARSE_MSG(msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength()));

        uint32_t user_id = msg.user_id();
        uint32_t group_cnt = msg.group_info_list_size();
        CPduAttachData pduAttachData((uchar_t*)msg.attach_data().c_str(), msg.attach_data().length());

        log("HandleGroupInfoResponse, user_id=%u, group_cnt=%u. ", user_id, group_cnt);

        //此处是查询成员时使用，主要用于群消息从数据库获得msg_id后进行发送,一般此时group_cnt = 1
        if (pduAttachData.GetPduLength() > 0 && group_cnt > 0)
        {
            IM::BaseDefine::GroupInfo group_info = msg.group_info_list(0);
            uint32_t group_id = group_info.group_id();
            log("GroupInfoRequest is send by server, group_id=%u ", group_id);

            std::set<uint32_t> group_member_set;
            for (uint32_t i = 0; i < group_info.group_member_list_size(); i++)
            {
                uint32_t member_user_id = group_info.group_member_list(i);
                group_member_set.insert(member_user_id);
            }
            //发送者不是群内成员的情况
            if (group_member_set.find(user_id) == group_member_set.end())
            {
                log("user_id=%u is not in group, group_id=%u. ", user_id, group_id);
                return;
            }

            IM::Message::IMMsgData msg2;
            CHECK_PB_PARSE_MSG(msg2.ParseFromArray(pduAttachData.GetPdu(), pduAttachData.GetPduLength()));
            CImPdu pdu;
            std::string msg_data=msg2.msg_data();
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
            msg2.set_msg_data(pOutData);
            pdu.SetPBMsg(&msg2);
            pdu.SetServiceId(SID_MSG);
            pdu.SetCommandId(CID_MSG_DATA);

            //Push相关
            IM::Server::IMGroupGetShieldReq msg3;
            msg3.set_group_id(group_id);
            msg3.set_attach_data(pdu.GetBodyData(), pdu.GetBodyLength());

            for (uint32_t i = 0; i < group_info.group_member_list_size(); i++)
            {
                uint32_t member_user_id = group_info.group_member_list(i);
                msg3.add_user_id(member_user_id);
                CImUser* pToImUser = CImUserManager::GetInstance()->GetImUserById(member_user_id);
                if (pToImUser)
                {
                   // msg2.set_to_session_id(member_user_id);
                    msg2.set_to_user_id(member_user_id);
                    pdu.SetPBMsg(&msg2);
                    pdu.SetServiceId(SID_MSG);
                    pdu.SetCommandId(CID_MSG_DATA);
                    WebSocketBroadcastMsg(&pdu);
                }
            }

            CImPdu pdu2;

//            pdu2.SetPBMsg(&msg3);
//            pdu2.SetServiceId(SID_OTHER);
//            pdu2.SetCommandId(CID_OTHER_GET_SHIELD_REQ);
//            CDBServConn* pDbConn = get_db_serv_conn();
//            if (pDbConn)
//            {
//                pDbConn->SendPdu(&pdu2);
//            }
        }
        else if (pduAttachData.GetPduLength() == 0)
        {
        }
}
void CDBServConn::_HandleValidateResponse(CImPdu* pPdu)
{
        IM::Server::IMValidateRsp msg;
        CHECK_PB_PARSE_MSG(msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength()));
        string login_name = msg.user_name();
        uint32_t result = msg.result_code();
        string result_string = msg.result_string();
        CDbAttachData attach_data((uchar_t*)msg.attach_data().c_str(), msg.attach_data().length());
        log("HandleValidateResp, user_name=%s, result=%d", login_name.c_str(), result);
        CImUser* pImUser = CImUserManager::GetInstance()->GetImUserByLoginName(login_name);

        if (result != 0) {
            result = IM::BaseDefine::REFUSE_REASON_DB_VALIDATE_FAILED;
        }

        if (result == 0)
        {
            IM::BaseDefine::UserInfo user_info = msg.user_info();
            uint32_t user_id = user_info.user_id();

            CImUser* pUser = CImUserManager::GetInstance()->GetImUserById(user_id);
            if (!pUser)
            {
                pUser = pImUser;
            }

            pUser->SetUserId(user_id);
            pUser->SetNickName(user_info.user_nick_name());
            pUser->SetValidated();
            CImUserManager::GetInstance()->AddImUserById(user_id, pUser);


            //通知路由器。T掉其它server相同登录的用户 暂时不作处理
//            CRouteServConn* pRouteConn = get_route_serv_conn();
//            if (pRouteConn) {
//                IM::Server::IMServerKickUser msg2;
//                msg2.set_user_id(user_id);
//                msg2.set_client_type(CLIENT_TYPE_WINDOWS);
//                msg2.set_reason(1);
//                CImPdu pdu;
//                pdu.SetPBMsg(&msg2);
//                pdu.SetServiceId(SID_OTHER);
//                pdu.SetCommandId(CID_OTHER_SERVER_KICK_USER);
//                pRouteConn->SendPdu(&pdu);
//            }

            log("user_name: %s, uid: %d", login_name.c_str(), user_id);

            IM::Login::IMLoginRes msg3;
            msg3.set_server_time(time(NULL));
            msg3.set_result_code(IM::BaseDefine::REFUSE_REASON_NONE);
            msg3.set_result_string(result_string);
            msg3.set_online_status(USER_STATUS_ONLINE);

            IM::BaseDefine::UserInfo* user_info_tmp = msg3.mutable_user_info();
            user_info_tmp->set_user_id(user_info.user_id());
            user_info_tmp->set_user_gender(user_info.user_gender());
            user_info_tmp->set_user_nick_name(user_info.user_nick_name());
            user_info_tmp->set_avatar_url(user_info.avatar_url());
            user_info_tmp->set_sign_info(user_info.sign_info());
            user_info_tmp->set_department_id(user_info.department_id());
            user_info_tmp->set_email(user_info.email());
            user_info_tmp->set_user_real_name(user_info.user_real_name());
            user_info_tmp->set_user_tel(user_info.user_tel());
            user_info_tmp->set_user_domain(user_info.user_domain());
            user_info_tmp->set_status(user_info.status());
            CImPdu pdu2;
            pdu2.SetPBMsg(&msg3);
            pdu2.SetServiceId(SID_LOGIN);
            pdu2.SetCommandId(CID_LOGIN_RES_USERLOGIN);
            pdu2.SetSeqNum(pPdu->GetSeqNum());


            WebSocketBroadcastMsg(&pdu2);

        }
        else
        {

            IM::Login::IMLoginRes msg4;
            msg4.set_server_time(time(NULL));
            msg4.set_result_code((IM::BaseDefine::ResultType)result);
            msg4.set_result_string(result_string);
            CImPdu pdu3;
            pdu3.SetPBMsg(&msg4);
            pdu3.SetServiceId(SID_LOGIN);
            pdu3.SetCommandId(CID_LOGIN_RES_USERLOGIN);
            pdu3.SetSeqNum(pPdu->GetSeqNum());
            WebSocketBroadcastMsg(&pdu3);

        }
 }

void CDBServConn::_HandleStopReceivePacket(CImPdu* pPdu)
{
	log("HandleStopReceivePacket, from %s:%d",
			g_db_server_list[m_serv_idx].server_ip.c_str(), g_db_server_list[m_serv_idx].server_port);

	m_bOpen = false;
}
    
void CDBServConn::_HandleCreateGroupRsp(CImPdu *pPdu)
{
    IM::Group::IMGroupCreateRsp msg;
    CHECK_PB_PARSE_MSG(msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength()));
    uint32_t user_id = msg.user_id();
    string group_name = msg.group_name();
    uint32_t result_code = msg.result_code();
    uint32_t group_id = 0;
    if (msg.has_group_id()) {
        group_id = msg.group_id();
    }
    CDbAttachData attach_data((uchar_t*)msg.attach_data().c_str(), msg.attach_data().length());
    uint32_t http_handle = attach_data.GetHandle();
    CHttpConn* pHttpConn = FindHttpConnByHandle(http_handle);
    if(!pHttpConn)
    {
        log("no http connection");
        return;
    }
    log("HandleCreateGroupRsp, req_id=%u, group_name=%s, result=%u", user_id, group_name.c_str(),result_code);
    
    char* response_buf = NULL;
    if (result_code != 0)
    {
        response_buf = PackSendCreateGroupResult(HTTP_ERROR_CREATE_GROUP, HTTP_ERROR_MSG[10].c_str(), group_id);
    }
    else
    {
        response_buf = PackSendCreateGroupResult(HTTP_ERROR_SUCCESS, HTTP_ERROR_MSG[0].c_str(), group_id);
    }
    pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
    pHttpConn->Close();
    
}

//http发送消息 从数据库回调方法
void CDBServConn::_HandleMsgData(CImPdu *pPdu)
{
    IM::Message::IMMsgData msg;
    CHECK_PB_PARSE_MSG(msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength()));
    if (CHECK_MSG_TYPE_GROUP(msg.msg_type())) {
        HandleGroupMessage(pPdu); //群信息的发送处理
        return;
    }

    uint32_t from_user_id = msg.from_user_id();
    uint32_t to_user_id = msg.to_session_id();
    uint32_t msg_id = msg.msg_id();


    /*
     * 对同在此HttpMsgServer的用户，进行广播
     */
    CImUser* pFromImUser =  CImUserManager::GetInstance()->GetImUserById(from_user_id);
    CImUser* pToImUser =  CImUserManager::GetInstance()->GetImUserById(to_user_id);
    pPdu->SetSeqNum(0);
    if (pFromImUser || pToImUser) {
        WebSocketBroadcastMsg(pPdu);
    }

    //发送ack回调
    if(pFromImUser)
    {
        IM::Message::IMMsgDataAck msg2;
        msg2.set_user_id(from_user_id);
        msg2.set_msg_id(msg_id);
        msg2.set_session_id(to_user_id);
        msg2.set_session_type(::IM::BaseDefine::SESSION_TYPE_SINGLE);
        msg2.set_to_user_id(to_user_id);
        CImPdu pdu;
        pdu.SetPBMsg(&msg2);
        pdu.SetServiceId(SID_MSG);
        pdu.SetCommandId(CID_MSG_DATA_ACK);
        pdu.SetSeqNum(pPdu->GetSeqNum());
        WebSocketBroadcastMsg(&pdu);
    }

    CRouteServConn* pRouteConn = get_route_serv_conn();
    if (pRouteConn) {
        pRouteConn->SendPdu(pPdu);
    }

    //请求push token
    IM::Server::IMGetDeviceTokenReq msg3;
    msg3.add_user_id(to_user_id);
    msg3.set_attach_data(pPdu->GetBodyData(), pPdu->GetBodyLength());
    CImPdu pdu2;
    pdu2.SetPBMsg(&msg3);
    pdu2.SetServiceId(SID_OTHER);
    pdu2.SetCommandId(CID_OTHER_GET_DEVICE_TOKEN_REQ);
    SendPdu(&pdu2);
}

//专门处理群组的类
//流程-1。发db查询群成员列表。 2.查询回调方法里面向每个成员发送消息。
void CDBServConn::HandleGroupMessage(CImPdu* pPdu)
{
        IM::Message::IMMsgData msg;
        CHECK_PB_PARSE_MSG(msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength()));
        uint32_t from_user_id = msg.from_user_id();
        uint32_t to_group_id = msg.to_session_id();
        string msg_data = msg.msg_data();
        uint32_t msg_id = msg.msg_id();
        if (msg_id == 0) {
            log("HandleGroupMsg, write db failed, %u->%u. ", from_user_id, to_group_id);
            return;
        }
        uint8_t msg_type = msg.msg_type();
        CDbAttachData attach_data((uchar_t*)msg.attach_data().c_str(), msg.attach_data().length());

        log("HandleGroupMsg, %u->%u, msg id=%u. ", from_user_id, to_group_id, msg_id);

    CImUser* pFromImUser =  CImUserManager::GetInstance()->GetImUserById(from_user_id);
    if (pFromImUser)
    {
        IM::Message::IMMsgDataAck msg2;
        msg2.set_user_id(from_user_id);
        msg2.set_msg_id(msg_id);
        msg2.set_session_id(to_group_id);
        msg2.set_to_user_id(from_user_id);
        msg2.set_session_type(::IM::BaseDefine::SESSION_TYPE_SINGLE);
        CImPdu pdu;
        pdu.SetPBMsg(&msg2);
        pdu.SetServiceId(SID_MSG);
        pdu.SetCommandId(CID_MSG_DATA_ACK);
        pdu.SetSeqNum(pPdu->GetSeqNum());
        WebSocketBroadcastMsg(&pdu);
    }



        CRouteServConn* pRouteConn = get_route_serv_conn();
        if (pRouteConn)
        {
            pRouteConn->SendPdu(pPdu);
        }

        // 服务器没有群的信息，向DB服务器请求群信息，并带上消息作为附件，返回时在发送该消息给其他群成员
        //IM::BaseDefine::GroupVersionInfo group_version_info;
        CPduAttachData pduAttachData(3, attach_data.GetHandle(), pPdu->GetBodyLength(), pPdu->GetBodyData());

        IM::Group::IMGroupInfoListReq msg3;
        msg3.set_user_id(from_user_id);
        IM::BaseDefine::GroupVersionInfo* group_version_info = msg3.add_group_version_list();
        group_version_info->set_group_id(to_group_id);
        group_version_info->set_version(0);
        msg3.set_attach_data(pduAttachData.GetBuffer(), pduAttachData.GetLength());
        CImPdu pdu;
        pdu.SetPBMsg(&msg3);
        pdu.SetServiceId(SID_GROUP);
        pdu.SetCommandId(CID_GROUP_INFO_REQUEST);
        CDBServConn* pDbConn = get_db_serv_conn();
        if(pDbConn)
        {
            pDbConn->SendPdu(&pdu);
        }
}

void CDBServConn::_HandleChangeMemberRsp(CImPdu *pPdu)
{
    IM::Group::IMGroupChangeMemberRsp msg;
    CHECK_PB_PARSE_MSG(msg.ParseFromArray(pPdu->GetBodyData(), pPdu->GetBodyLength()));
    
    uint32_t change_type = msg.change_type();
    uint32_t user_id = msg.user_id();
    uint32_t result = msg.result_code();
    uint32_t group_id = msg.group_id();
    uint32_t chg_user_cnt = msg.chg_user_id_list_size();
    uint32_t cur_user_cnt = msg.cur_user_id_list_size();
    log("HandleChangeMemberResp, change_type=%u, req_id=%u, group_id=%u, result=%u, chg_usr_cnt=%u, cur_user_cnt=%u.",
        change_type, user_id, group_id, result, chg_user_cnt, cur_user_cnt);
    CDbAttachData attach_data((uchar_t*)msg.attach_data().c_str(), msg.attach_data().length());
    uint32_t http_handle = attach_data.GetHandle();
    CHttpConn* pHttpConn = FindHttpConnByHandle(http_handle);
    if(!pHttpConn)
    {
        log("no http connection.");
        return;
    }
    char* response_buf = NULL;
    if (result != 0)
    {
        response_buf = PackSendResult(HTTP_ERROR_CHANGE_MEMBER, HTTP_ERROR_MSG[11].c_str());
    }
    else
    {
        response_buf = PackSendResult(HTTP_ERROR_SUCCESS, HTTP_ERROR_MSG[0].c_str());
    }
    pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
    pHttpConn->Close();
    
    if (!result) {
        IM::Group::IMGroupChangeMemberNotify msg2;
        msg2.set_user_id(user_id);
        msg2.set_change_type((::IM::BaseDefine::GroupModifyType)change_type);
        msg2.set_group_id(group_id);
        for (uint32_t i = 0; i < chg_user_cnt; i++) {
            msg2.add_chg_user_id_list(msg.chg_user_id_list(i));
        }
        for (uint32_t i = 0; i < cur_user_cnt; i++) {
            msg2.add_cur_user_id_list(msg.cur_user_id_list(i));
        }
        CImPdu pdu;
        pdu.SetPBMsg(&msg2);
        pdu.SetServiceId(SID_GROUP);
        pdu.SetCommandId(CID_GROUP_CHANGE_MEMBER_NOTIFY);
        CRouteServConn* pRouteConn = get_route_serv_conn();
        if (pRouteConn) {
            pRouteConn->SendPdu(&pdu);
        }
    }
}
