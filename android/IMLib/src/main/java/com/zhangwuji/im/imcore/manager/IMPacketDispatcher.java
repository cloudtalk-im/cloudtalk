package com.zhangwuji.im.imcore.manager;

import android.util.Log;

import com.google.protobuf.CodedInputStream;
import com.zhangwuji.im.protobuf.IMBaseDefine;
import com.zhangwuji.im.protobuf.IMBuddy;
import com.zhangwuji.im.protobuf.IMGroup;
import com.zhangwuji.im.protobuf.IMLogin;
import com.zhangwuji.im.protobuf.IMMessage;
import com.zhangwuji.im.protobuf.IMSwitchService;
import com.zhangwuji.im.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * yingmu
 * 消息分发中心，处理消息服务器返回的数据包
 * 1. decode  header与body的解析
 * 2. 分发
 */
public class IMPacketDispatcher {
	private static Logger logger = Logger.getLogger(IMPacketDispatcher.class);

    /**
     * @param commandId
     * @param buffer
     *
     * 有没有更加优雅的方式
     */
    public static void loginPacketDispatcher(int commandId,CodedInputStream buffer){
        try {
        switch (commandId) {
//            case IMBaseDefine.LoginCmdID.CID_LOGIN_RES_USERLOGIN_VALUE :
//                IMLogin.IMLoginRes  imLoginRes = IMLogin.IMLoginRes.parseFrom(buffer);
//                IMLoginManager.instance().onRepMsgServerLogin(imLoginRes);
//                return;
            case IMBaseDefine.LoginCmdID.CID_LOGIN_RES_LOGINOUT_VALUE:
                IMLogin.IMLogoutRsp imLogoutRsp = IMLogin.IMLogoutRsp.parseFrom(buffer);
                IMLoginManager.instance().onRepLoginOut(imLogoutRsp);
                return;

            case IMBaseDefine.LoginCmdID.CID_LOGIN_KICK_USER_VALUE:
                IMLogin.IMKickUser imKickUser = IMLogin.IMKickUser.parseFrom(buffer);
                IMLoginManager.instance().onKickout(imKickUser);
            }
        } catch (IOException e) {
            logger.e("loginPacketDispatcher# error,cid:%d",commandId);
        }
    }

    public static void buddyPacketDispatcher(int commandId,CodedInputStream buffer){
        try {
        switch (commandId) {
            case IMBaseDefine.BuddyListCmdID.CID_BUDDY_LIST_ALL_USER_RESPONSE_VALUE:
                    IMBuddy.IMAllUserRsp imAllUserRsp = IMBuddy.IMAllUserRsp.parseFrom(buffer);
                 //   IMContactManager.instance().onRepAllUsers(imAllUserRsp);
                return;

            case IMBaseDefine.BuddyListCmdID.CID_BUDDY_LIST_USER_INFO_RESPONSE_VALUE:
                   //IMBuddy.IMUsersInfoRsp imUsersInfoRsp = IMBuddy.IMUsersInfoRsp.parseFrom(buffer);
                   //IMContactManager.instance().onRepDetailUsers(imUsersInfoRsp);
                return;
            case IMBaseDefine.BuddyListCmdID.CID_BUDDY_LIST_RECENT_CONTACT_SESSION_RESPONSE_VALUE:
                IMBuddy.IMRecentContactSessionRsp recentContactSessionRsp = IMBuddy.IMRecentContactSessionRsp.parseFrom(buffer);
                IMSessionManager.instance().onRepRecentContacts(recentContactSessionRsp);
                return;

            case IMBaseDefine.BuddyListCmdID.CID_BUDDY_LIST_REMOVE_SESSION_RES_VALUE:
                IMBuddy.IMRemoveSessionRsp removeSessionRsp = IMBuddy.IMRemoveSessionRsp.parseFrom(buffer);
                    IMSessionManager.instance().onRepRemoveSession(removeSessionRsp);
                return;
            case IMBaseDefine.BuddyListCmdID.CID_BUDDY_LIST_PC_LOGIN_STATUS_NOTIFY_VALUE:
                IMBuddy.IMPCLoginStatusNotify statusNotify = IMBuddy.IMPCLoginStatusNotify.parseFrom(buffer);
                IMLoginManager.instance().onLoginStatusNotify(statusNotify);
                return;

            case IMBaseDefine.BuddyListCmdID.CID_BUDDY_LIST_DEPARTMENT_RESPONSE_VALUE:
                IMBuddy.IMDepartmentRsp departmentRsp = IMBuddy.IMDepartmentRsp.parseFrom(buffer);
              //  IMContactManager.instance().onRepDepartment(departmentRsp);
                return;

        }
        } catch (IOException e) {
            logger.e("buddyPacketDispatcher# error,cid:%d",commandId);
        }
    }

