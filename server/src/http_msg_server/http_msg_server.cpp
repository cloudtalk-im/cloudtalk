//
//  http_msg_server.cpp
//  http_msg_server
//
//

#include "WebSocketServConn.h"
#include "netlib.h"
#include "ConfigFileReader.h"
#include "RouteServConn.h"
#include "DBServConn.h"
#include "version.h"
#include "ServInfo.h"
#include "HttpConn.h"
#include "HttpQuery.h"
#include "util.h"
#include "security.h"
#include "EncDec.h"


#define DEFAULT_CONCURRENT_DB_CONN_CNT  2

CAes *pAes;
// for client connect in
void http_callback(void* callback_data, uint8_t msg, uint32_t handle, void* pParam)
{
	if (msg == NETLIB_MSG_CONNECT)
	{
		CHttpConn* pConn = new CHttpConn();
		pConn->OnConnect(handle);
	}
	else
	{
		log("!!!error msg: %d ", msg);
	}
}

void websocket_serv_callback(void* callback_data, uint8_t msg, uint32_t handle, void* pParam)
{
    if (msg == NETLIB_MSG_CONNECT)
    {
		CWebSocketServConn* pConn = new CWebSocketServConn();
        pConn->OnConnect(handle);
    }
    else
    {
        log("!!!websocket_serv_callback error msg: %d ", msg);
    }
}

int main(int argc, char* argv[])
{
	if ((argc == 2) && (strcmp(argv[1], "-v") == 0)) {
		printf("Server Version: HttpMsgServer/%s\n", VERSION);
		printf("Server Build: %s %s\n", __DATE__, __TIME__);
		return 0;
	}
    
	signal(SIGPIPE, SIG_IGN);
	srand(time(NULL));
    
	log("MsgServer max files can open: %d ", getdtablesize());
    
	CConfigFileReader config_file("httpmsgserver.conf");
    
	char* listen_ip = config_file.GetConfigName("ListenIP");
	char* str_listen_port = config_file.GetConfigName("ListenPort");

	//websocket服务监听
	char* websocket_listen_ip = config_file.GetConfigName("wsListenIP");
	char* str_listen_ws_port = config_file.GetConfigName("wsListenPort");
	uint16_t listen_ws_port = atoi(str_listen_ws_port);
    
	uint32_t db_server_count = 0;
	serv_info_t* db_server_list = read_server_config(&config_file, "DBServerIP", "DBServerPort", db_server_count);
    
	uint32_t route_server_count = 0;
	serv_info_t* route_server_list = read_server_config(&config_file, "RouteServerIP", "RouteServerPort", route_server_count);

    char* str_aes_key = config_file.GetConfigName("aesKey");

	// 到BusinessServer的开多个并发的连接
	uint32_t concurrent_db_conn_cnt = DEFAULT_CONCURRENT_DB_CONN_CNT;
	uint32_t db_server_count2 = db_server_count * DEFAULT_CONCURRENT_DB_CONN_CNT;
	char* concurrent_db_conn = config_file.GetConfigName("ConcurrentDBConnCnt");
	if (concurrent_db_conn) {
		concurrent_db_conn_cnt  = atoi(concurrent_db_conn);
		db_server_count2 = db_server_count * concurrent_db_conn_cnt;
	}

	serv_info_t* db_server_list2 = NULL;
	if (db_server_count2 > 0) {
		db_server_list2 = new serv_info_t [ db_server_count2];
		for (uint32_t i = 0; i < db_server_count2; i++) {
			db_server_list2[i].server_ip = db_server_list[i / concurrent_db_conn_cnt].server_ip.c_str();
			db_server_list2[i].server_port = db_server_list[i / concurrent_db_conn_cnt].server_port;
		}
	}

	if (!listen_ip || !str_listen_port) {
		log("config file miss, exit... ");
		return -1;
	}

    if (!str_aes_key || strlen(str_aes_key)!=32) {
        log("aes key is invalied");
        return -1;
    }
    pAes = new CAes(str_aes_key);

	uint16_t listen_port = atoi(str_listen_port);
    
	int ret = netlib_init();
    
	if (ret == NETLIB_ERROR)
		return ret;
    
	CStrExplode listen_ip_list(listen_ip, ';');
	for (uint32_t i = 0; i < listen_ip_list.GetItemCnt(); i++) {
		ret = netlib_listen(listen_ip_list.GetItem(i), listen_port, http_callback, NULL);
		if (ret == NETLIB_ERROR)
			return ret;
	}

	printf("server start listen on: %s:%d\n", listen_ip, listen_port);
    
	init_http_conn();
    
	if (db_server_count > 0) {
		init_db_serv_conn(db_server_list2, db_server_count2, concurrent_db_conn_cnt);
	}

	if (route_server_count > 0) {
		init_route_serv_conn(route_server_list, route_server_count);
	}



	//websocket服务开启
	CStrExplode ws_listen_ip_list(websocket_listen_ip, ';');
	for (uint32_t i = 0; i < ws_listen_ip_list.GetItemCnt(); i++) {
		ret = netlib_listen(ws_listen_ip_list.GetItem(i), listen_ws_port, websocket_serv_callback, NULL);
		if (ret == NETLIB_ERROR)
			return ret;
	}
	init_websocket_timer_callback();
    printf("server start websocket server listen on: %s:%d\n", websocket_listen_ip, listen_ws_port);







	printf("now enter the event loop...\n");
    
    writePid();

	netlib_eventloop();
    
	return 0;
}
