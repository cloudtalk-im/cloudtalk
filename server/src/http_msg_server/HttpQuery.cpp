/*
 * HttpQuery.cpp
 *
 *  Created on: 2013-10-22
 *      Author: ziteng@mogujie.com
 */
#include <sstream>
#include "EncDec.h"
#include "HttpQuery.h"
#include "RouteServConn.h"
#include "DBServConn.h"
#include "HttpPdu.h"
#include "public_define.h"
#include "AttachData.h"
#include "IM.Message.pb.h"
#include "IM.Buddy.pb.h"
#include "IM.SwitchService.pb.h"
#include "IM.Group.pb.h"
#include "security.h"
static uint32_t g_total_query = 0;
static uint32_t g_last_year = 0;
static uint32_t g_last_month = 0;
static uint32_t g_last_mday = 0;

CHttpQuery* CHttpQuery::m_query_instance = NULL;

hash_map<string, auth_struct*> g_hm_http_auth;
extern CAes *pAes;

void http_query_timer_callback(void* callback_data, uint8_t msg, uint32_t handle, void* pParam)
{
	struct tm* tm;
	time_t currTime;

	time(&currTime);
	tm = localtime(&currTime);

	uint32_t year = tm->tm_year + 1900;
	uint32_t mon = tm->tm_mon + 1;
	uint32_t mday = tm->tm_mday;
	if (year != g_last_year || mon != g_last_month || mday != g_last_mday) {
		// a new day begin, clear the count
		log("a new day begin, g_total_query=%u ", g_total_query);
		g_total_query = 0;
		g_last_year = year;
		g_last_month = mon;
		g_last_mday = mday;
	}
}

CHttpQuery* CHttpQuery::GetInstance()
{
	if (!m_query_instance) {
		m_query_instance = new CHttpQuery();
		netlib_register_timer(http_query_timer_callback, NULL, 1000);
	}
	return m_query_instance;
}

void CHttpQuery::DispatchQuery(std::string& url, std::string& post_data, CHttpConn* pHttpConn)
{
	++g_total_query;
    
    /* 在用
     /query/SendMessageByXiaoT
     /query/GroupP2PMessage
     
     */
	log("DispatchQuery, url=%s, content=%s ", url.c_str(), post_data.c_str());

    Json::Reader reader;
    Json::Value value;
    Json::Value root;

	if ( !reader.parse(post_data, value) ) {
		log("json parse failed, post_data=%s ", post_data.c_str());
		pHttpConn->Close();
		return;
	}
    
    string strErrorMsg;
    string strAppKey;
    HTTP_ERROR_CODE nRet = HTTP_ERROR_SUCCESS;
    try
    {
        string strInterface(url.c_str() + strlen("/query/"));
        strAppKey = value["app_key"].asString();
        string strIp = pHttpConn->GetPeerIP();
        uint32_t nUserId = value["req_user_id"].asUInt();
        nRet = _CheckAuth(strAppKey, nUserId, strInterface, strIp);
    }
    catch ( std::runtime_error msg)
    {
        nRet = HTTP_ERROR_INTERFACE;
    }
    
    if(HTTP_ERROR_SUCCESS != nRet)
    {
        if(nRet < HTTP_ERROR_MAX)
        {
            root["error_code"] = nRet;
            root["error_msg"] = HTTP_ERROR_MSG[nRet];
        }
        else
        {
            root["error_code"] = -1;
            root["error_msg"] = "未知错误";
        }
        string strResponse = root.toStyledString();
        pHttpConn->Send((void*)strResponse.c_str(), strResponse.length());
        return;
    }
    
    // process post request with post content
    if (strcmp(url.c_str(), "/query/CreateGroup") == 0)
    {
        _QueryCreateGroup(strAppKey, value, pHttpConn);
    }
    else if (strcmp(url.c_str(), "/query/ChangeMembers") == 0)
    {
        _QueryChangeMember(strAppKey, value, pHttpConn);
    }
    else if (strcmp(url.c_str(), "/api/sendmsg") == 0)
    {
        _ApiSendMsg(strAppKey, value, pHttpConn);
    }
    else {
        log("url not support ");
        pHttpConn->Close();
        return;
    }
}

