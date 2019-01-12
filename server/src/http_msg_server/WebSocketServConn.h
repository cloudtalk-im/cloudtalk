/*
 * RouteServConn.h
 *
 *  Created on: 2013-7-8
 *      Author: ziteng@mogujie.com
 */

#ifndef WEBSOCKETSERVCONN_H_
#define WEBSOCKETSERVCONN_H_

#include "imconn.h"
#include "ServInfo.h"

class CWebSocketServConn : public CImConn
{
public:
    CWebSocketServConn();
	virtual ~CWebSocketServConn();
    void SetOpen() { m_bOpen = true; }
    bool IsOpen() { return m_bOpen; }
    void SetKickOff() { m_bKickOff = true; }
    bool IsKickOff() { return m_bKickOff; }
    virtual void Close();
    virtual void OnConnect(net_handle_t handle);
    virtual void OnClose();
    virtual void OnTimer(uint64_t curr_tick);
    virtual void HandlePdu(CImPdu* pPdu);

private:
	bool 		m_bOpen;
	uint32_t	m_serv_idx;
	uint64_t	m_connect_time;

    void _HandleLoginRequest(CImPdu* pPdu);
	void _HandleClientMsgData(CImPdu* pPdu);

	bool			m_bMaster; string          m_login_name;        //登录名拼音
    uint32_t        m_user_id;
    bool            m_bKickOff;
    uint64_t		m_login_time;

    uint32_t		m_last_seq_no;

    uint16_t		m_pdu_version;

    string 			m_client_version;	// e.g MAC/2.2, or WIN/2.2

    uint32_t		m_msg_cnt_per_sec;

    uint32_t        m_client_type;        //客户端登录方式

    uint32_t        m_online_status;      //在线状态 1-online, 2-off-line, 3-leave
};

ConnMap_t get_websocket_serv_conn();
void websocket_serv_timer_callback(void* callback_data, uint8_t msg, uint32_t handle, void* pParam);
void init_websocket_timer_callback();
void WebSocketBroadcastMsg(CImPdu* pPdu);
#endif /* WEBSOCKETSERVCONN_H_ */
