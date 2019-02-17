package com.zhangwuji.im.imcore.manager;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.CodedInputStream;
import com.zhangwuji.im.DB.entity.Department;
import com.zhangwuji.im.DB.entity.Group;
import com.zhangwuji.im.config.DBConstant;
import com.zhangwuji.im.DB.DBInterface;
import com.zhangwuji.im.DB.entity.Session;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.imcore.callback.Packetlistener;
import com.zhangwuji.im.imcore.event.GroupEvent;
import com.zhangwuji.im.imcore.event.SessionEvent;
import com.zhangwuji.im.imcore.event.UserInfoEvent;
import com.zhangwuji.im.protobuf.helper.EntityChangeEngine;
import com.zhangwuji.im.protobuf.helper.ProtoBuf2JavaBean;
import com.zhangwuji.im.protobuf.IMBaseDefine;
import com.zhangwuji.im.protobuf.IMGroup;
import com.zhangwuji.im.server.network.BaseAction;
import com.zhangwuji.im.server.network.IMAction;
import com.zhangwuji.im.server.utils.json.JsonMananger;
import com.zhangwuji.im.utils.IMUIHelper;
import com.zhangwuji.im.utils.Logger;
import com.zhangwuji.im.utils.pinyin.PinYin;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class IMGroupManager extends IMManager {
    private Logger logger = Logger.getLogger(IMGroupManager.class);
    private static IMGroupManager inst = new IMGroupManager();
    public static IMGroupManager instance() {
        return inst;
    }

    // 依赖的服务管理
    private IMSocketManager imSocketManager = IMSocketManager.instance();
    private IMLoginManager imLoginManager=IMLoginManager.instance();
    private DBInterface dbInterface = DBInterface.instance();


    // todo Pinyin的处理
    //正式群,临时群都会有的，存在竞争 如果不同时请求的话
    private Map<Integer,Group> groupMap = new ConcurrentHashMap<>();
    // 群组状态
    private boolean isGroupReady = false;
    private IMAction imAction;
    @Override
    public void doOnStart() {
        imAction=new IMAction(ctx);
        groupMap.clear();
    }

    public void onNormalLoginOk(){
        onLocalLoginOk();
        onLocalNetOk();
    }

    /**
     * 1. 加载本地信息
     * 2. 请求正规群信息 ， 与本地进行对比
     * 3. version groupId 请求
     * */
    public void onLocalLoginOk(){
        logger.i("group#loadFromDb");

        if(!EventBus.getDefault().isRegistered(inst)){
            EventBus.getDefault().register(inst);
        }

        // 加载本地group
        List<Group> localGroupInfoList = dbInterface.loadAllGroup();
        for(Group groupInfo:localGroupInfoList){
            groupMap.put(groupInfo.getPeerId(),groupInfo);
        }

        triggerEvent(new GroupEvent(GroupEvent.Event.GROUP_INFO_OK));
    }

    public void onLocalNetOk(){
        reqGetNormalGroupList();
    }

    @Override
    public void reset() {
        isGroupReady =false;
        groupMap.clear();
        EventBus.getDefault().unregister(inst);
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent(SessionEvent event){
        switch (event){
            case RECENT_SESSION_LIST_UPDATE:
                // groupMap 本地已经加载完毕之后才触发
                loadSessionGroupInfo();
                break;
        }
    }

    /**
     * 实现自身的事件驱动
     * @param event
     */
    public  synchronized void triggerEvent(GroupEvent event) {
        switch (event.getEvent()){
            case GROUP_INFO_OK:
                isGroupReady = true;
                break;
            case GROUP_INFO_UPDATED:
                isGroupReady = true;
                break;
        }
        EventBus.getDefault().postSticky(event);
    }

    /**---------------事件驱动end------------------------------*/

    /**
     * 1. 加载本地信息
     * 2. 从session中获取 群组信息
     * 3. 合并上述的merge结果， version groupId 请求
     * */
    private void loadSessionGroupInfo(){
        logger.i("group#loadSessionGroupInfo");

        List<Session> sessionInfoList =   IMSessionManager.instance().getRecentSessionList();

        ArrayList<Integer> needReqList = new ArrayList<>();
        for(Session sessionInfo:sessionInfoList){
            if(sessionInfo.getPeerType() == DBConstant.SESSION_TYPE_GROUP /**群组*/){
                if(!groupMap.containsKey(sessionInfo.getPeerId()))
                {
                    needReqList.add(sessionInfo.getPeerId());
                }
            }
        }
        // 事件触发的时候需要注意
        if(needReqList.size() >0){
            reqGetGroupDetailInfo(needReqList);
            return ;
        }
    }

    /**
     * 联系人页面正式群的请求
     */
    private void reqGetNormalGroupList() {
        logger.i("group#reqGetNormalGroupList");

        imAction.getGroupList(new BaseAction.ResultCallback<String>() {
            @Override
            public void onSuccess(String s) {
                ArrayList<Group> needDb1 = new ArrayList<>();
                JSONObject object=JSONObject.parseObject(s);
                if(object.getIntValue("code")==200)
                {
                    JSONObject data =object.getJSONObject("data");
                    //*******解析群组列表************
                    JSONArray alllist1=data.getJSONArray("grouplist");
                    for(int i=0;i<alllist1.size();i++)
                    {
                        Group tempDep=JsonMananger.parseGroup(alllist1.getJSONObject(i));
                        needDb1.add(tempDep);
                        if(tempDep.getGroupType()<3) {
                            groupMap.put(tempDep.getPeerId(), tempDep);
                        }
                    }
                    if(needDb1.size()>0)
                    {
                        dbInterface.batchInsertOrUpdateGroup(needDb1);
                        triggerEvent(new GroupEvent(GroupEvent.Event.GROUP_INFO_UPDATED));
                    }
                }
            }
            @Override
            public void onError(String errString) {
            }
        });
    }

    public void  reqGroupDetailInfo(int groupId){
        ArrayList<Integer> list = new ArrayList<>();
        list.add(groupId);
        reqGetGroupDetailInfo(list);
    }

    /**
     * 请求群组的详细信息
     */
    public void reqGetGroupDetailInfo(ArrayList<Integer>  groupids){
        logger.i("group#reqGetGroupDetailInfo");
        if(groupids==null || groupids.size()<=0)return;

        String ids="";
        for(int s:groupids)
        {
            ids+=s+",";
        }
        if(ids!="")
        {
            ids=ids.substring(0,ids.length()-1);
        }
        imAction.getGroupInfo(ids,new BaseAction.ResultCallback<String>() {
            @Override
            public void onSuccess(String s) {
                ArrayList<Group> needDb1 = new ArrayList<>();
                JSONObject object=JSONObject.parseObject(s);
                if(object.getIntValue("code")==200)
                {
                    JSONObject data =object.getJSONObject("data");
                    //*******解析群组列表************
                    JSONArray alllist1=data.getJSONArray("grouplist");
                    for(int i=0;i<alllist1.size();i++)
                    {
                        Group tempDep=JsonMananger.parseGroup(alllist1.getJSONObject(i));
                        needDb1.add(tempDep);
                        if(!groupMap.containsKey(tempDep.getPeerId()))
                        {
                            groupMap.put(tempDep.getPeerId(),tempDep);
                        }
                    }
                    if(needDb1.size()>0)
                    {
                        dbInterface.batchInsertOrUpdateGroup(needDb1);
                        triggerEvent(new GroupEvent(GroupEvent.Event.GROUP_INFO_UPDATED));
                    }
                }
            }
            @Override
            public void onError(String errString) {

            }
        });

    }


    /**
     * 创建群
     * 默认是创建临时群，且客户端只能创建临时群
     */
    public void reqCreateTempGroup(String groupName, Set<Integer> memberList){

        logger.i("group#reqCreateTempGroup, tempGroupName = %s", groupName);

        int loginId = imLoginManager.getLoginId();

        IMGroup.IMGroupCreateReq groupCreateReq  = IMGroup.IMGroupCreateReq.newBuilder()
                .setUserId(loginId)
                .setGroupType(IMBaseDefine.GroupType.GROUP_TYPE_NORMAL)
                .setGroupName(groupName)
                .setGroupAvatar("")// todo 群头像 现在是四宫格
                .addAllMemberIdList(memberList)
                .build();

        int sid = IMBaseDefine.ServiceID.SID_GROUP_VALUE;
        int cid = IMBaseDefine.GroupCmdID.CID_GROUP_CREATE_REQUEST_VALUE;
        imSocketManager.sendRequest(groupCreateReq, sid, cid,new Packetlistener() {
            @Override
            public void onSuccess(Object response) {
                try {
                    IMGroup.IMGroupCreateRsp groupCreateRsp  = IMGroup.IMGroupCreateRsp.parseFrom((CodedInputStream)response);
                    IMGroupManager.instance().onReqCreateTempGroup(groupCreateRsp);
                } catch (IOException e) {
                    logger.e("reqCreateTempGroup parse error");
                    triggerEvent(new GroupEvent(GroupEvent.Event.CREATE_GROUP_FAIL));
                }
            }

            @Override
            public void onFaild() {
              triggerEvent(new GroupEvent(GroupEvent.Event.CREATE_GROUP_FAIL));
            }

            @Override
            public void onTimeout() {
              triggerEvent(new GroupEvent(GroupEvent.Event.CREATE_GROUP_TIMEOUT));
            }
        });
    }

    public void onReqCreateTempGroup(IMGroup.IMGroupCreateRsp groupCreateRsp){
        logger.d("group#onReqCreateTempGroup");

        int resultCode = groupCreateRsp.getResultCode();
        if(0 != resultCode){
            logger.e("group#createGroup failed");
            triggerEvent(new GroupEvent(GroupEvent.Event.CREATE_GROUP_FAIL));
            return;
        }
        Group groupEntity = ProtoBuf2JavaBean.getGroupEntity(groupCreateRsp);
        // 更新DB 更新map
        groupMap.put(groupEntity.getPeerId(),groupEntity);

        IMSessionManager.instance().updateSession(groupEntity);
        dbInterface.insertOrUpdateGroup(groupEntity);
        triggerEvent(new GroupEvent(GroupEvent.Event.CREATE_GROUP_OK, groupEntity)); // 接收到之后修改UI
    }

    /**
     * 删除群成员
     * REMOVE_CHANGE_MEMBER_TYPE
     * 可能会触发头像的修改
     */
    public void reqRemoveGroupMember(int groupId,Set<Integer> removeMemberlist){
        reqChangeGroupMember(groupId,IMBaseDefine.GroupModifyType.GROUP_MODIFY_TYPE_DEL, removeMemberlist);
    }
    /**
     * 新增群成员
     * ADD_CHANGE_MEMBER_TYPE
     * 可能会触发头像的修改
     */
    public void reqAddGroupMember(int groupId,Set<Integer> addMemberlist){
        reqChangeGroupMember(groupId,IMBaseDefine.GroupModifyType.GROUP_MODIFY_TYPE_ADD, addMemberlist);
    }

    private void reqChangeGroupMember(int groupId,IMBaseDefine.GroupModifyType groupModifyType, Set<Integer> changeMemberlist) {
        logger.i("group#reqChangeGroupMember, changeGroupMemberType = %s", groupModifyType.toString());

        final int loginId = imLoginManager.getLoginId();
        IMGroup.IMGroupChangeMemberReq groupChangeMemberReq = IMGroup.IMGroupChangeMemberReq.newBuilder()
                .setUserId(loginId)
                .setChangeType(groupModifyType)
                .addAllMemberIdList(changeMemberlist)
                .setGroupId(groupId)
                .build();

        int sid = IMBaseDefine.ServiceID.SID_GROUP_VALUE;
        int cid = IMBaseDefine.GroupCmdID.CID_GROUP_CHANGE_MEMBER_REQUEST_VALUE;
        imSocketManager.sendRequest(groupChangeMemberReq, sid, cid,new Packetlistener() {
            @Override
            public void onSuccess(Object response) {
                try {
                    IMGroup.IMGroupChangeMemberRsp groupChangeMemberRsp = IMGroup.IMGroupChangeMemberRsp.parseFrom((CodedInputStream)response);
                    IMGroupManager.instance().onReqChangeGroupMember(groupChangeMemberRsp);
                } catch (IOException e) {
                    logger.e("reqChangeGroupMember parse error!");
                    triggerEvent(new GroupEvent(GroupEvent.Event.CHANGE_GROUP_MEMBER_FAIL));
                }
            }

            @Override
            public void onFaild() {
                triggerEvent(new GroupEvent(GroupEvent.Event.CHANGE_GROUP_MEMBER_FAIL));
            }

            @Override
            public void onTimeout() {
                triggerEvent(new GroupEvent(GroupEvent.Event.CHANGE_GROUP_MEMBER_TIMEOUT));
            }
        });
    }

    public void onReqChangeGroupMember(IMGroup.IMGroupChangeMemberRsp groupChangeMemberRsp){
        int resultCode = groupChangeMemberRsp.getResultCode();
        if (0 != resultCode){
            triggerEvent(new GroupEvent(GroupEvent.Event.CHANGE_GROUP_MEMBER_FAIL));
            return;
        }

        int groupId = groupChangeMemberRsp.getGroupId();
        List<Integer> changeUserIdList = groupChangeMemberRsp.getChgUserIdListList();
        IMBaseDefine.GroupModifyType groupModifyType = groupChangeMemberRsp.getChangeType();


        Group groupEntityRet = groupMap.get(groupId);
        groupEntityRet.setlistGroupMemberIds(groupChangeMemberRsp.getCurUserIdListList());
        groupMap.put(groupId,groupEntityRet);
        dbInterface.insertOrUpdateGroup(groupEntityRet);


        GroupEvent groupEvent = new GroupEvent(GroupEvent.Event.CHANGE_GROUP_MEMBER_SUCCESS);
        groupEvent.setChangeList(changeUserIdList);
        groupEvent.setChangeType(ProtoBuf2JavaBean.getGroupChangeType(groupModifyType));
        groupEvent.setGroupEntity(groupEntityRet);
        triggerEvent(groupEvent);
    }

    /**
     * 屏蔽群消息
     * IMGroupShieldReq
     * 备注:应为屏蔽之后大部分操作依旧需要客户端做
     * */
    public void reqShieldGroup(final int groupId,final int shieldType){
        final Group entity =  groupMap.get(groupId);
        if(entity == null){
            logger.i("Group do not exist!");
            return;
        }
        final int loginId = IMLoginManager.instance().getLoginId();
        IMGroup.IMGroupShieldReq shieldReq = IMGroup.IMGroupShieldReq.newBuilder()
                .setShieldStatus(shieldType)
                .setGroupId(groupId)
                .setUserId(loginId)
                .build();
        int sid = IMBaseDefine.ServiceID.SID_GROUP_VALUE;
        int cid = IMBaseDefine.GroupCmdID.CID_GROUP_SHIELD_GROUP_REQUEST_VALUE;
        imSocketManager.sendRequest(shieldReq,sid,cid,new Packetlistener() {
            @Override
            public void onSuccess(Object response) {
                try {
                    IMGroup.IMGroupShieldRsp groupShieldRsp = IMGroup.IMGroupShieldRsp.parseFrom((CodedInputStream)response);
                    int resCode = groupShieldRsp.getResultCode();
                    if(resCode !=0){
                        triggerEvent(new GroupEvent(GroupEvent.Event.SHIELD_GROUP_FAIL));
                        return;
                    }
                    if(groupShieldRsp.getGroupId() != groupId || groupShieldRsp.getUserId()!=loginId){
                        return;
                    }
                    // 更新DB状态
                    entity.setStatus(shieldType);
                    dbInterface.insertOrUpdateGroup(entity);
                    // 更改未读计数状态
                    boolean isFor = shieldType == DBConstant.GROUP_STATUS_SHIELD;
                    IMUnreadMsgManager.instance().setForbidden(
                            EntityChangeEngine.getSessionKey(groupId,DBConstant.SESSION_TYPE_GROUP),isFor);
                    triggerEvent(new GroupEvent(GroupEvent.Event.SHIELD_GROUP_OK,entity));

                } catch (IOException e) {
                    logger.e("reqChangeGroupMember parse error!");
                    triggerEvent(new GroupEvent(GroupEvent.Event.SHIELD_GROUP_FAIL));
                }
            }
            @Override
            public void onFaild() {
                triggerEvent(new GroupEvent(GroupEvent.Event.SHIELD_GROUP_FAIL));
            }

            @Override
            public void onTimeout() {
                triggerEvent(new GroupEvent(GroupEvent.Event.SHIELD_GROUP_TIMEOUT));
            }
        });
    }


    /**
     * 收到群成员发生变更消息
     * 服务端主动发出
     * DB
     */
    public void receiveGroupChangeMemberNotify(IMGroup.IMGroupChangeMemberNotify notify){
       int groupId =  notify.getGroupId();
       int changeType = ProtoBuf2JavaBean.getGroupChangeType(notify.getChangeType());
       List<Integer> changeList =  notify.getChgUserIdListList();

       List<Integer> curMemberList = notify.getCurUserIdListList();
       if(groupMap.containsKey(groupId)){
           Group entity = groupMap.get(groupId);
           entity.setlistGroupMemberIds(curMemberList);
           dbInterface.insertOrUpdateGroup(entity);
           groupMap.put(groupId,entity);

           GroupEvent groupEvent = new GroupEvent(GroupEvent.Event.CHANGE_GROUP_MEMBER_SUCCESS);
           groupEvent.setChangeList(changeList);
           groupEvent.setChangeType(changeType);
           groupEvent.setGroupEntity(entity);
           triggerEvent(groupEvent);
       }else{
           //todo 没有就暂时不管了，只要聊过天都会显示在回话里面
       }
    }

	public List<Group> getNormalGroupList() {
		List<Group> normalGroupList = new ArrayList<>();
		for (Entry<Integer, Group> entry : groupMap.entrySet()) {
			Group group = entry.getValue();
			if (group == null) {
				continue;
			}
			if (group.getGroupType() == DBConstant.GROUP_TYPE_NORMAL) {
				normalGroupList.add(group);
			}
		}
		return normalGroupList;
	}

    // 该方法只有正式群
    // todo eric efficiency
    public  List<Group> getNormalGroupSortedList() {
        List<Group> groupList = getNormalGroupList();
        Collections.sort(groupList, new Comparator<Group>(){
            @Override
            public int compare(Group entity1, Group entity2) {
                if(entity1.getPinyinElement().pinyin==null)
                {
                    PinYin.getPinYin(entity1.getMainName(), entity1.getPinyinElement());
                }
                if(entity2.getPinyinElement().pinyin==null)
                {
                    PinYin.getPinYin(entity2.getMainName(),entity2.getPinyinElement());
                }
                return entity1.getPinyinElement().pinyin.compareToIgnoreCase(entity2.getPinyinElement().pinyin);
            }
        });

        return groupList;
    }

	public Group findGroup(int groupId) {
		logger.d("group#findGroup groupId:%s", groupId);
        if(groupMap.containsKey(groupId)){
            return groupMap.get(groupId);
        }
        else
        {
            Group group=dbInterface.getGroup(groupId); //看数据库里有没有记录。临时群和聊天室都可以
            return group;
        }
	}

    public List<Group>  getSearchAllGroupList(String key){
        List<Group> searchList = new ArrayList<>();
        for(Map.Entry<Integer,Group> entry:groupMap.entrySet()){
            Group groupEntity = entry.getValue();
            if (IMUIHelper.handleGroupSearch(key, groupEntity)) {
                searchList.add(groupEntity);
            }
        }
        return searchList;
    }

	public List<User> getGroupMembers(int groupId) {
		logger.d("group#getGroupMembers groupId:%s", groupId);

		Group group = findGroup(groupId);
		if (group == null) {
			logger.e("group#no such group id:%s", groupId);
			return null;
		}
        Set<Integer> userList = group.getlistGroupMemberIds();
		ArrayList<User> memberList = new ArrayList<User>();
		for (Integer id : userList) {
			User contact = IMContactManager.instance().findContact(id);
			if (contact == null) {
				logger.e("group#no such contact id:%s", id);
				continue;
			}
			memberList.add(contact);
		}
		return memberList;
	}

    /**------set/get 的定义*/
    public Map<Integer, Group> getGroupMap() {
        return groupMap;
    }

    public boolean isGroupReady() {
        return isGroupReady;
    }
}