void CHttpQuery::_ApiSendMsg(const string& strAppKey, Json::Value &post_json_obj, CHttpConn *pHttpConn) {
    CDBServConn *pConn =get_db_serv_conn();
    if (!pConn) {
        log("no connection to dbServConn ");
        char *response_buf = PackSendResult(HTTP_ERROR_SERVER_EXCEPTION, HTTP_ERROR_MSG[9].c_str());
        pHttpConn->Send(response_buf, (uint32_t) strlen(response_buf));
        pHttpConn->Close();
        return;
    }

    if (post_json_obj["to_session_id"].isNull() || post_json_obj["msg_type"].isNull() || post_json_obj["msg_data"].isNull() || post_json_obj["from_user_id"].isNull()) {
        log("send msg 参数不完整.");
        char *response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t) strlen(response_buf));
        pHttpConn->Close();
        return;
    }

    if(!IM::BaseDefine::MsgType_IsValid(post_json_obj["msg_type"].asInt()))
    {
        return;
    }
    IM::Message::IMMsgData msg;
    std::string msg_data = post_json_obj["msg_data"].asString();
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
    msg.set_msg_type((IM::BaseDefine::MsgType)post_json_obj["msg_type"].asInt());
    msg.set_msg_id(post_json_obj["msg_id"].asInt());
    msg.set_from_user_id(post_json_obj["from_user_id"].asInt());
    msg.set_to_session_id(post_json_obj["to_session_id"].asInt());

    uint32_t cur_time = time(NULL);
    msg.set_create_time(cur_time);

    CImPdu pdu;
    CDbAttachData attach_data(ATTACH_TYPE_HANDLE, pHttpConn->GetConnHandle());
    msg.set_attach_data(attach_data.GetBuffer(), attach_data.GetLength());
    pdu.SetPBMsg(&msg);
    pdu.SetServiceId(IM::BaseDefine::SID_MSG);
    pdu.SetCommandId(IM::BaseDefine::CID_MSG_DATA);
    // send to DB storage server

    if (pConn) {
        pConn->SendPdu(&pdu);
    }
    char* response_buf = NULL;
    response_buf = PackSendResult(HTTP_ERROR_SUCCESS, HTTP_ERROR_MSG[0].c_str());
    pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
    pHttpConn->Close();
}

string& CHttpQuery::doProcess1(const string& pData)
{
    std::string pOutData="";
    char* pOutDataTemp = 0;
    uint32_t nOutLen = 0;
    int retCode = EncryptMsg(pData.c_str(), pData.length(), &pOutDataTemp, nOutLen);
    if (retCode == 0 && nOutLen > 0 && pOutDataTemp != 0)
    {
        pOutData = std::string(pOutDataTemp,nOutLen);
        Free(pOutDataTemp);
    }
    else
    {
        pOutData = pData;
    }
    return pOutData;
}