    public static void msgPacketDispatcher(int commandId,CodedInputStream buffer){
        try {
        switch (commandId) {
            case  IMBaseDefine.MessageCmdID.CID_MSG_DATA_ACK_VALUE:
                // have some problem  todo
            return;

            case IMBaseDefine.MessageCmdID.CID_MSG_LIST_RESPONSE_VALUE:
                IMMessage.IMGetMsgListRsp rsp = IMMessage.IMGetMsgListRsp.parseFrom(buffer);
                IMMessageManager.instance().onReqHistoryMsg(rsp);
            return;

            case IMBaseDefine.MessageCmdID.CID_MSG_DATA_VALUE:
                IMMessage.IMMsgData imMsgData = IMMessage.IMMsgData.parseFrom(buffer);
                IMMessageManager.instance().onRecvMessage(imMsgData);
                return;

            case IMBaseDefine.MessageCmdID.CID_MSG_READ_NOTIFY_VALUE:
                IMMessage.IMMsgDataReadNotify readNotify = IMMessage.IMMsgDataReadNotify.parseFrom(buffer);
                IMUnreadMsgManager.instance().onNotifyRead(readNotify);
                return;
            case IMBaseDefine.MessageCmdID.CID_MSG_UNREAD_CNT_RESPONSE_VALUE:
                IMMessage.IMUnreadMsgCntRsp unreadMsgCntRsp = IMMessage.IMUnreadMsgCntRsp.parseFrom(buffer);
                IMUnreadMsgManager.instance().onRepUnreadMsgContactList(unreadMsgCntRsp);
                return;

            case IMBaseDefine.MessageCmdID.CID_MSG_GET_BY_MSG_ID_RES_VALUE:
                IMMessage.IMGetMsgByIdRsp getMsgByIdRsp = IMMessage.IMGetMsgByIdRsp.parseFrom(buffer);
                IMMessageManager.instance().onReqMsgById(getMsgByIdRsp);
                break;
        }
        } catch (IOException e) {
            logger.e("msgPacketDispatcher# error,cid:%d",commandId);
        }
    }

    public static void p2pcmdPacketDispatcher(int commandId,CodedInputStream buffer){
        try {
            switch (commandId) {
                case IMBaseDefine.SwitchServiceCmdID.CID_SWITCH_P2P_CMD_VALUE:
                    IMSwitchService.IMP2PCmdMsg p2pmsg = IMSwitchService.IMP2PCmdMsg.parseFrom(buffer);
                     String data=p2pmsg.getCmdMsgData();
                    try {
                        JSONObject jsonObject = new JSONObject(data);
                        int cmd=jsonObject.getInt("cmd_id");
                        if(cmd==66666)//收到视频通话
                        {
                            IMMessageManager.instance().recVideoMsg(p2pmsg);
                        }
                        else if(cmd==66668) //对方已经收到视频请求的应答。15秒未收到应答，表示对方不在线
                        {
                            IMMessageManager.instance().sendVideoactOk(p2pmsg);
                        }
                        else if(cmd==66669) //对方已结束通话
                        {
                            IMMessageManager.instance().endofVideo(p2pmsg);
                        }
                        else if(cmd==66660) //对方拒绝
                        {
                            IMMessageManager.instance().rejectofVideo(p2pmsg);
                        }
                        else if(cmd==66661) //对方已同意准备开始
                        {
                            IMMessageManager.instance().agreeofVideo(p2pmsg);
                        }
                    } catch (JSONException e) {}

                    Log.v("IMSwitchService==",data);
                    return;
            }
        }catch(IOException e){
            logger.e("groupPacketDispatcher# error,cid:%d",commandId);
        }
    }

    public static void groupPacketDispatcher(int commandId,CodedInputStream buffer){
        try {
            switch (commandId) {
                case IMBaseDefine.GroupCmdID.CID_GROUP_CHANGE_MEMBER_NOTIFY_VALUE:
                    IMGroup.IMGroupChangeMemberNotify notify = IMGroup.IMGroupChangeMemberNotify.parseFrom(buffer);
                    IMGroupManager.instance().receiveGroupChangeMemberNotify(notify);
                case IMBaseDefine.GroupCmdID.CID_GROUP_SHIELD_GROUP_RESPONSE_VALUE:
                    //todo
                    return;
            }
        }catch(IOException e){
            logger.e("groupPacketDispatcher# error,cid:%d",commandId);
            }
    }
}
