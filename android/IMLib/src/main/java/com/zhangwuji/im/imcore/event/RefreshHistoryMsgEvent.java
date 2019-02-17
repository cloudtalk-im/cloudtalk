package com.zhangwuji.im.imcore.event;

import com.zhangwuji.im.DB.entity.Message;

import java.util.List;

/**
 * @author : yingmu on 15-3-26.
 * @email : yingmu@mogujie.com.
 *
 * 异步刷新历史消息
 */
public class RefreshHistoryMsgEvent {
   public int pullTimes;
   public int lastMsgId;
   public int count;
   public List<Message> listMsg;
   public int peerId;
   public int peerType;
   public String sessionKey;

   public RefreshHistoryMsgEvent(){}

}