void CHttpQuery::_QueryCreateGroup(const string& strAppKey, Json::Value &post_json_obj, CHttpConn *pHttpConn)
{
    CDBServConn *pConn = get_db_serv_conn();
    if (!pConn) {
        log("no connection to DBProxy ");
        char* response_buf = PackSendResult(HTTP_ERROR_SERVER_EXCEPTION , HTTP_ERROR_MSG[9].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
        return;
    }
    
    if ( post_json_obj["req_user_id"].isNull()) {
        log("no user id ");
        char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
        return;
    }
    
    if (post_json_obj["group_name"].isNull()) {
        log("no group name ");
        char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
        return;
    }
    
    if (post_json_obj["group_type"].isNull()) {
        log("no group type ");
        char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
        return;
    }
    
    if (post_json_obj["group_avatar"].isNull()) {
        log("no group avatar ");
        char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
        return;
    }
    
    if (post_json_obj["user_id_list"].isNull()) {
        log("no user list ");
        char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
        return;
    }
    
    try
    {
        uint32_t user_id = post_json_obj["req_user_id"].asUInt();
        string group_name = post_json_obj["group_name"].asString();
        uint32_t group_type = post_json_obj["group_type"].asUInt();
        string group_avatar = post_json_obj["group_avatar"].asString();
        uint32_t user_cnt = post_json_obj["user_id_list"].size();
        log("QueryCreateGroup, user_id: %u, group_name: %s, group_type: %u, user_cnt: %u. ",
            user_id, group_name.c_str(), group_type, user_cnt);
        if (!IM::BaseDefine::GroupType_IsValid(group_type)) {
            log("QueryCreateGroup, unvalid group_type");
            char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
            pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
            pHttpConn->Close();
            return;
        }
        
        CDbAttachData attach_data(ATTACH_TYPE_HANDLE, pHttpConn->GetConnHandle());
        IM::Group::IMGroupCreateReq msg;
        msg.set_user_id(0);
        msg.set_group_name(group_name);
        msg.set_group_avatar(group_avatar);
        msg.set_group_type((::IM::BaseDefine::GroupType)group_type);
        for (uint32_t i = 0; i < user_cnt; i++) {
            uint32_t member_id = post_json_obj["user_id_list"][i].asUInt();
            msg.add_member_id_list(member_id);
        }
        msg.set_attach_data(attach_data.GetBuffer(), attach_data.GetLength());
        
        CImPdu pdu;
        pdu.SetPBMsg(&msg);
        pdu.SetServiceId(IM::BaseDefine::SID_GROUP);
        pdu.SetCommandId(IM::BaseDefine::CID_GROUP_CREATE_REQUEST);
        pConn->SendPdu(&pdu);

    }
    catch (std::runtime_error msg)
    {
        log("parse json data failed.");
        char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
    }
}

void CHttpQuery::_QueryChangeMember(const string& strAppKey, Json::Value &post_json_obj, CHttpConn *pHttpConn)
{
    CDBServConn *pConn = get_db_serv_conn();
    if (!pConn) {
        log("no connection to RouteServConn ");
        char* response_buf = PackSendResult(HTTP_ERROR_SERVER_EXCEPTION, HTTP_ERROR_MSG[9].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
        return;
    }
    if ( post_json_obj["req_user_id"].isNull()) {
        log("no user id ");
        char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
        return;
    }
    
    if ( post_json_obj["group_id"].isNull() ) {
        log("no group id ");
        char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
        return;
    }
    
    if ( post_json_obj["modify_type"].isNull() ) {
        log("no modify_type ");
        char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
        return;
    }
    
    if (post_json_obj["user_id_list"].isNull()) {
        log("no user list ");
        char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
        return;
    }
    
    try
    {
        uint32_t user_id = post_json_obj["req_user_id"].asUInt();
        uint32_t group_id = post_json_obj["group_id"].asUInt();
        uint32_t modify_type = post_json_obj["modify_type"].asUInt();
        uint32_t user_cnt =  post_json_obj["user_id_list"].size();
        log("QueryChangeMember, user_id: %u, group_id: %u, modify type: %u, user_cnt: %u. ",
            user_id, group_id, modify_type, user_cnt);
        if (!IM::BaseDefine::GroupModifyType_IsValid(modify_type)) {
            log("QueryChangeMember, unvalid modify_type");
            char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
            pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
            pHttpConn->Close();
            return;
        }
        CDbAttachData attach_data(ATTACH_TYPE_HANDLE, pHttpConn->GetConnHandle());
        IM::Group::IMGroupChangeMemberReq msg;
        msg.set_user_id(0);
        msg.set_change_type((::IM::BaseDefine::GroupModifyType)modify_type);
        msg.set_group_id(group_id);
        for (uint32_t i = 0; i < user_cnt; i++) {
            uint32_t member_id = post_json_obj["user_id_list"][i].asUInt();
            msg.add_member_id_list(member_id);
        }
        msg.set_attach_data(attach_data.GetBuffer(), attach_data.GetLength());
        CImPdu pdu;
        pdu.SetPBMsg(&msg);
        pdu.SetServiceId(IM::BaseDefine::SID_GROUP);
        pdu.SetCommandId(IM::BaseDefine::CID_GROUP_CHANGE_MEMBER_REQUEST);
        pConn->SendPdu(&pdu);
    }
    catch (std::runtime_error msg)
    {
        log("parse json data failed.");
        char* response_buf = PackSendResult(HTTP_ERROR_PARMENT, HTTP_ERROR_MSG[1].c_str());
        pHttpConn->Send(response_buf, (uint32_t)strlen(response_buf));
        pHttpConn->Close();
    }
}

HTTP_ERROR_CODE CHttpQuery::_CheckAuth(const string& strAppKey, const uint32_t userId, const string& strInterface, const string& strIp)
{
    return HTTP_ERROR_SUCCESS;
}

HTTP_ERROR_CODE CHttpQuery::_CheckPermission(const string& strAppKey, uint8_t nType, const list<uint32_t>& lsToId, string strMsg)
{
    strMsg.clear();
    return HTTP_ERROR_SUCCESS;
}

