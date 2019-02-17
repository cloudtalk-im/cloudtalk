package com.zhangwuji.im.imcore.event;

import com.zhangwuji.im.DB.entity.Message;

import java.util.ArrayList;

/**
 * @author : yingmu on 14-12-30.
 * @email : yingmu@mogujie.com.
 *
 */
public class MessageEvent {

    private ArrayList<Message> msgList;
    private Event event;

    public MessageEvent(){
    }

    public MessageEvent(Event event){
        //默认值 初始化使用
        this.event = event;
    }

    public MessageEvent(Event event,Message entity){
        //默认值 初始化使用
        this.event = event;
        msgList = new ArrayList<>(1);
        msgList.add(entity);
    }

    public enum Event{
      NONE,
      HISTORY_MSG_OBTAIN,

      SENDING_MESSAGE,

      ACK_SEND_MESSAGE_OK,
       ACK_SEND_MESSAGE_TIME_OUT,
      ACK_SEND_MESSAGE_FAILURE,

      HANDLER_IMAGE_UPLOAD_FAILD,
        IMAGE_UPLOAD_FAILD,
        HANDLER_IMAGE_UPLOAD_SUCCESS,
        IMAGE_UPLOAD_SUCCESS,
        SENDPUSHLIST,
        PLUGINCOMPLETE
     }

    public Message getMessageEntity() {
        if(msgList == null || msgList.size() <=0){
            return null;
        }
        return msgList.get(0);
    }

    public void setMessageEntity(Message messageEntity) {
        if(msgList == null){
            msgList = new ArrayList<>();
        }
        msgList.clear();
        msgList.add(messageEntity);
    }

    public ArrayList<Message> getMsgList() {
        return msgList;
    }

    public void setMsgList(ArrayList<Message> msgList) {
        this.msgList = msgList;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}
