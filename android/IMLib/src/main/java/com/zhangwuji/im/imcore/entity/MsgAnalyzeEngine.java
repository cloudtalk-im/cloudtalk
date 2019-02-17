package com.zhangwuji.im.imcore.entity;

import android.text.TextUtils;

import com.zhangwuji.im.DB.entity.Message;
import com.zhangwuji.im.Security;
import com.zhangwuji.im.config.DBConstant;
import com.zhangwuji.im.config.MessageConstant;
import com.zhangwuji.im.protobuf.helper.ProtoBuf2JavaBean;
import com.zhangwuji.im.protobuf.IMBaseDefine;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : yingmu on 15-1-6.
 * @email : yingmu@mogujie.com.
 *
 * historical reasons,没有充分利用msgType字段
 * 多端的富文本的考虑
 */
public class MsgAnalyzeEngine {
    public static String analyzeMessageDisplay(String content){
        String finalRes = content;
        String originContent = content;
        while (!originContent.isEmpty()) {
            int nStart = originContent.indexOf(MessageConstant.IMAGE_MSG_START);
            if (nStart < 0) {// 没有头
                break;
            } else {
                String subContentString = originContent.substring(nStart);
                int nEnd = subContentString.indexOf(MessageConstant.IMAGE_MSG_END);
                if (nEnd < 0) {// 没有尾
                    String strSplitString = originContent;
                    break;
                } else {// 匹配到
                    String pre = originContent.substring(0, nStart);

                    originContent = subContentString.substring(nEnd
                            + MessageConstant.IMAGE_MSG_END.length());

                    if(!TextUtils.isEmpty(pre) || !TextUtils.isEmpty(originContent)){
                        finalRes = DBConstant.DISPLAY_FOR_MIX;
                    }else{
                        finalRes = DBConstant.DISPLAY_FOR_IMAGE;
                    }
                }
            }
        }
        return finalRes;
    }


    // 抽离放在同一的地方
    public static Message analyzeMessage(IMBaseDefine.MsgInfo msgInfo) {
       Message messageEntity = new Message();

       messageEntity.setCreated(msgInfo.getCreateTime());
       messageEntity.setUpdated(msgInfo.getCreateTime());
       messageEntity.setFromId(msgInfo.getFromSessionId());
       messageEntity.setMsgId(msgInfo.getMsgId());
       messageEntity.setMsgType(ProtoBuf2JavaBean.getJavaMsgType(msgInfo.getMsgType()));
       messageEntity.setStatus(MessageConstant.MSG_SUCCESS);
       messageEntity.setContent(msgInfo.getMsgData().toStringUtf8());
        /**
         * 解密文本信息
         */
       String desMessage = new String(Security.getInstance().DecryptMsg(msgInfo.getMsgData().toStringUtf8()));
       messageEntity.setContent(desMessage);

       // 文本信息不为空
       if(!TextUtils.isEmpty(desMessage)){
           List<Message> msgList =  textDecode(messageEntity);
           if(msgList.size()>1){
               // 混合消息
               MixMessage mixMessage = new MixMessage(msgList);
               return mixMessage;
           }else if(msgList.size() == 0){
              // 可能解析失败 默认返回文本消息
              return TextMessage.parseFromNet(messageEntity);
           }else{
               //简单消息，返回第一个
               return msgList.get(0);
           }
       }else{
           // 如果为空
           return TextMessage.parseFromNet(messageEntity);
       }
    }


    /**
     * todo 优化字符串分析
     * @param msg
     * @return
     */
    private static List<Message> textDecode(Message msg){
        List<Message> msgList = new ArrayList<>();

        String originContent = msg.getContent();
        while (!TextUtils.isEmpty(originContent)) {
            int nStart = originContent.indexOf(MessageConstant.IMAGE_MSG_START);
            if (nStart < 0) {// 没有头
                String strSplitString = originContent;

                Message entity = addMessage(msg, strSplitString);
                if(entity!=null){
                    msgList.add(entity);
                }

                originContent = "";
            } else {
                String subContentString = originContent.substring(nStart);
                int nEnd = subContentString.indexOf(MessageConstant.IMAGE_MSG_END);
                if (nEnd < 0) {// 没有尾
                    String strSplitString = originContent;


                    Message entity = addMessage(msg,strSplitString);
                    if(entity!=null){
                        msgList.add(entity);
                    }

                    originContent = "";
                } else {// 匹配到
                    String pre = originContent.substring(0, nStart);
                    Message entity1 = addMessage(msg,pre);
                    if(entity1!=null){
                        msgList.add(entity1);
                    }

                    String matchString = subContentString.substring(0, nEnd
                            + MessageConstant.IMAGE_MSG_END.length());

                    Message entity2 = addMessage(msg,matchString);
                    if(entity2!=null){
                        msgList.add(entity2);
                    }

                    originContent = subContentString.substring(nEnd
                            + MessageConstant.IMAGE_MSG_END.length());
                }
            }
        }

        return msgList;
    }


    public static Message addMessage(Message msg, String strContent) {
        if (TextUtils.isEmpty(strContent.trim())){
            return null;
        }
        msg.setContent(strContent);

        if (strContent.startsWith(MessageConstant.IMAGE_MSG_START)
                && strContent.endsWith(MessageConstant.IMAGE_MSG_END)) {
            try {
                ImageMessage imageMessage =  ImageMessage.parseFromNet(msg);
                return imageMessage;
            } catch (JSONException e) {
                // e.printStackTrace();
                return null;
            }
        } else {
           return TextMessage.parseFromNet(msg);
        }
    }

}
