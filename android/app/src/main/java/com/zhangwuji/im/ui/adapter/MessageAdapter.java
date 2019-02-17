package com.zhangwuji.im.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhangwuji.im.DB.entity.Message;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.config.DBConstant;
import com.zhangwuji.im.R;
import com.zhangwuji.im.config.MessageConstant;
import com.zhangwuji.im.imcore.entity.MessageTag;
import com.zhangwuji.im.ui.helper.AudioPlayerHandler;
import com.zhangwuji.im.config.IntentConstant;
import com.zhangwuji.im.imcore.entity.AudioMessage;
import com.zhangwuji.im.imcore.entity.ImageMessage;
import com.zhangwuji.im.imcore.entity.MixMessage;
import com.zhangwuji.im.imcore.entity.TextMessage;
import com.zhangwuji.im.imcore.service.IMService;
import com.zhangwuji.im.ui.activity.PreviewGifActivity;
import com.zhangwuji.im.ui.activity.PreviewMessageImagesActivity;
import com.zhangwuji.im.ui.activity.PreviewTextActivity;
import com.zhangwuji.im.ui.helper.Emoparser;
import com.zhangwuji.im.ui.plugin.ExtensionModule;
import com.zhangwuji.im.ui.plugin.IMessageData;
import com.zhangwuji.im.ui.plugin.IMessageModule;
import com.zhangwuji.im.ui.widget.GifView;
import com.zhangwuji.im.ui.plugin.message.BigMojiImageRenderView;
import com.zhangwuji.im.ui.widget.message.GifImageRenderView;
import com.zhangwuji.im.utils.CommonUtil;
import com.zhangwuji.im.utils.DateUtil;
import com.zhangwuji.im.utils.FileUtil;
import com.zhangwuji.im.utils.Logger;
import com.zhangwuji.im.ui.widget.SpeekerToast;
import com.zhangwuji.im.ui.widget.message.AudioRenderView;
import com.zhangwuji.im.ui.widget.message.EmojiRenderView;
import com.zhangwuji.im.ui.widget.message.ImageRenderView;
import com.zhangwuji.im.ui.widget.message.MessageOperatePopup;
import com.zhangwuji.im.ui.widget.message.RenderType;
import com.zhangwuji.im.ui.widget.message.TextRenderView;
import com.zhangwuji.im.ui.widget.message.TimeRenderView;
import com.zhangwuji.im.ui.helper.listener.OnDoubleClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * @author : yingmu on 15-1-8.
 * @email : yingmu@mogujie.com.
 */
public class MessageAdapter extends BaseAdapter {
    private Logger logger = Logger.getLogger(MessageAdapter.class);

    private ArrayList<Object> msgObjectList = new ArrayList<>();

    /**
     * 弹出气泡
     */
    private MessageOperatePopup currentPop;
    private Context ctx;
    private int conversationType=1;
    /**
     * 依赖整体session状态的
     */
    private User loginUser;
    private IMService imService;
    ExtensionModule extensionModule;
    List<IMessageModule> messageModules=null;
    public MessageAdapter(Context ctx) {
        this.ctx = ctx;
    }
    IMessageData iMessageData=null;
    public HashMap FindMessageModule=new HashMap(); //缓存的自定义消息类

    /**
     * ----------------------init 的时候需要设定-----------------
     */
    public void setImService(IMService imService,User loginUser,int conversationType) {
        this.imService = imService;
        this.loginUser = loginUser;
        this.conversationType=conversationType;

        extensionModule=new ExtensionModule();
        messageModules=extensionModule.getMessageModule(ctx,conversationType);
        iMessageData=new IMessageData();
        iMessageData.setConversationType(conversationType);
        iMessageData.setCtx(ctx);
        iMessageData.setImService(imService);
        iMessageData.setLoginUser(loginUser);
    }


    /**
     * ----------------------添加历史消息-----------------
     */
    public void addItem(final Message msg) {
        if (msg.getDisplayType() == DBConstant.MSG_TYPE_SINGLE_TEXT) {
            if (isMsgGif(msg)) {
                msg.setGIfEmo(true);
            } else {
                msg.setGIfEmo(false);
            }
        }
        int nextTime = msg.getCreated();
        if (getCount() > 0) {
            Object object = msgObjectList.get(getCount() - 1);
            if (object instanceof Message) {
                int preTime = ((Message) object).getCreated();
                boolean needTime = DateUtil.needDisplayTime(preTime, nextTime);
                if (needTime) {
                    Integer in = nextTime;
                    msgObjectList.add(in);
                }
            }
        } else {
            Integer in = msg.getCreated();
            msgObjectList.add(in);
        }
        /**消息的判断*/
        if (msg.getDisplayType() == DBConstant.SHOW_MIX_TEXT) {
            MixMessage mixMessage = (MixMessage) msg;
            msgObjectList.addAll(mixMessage.getMsgList());
        } else {
            msgObjectList.add(msg);
        }
        if (msg instanceof ImageMessage) {
            ImageMessage.addToImageMessageList((ImageMessage) msg);
        }
        logger.d("#messageAdapter#addItem");
        notifyDataSetChanged();

    }

    private boolean isMsgGif(Message msg) {
        String content = msg.getContent();
        // @YM 临时处理  牙牙表情与消息混合出现的消息丢失
        if (TextUtils.isEmpty(content)
                || !(content.startsWith("[") && content.endsWith("]"))) {
            return false;
        }
        return Emoparser.getInstance(this.ctx).isMessageGif(msg.getContent());
    }

    public Message getTopMsgEntity() {
        if (msgObjectList.size() <= 0) {
            return null;
        }
        for (Object result : msgObjectList) {
            if (result instanceof Message) {
                return (Message) result;
            }
        }
        return null;
    }

    public static class MessageTimeComparator implements Comparator<Message> {
        @Override
        public int compare(Message lhs, Message rhs) {
            if (lhs.getCreated() == rhs.getCreated()) {
                return lhs.getMsgId() - rhs.getMsgId();
            }
            return lhs.getCreated() - rhs.getCreated();
        }
    }

    ;

    /**
     * 下拉载入历史消息,从最上面开始添加
     */
    public void loadHistoryList(final List<Message> historyList) {
        logger.d("#messageAdapter#loadHistoryList");
        if (null == historyList || historyList.size() <= 0) {
            return;
        }
        Collections.sort(historyList, new MessageTimeComparator());
        ArrayList<Object> chatList = new ArrayList<>();
        int preTime = 0;
        int nextTime = 0;
        for (Message msg : historyList) {
            if (msg.getDisplayType() == DBConstant.MSG_TYPE_SINGLE_TEXT) {
                if (isMsgGif(msg)) {
                    msg.setGIfEmo(true);
                } else {
                    msg.setGIfEmo(false);
                }
            }
            nextTime = msg.getCreated();
            boolean needTimeBubble = DateUtil.needDisplayTime(preTime, nextTime);
            if (needTimeBubble) {
                Integer in = nextTime;
                chatList.add(in);
            }
            preTime = nextTime;
            if (msg.getDisplayType() == DBConstant.SHOW_MIX_TEXT) {
                MixMessage mixMessage = (MixMessage) msg;
                chatList.addAll(mixMessage.getMsgList());
            } else {
                chatList.add(msg);
            }
        }
        // 如果是历史消息，从头开始加
        msgObjectList.addAll(0, chatList);
        getImageList();
        logger.d("#messageAdapter#addItem");
        notifyDataSetChanged();
    }

    /**
     * 获取图片消息列表
     */
    private void getImageList() {
        for (int i = msgObjectList.size() - 1; i >= 0; --i) {
            Object item = msgObjectList.get(i);
            if (item instanceof ImageMessage) {
                ImageMessage.addToImageMessageList((ImageMessage) item);
            }
        }
    }

    /**
     * 临时处理，一定要干掉
     */
    public void hidePopup() {
        if (currentPop != null) {
            currentPop.hidePopup();
        }
    }


    public void clearItem() {
        msgObjectList.clear();
    }

    /**
     * msgId 是消息ID
     * localId是本地的ID
     * position 是list 的位置
     * <p/>
     * 只更新item的状态
     * 刷新单条记录
     * <p/>
     */
    public void updateItemState(int position, final Message messageEntity) {
        //更新DB
        //更新单条记录
        imService.getDbInterface().insertOrUpdateMessage(messageEntity);
        notifyDataSetChanged();
    }

    /**
     * 对于混合消息的特殊处理
     */
    public void updateItemState(final Message messageEntity) {
        long dbId = messageEntity.getId();
        int msgId = messageEntity.getMsgId();
        int len = msgObjectList.size();
        for (int index = len - 1; index > 0; index--) {
            Object object = msgObjectList.get(index);
            if (object instanceof Message) {
                Message entity = (Message) object;
                if (object instanceof ImageMessage) {
                    ImageMessage.addToImageMessageList((ImageMessage) object);
                }
                if (entity.getId() == dbId && entity.getMsgId() == msgId) {
                    msgObjectList.set(index, messageEntity);
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (null == msgObjectList) {
            return 0;
        } else {
            return msgObjectList.size();
        }
    }

    @Override
    public int getViewTypeCount() {
        return RenderType.values().length;
    }


    @Override
    public int getItemViewType(int position) {
        try {
            /**默认是失败类型*/
            RenderType type = RenderType.MESSAGE_TYPE_INVALID;

            Object obj = msgObjectList.get(position);
            if (obj instanceof Integer) {
                type = RenderType.MESSAGE_TYPE_TIME_TITLE;
            } else if (obj instanceof Message) {
                Message info = (Message) obj;
                boolean isMine = info.getFromId() == loginUser.getPeerId();
                switch (info.getDisplayType()) {
                    case DBConstant.SHOW_AUDIO_TYPE:
                        type = isMine ? RenderType.MESSAGE_TYPE_MINE_AUDIO
                                : RenderType.MESSAGE_TYPE_OTHER_AUDIO;
                        break;
                    case DBConstant.SHOW_IMAGE_TYPE:
                        ImageMessage imageMessage = (ImageMessage) info;
                        if (CommonUtil.gifCheck(imageMessage.getUrl())) {
                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_GIF_IMAGE
                                    : RenderType.MESSAGE_TYPE_OTHER_GIF_IMAGE;
                        } else {
                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_IMAGE
                                    : RenderType.MESSAGE_TYPE_OTHER_IMAGE;
                        }

                        break;
                    case DBConstant.SHOW_ORIGIN_TEXT_TYPE:
                        if (info.isGIfEmo()) {
                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_GIF
                                    : RenderType.MESSAGE_TYPE_OTHER_GIF;
                        }
                        else {
                            type = isMine ? RenderType.MESSAGE_TYPE_MINE_TETX
                                    : RenderType.MESSAGE_TYPE_OTHER_TEXT;
                        }

                        break;
                    case DBConstant.SHOW_MIX_TEXT:
                        //
                        logger.e("混合的消息类型%s", obj);
                    default:
                        break;
                }
            }
            return type.ordinal();
        } catch (Exception e) {
            logger.e(e.getMessage());
            return RenderType.MESSAGE_TYPE_INVALID.ordinal();
        }
    }

    @Override
    public Object getItem(int position) {
        if (position >= getCount() || position < 0) {
            return null;
        }
        return msgObjectList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    /**
     * 时间气泡的渲染展示
     */
    private View timeBubbleRender(int position, View convertView, ViewGroup parent) {
        TimeRenderView timeRenderView;
        Integer timeBubble = (Integer) msgObjectList.get(position);
        if (null != convertView && convertView.getClass().equals(TimeRenderView.class)) {
            timeRenderView = (TimeRenderView) convertView;
        } else {
            timeRenderView = TimeRenderView.inflater(ctx, parent);
        }
        timeRenderView.setTime(timeBubble);
        return timeRenderView;
    }

    /**
     * 1.头像事件
     * mine:事件 other事件
     * 图片的状态  消息收到，没收到，图片展示成功，没有成功
     * 触发图片的事件  【长按】
     * <p/>
     * 图片消息类型的render
     *
     * @param position
     * @param convertView
     * @param parent
     * @param isMine
     * @return
     */
    private View imageMsgRender(final int position, View convertView, final ViewGroup parent, final boolean isMine) {
        ImageRenderView imageRenderView;
        final ImageMessage imageMessage = (ImageMessage) msgObjectList.get(position);
        User userEntity = imService.getContactManager().findContact(imageMessage.getFromId(),2);

        /**保存在本地的path*/
        final String imagePath = imageMessage.getPath();
        /**消息中的image路径*/
        final String imageUrl = imageMessage.getUrl();

       if (null != convertView && convertView.getClass().equals(ImageRenderView.class)) {
           imageRenderView = (ImageRenderView) convertView;
        } else {
           imageRenderView = ImageRenderView.inflater(ctx, parent, isMine);
        }

        final ImageView messageImage = imageRenderView.getMessageImage();
        final int msgId = imageMessage.getMsgId();
        imageRenderView.setBtnImageListener(new ImageRenderView.BtnImageListener() {
            @Override
            public void onMsgFailure() {
                /**
                 * 多端同步也不会拉到本地失败的数据
                 * 只有isMine才有的状态，消息发送失败
                 * 1. 图片上传失败。点击图片重新上传??[也是重新发送]
                 * 2. 图片上传成功，但是发送失败。 点击重新发送??
                 */
                if (FileUtil.isSdCardAvailuable()) {
//                    imageMessage.setLoadStatus(MessageStatus.IMAGE_UNLOAD);//如果是图片已经上传成功呢？
                    imageMessage.setStatus(MessageConstant.MSG_SENDING);
                    if (imService != null) {
                        imService.getMessageManager().resendMessage(imageMessage);
                    }
                    updateItemState(msgId, imageMessage);
                } else {
                    Toast.makeText(ctx, ctx.getString(R.string.sdcard_unavaluable), Toast.LENGTH_LONG).show();
                }
            }

            //DetailPortraitActivity 以前用的是DisplayImageActivity 这个类
            @Override
            public void onMsgSuccess() {
                Intent i = new Intent(ctx, PreviewMessageImagesActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(IntentConstant.CUR_MESSAGE, imageMessage);
                i.putExtras(bundle);
                ctx.startActivity(i);
                ((Activity) ctx).overridePendingTransition(R.anim.tt_image_enter, R.anim.tt_stay);
            }
        });

        // 设定触发loadImage的事件
        imageRenderView.setImageLoadListener(new ImageRenderView.ImageLoadListener() {

            @Override
            public void onLoadComplete(String loaclPath) {
                logger.d("chat#pic#save image ok");
                logger.d("pic#setsavepath:%s", loaclPath);
//                imageMessage.setPath(loaclPath);//下载的本地路径不再存储
                imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADED_SUCCESS);
                updateItemState(imageMessage);
            }

            @Override
            public void onLoadFailed() {
                logger.d("chat#pic#onBitmapFailed");
                imageMessage.setLoadStatus(MessageConstant.IMAGE_LOADED_FAILURE);
                updateItemState(imageMessage);
                logger.d("download failed");
            }
        });

        final View messageLayout = imageRenderView.getMessageLayout();
        messageImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 创建一个pop对象，然后 分支判断状态，然后显示需要的内容
                MessageOperatePopup popup = getPopMenu(parent, new OperateItemClickListener(imageMessage, position));
                boolean bResend = (imageMessage.getStatus() == MessageConstant.MSG_FAILURE)
                        || (imageMessage.getLoadStatus() == MessageConstant.IMAGE_UNLOAD);
                popup.show(messageLayout, DBConstant.SHOW_IMAGE_TYPE, bResend, isMine);
                return true;
            }
        });

        /**父类控件中的发送失败view*/
        imageRenderView.getMessageFailed().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // 重发或者重新加载
                MessageOperatePopup popup = getPopMenu(parent, new OperateItemClickListener(imageMessage, position));
                popup.show(messageLayout, DBConstant.SHOW_IMAGE_TYPE, true, isMine);
            }
        });
        imageRenderView.render(imageMessage, userEntity, ctx);

        return imageRenderView;
    }

    private View GifImageMsgRender(final int position, View convertView, final ViewGroup parent, final boolean isMine) {
        GifImageRenderView imageRenderView;
        final ImageMessage imageMessage = (ImageMessage) msgObjectList.get(position);
        User userEntity = imService.getContactManager().findContact(imageMessage.getFromId(),2);
       if(null != convertView && convertView.getClass().equals(GifImageRenderView.class)) {
               imageRenderView = (GifImageRenderView) convertView;
       } else {
           imageRenderView = GifImageRenderView.inflater(ctx, parent, isMine);
       }
        GifView imageView = imageRenderView.getMessageContent();
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                final String url = imageMessage.getUrl();
                Intent intent = new Intent(ctx, PreviewGifActivity.class);
                intent.putExtra(IntentConstant.PREVIEW_TEXT_CONTENT, url);
                ctx.startActivity(intent);
                ((Activity) ctx).overridePendingTransition(R.anim.tt_image_enter, R.anim.tt_stay);
            }
        });
        imageRenderView.render(imageMessage, userEntity, ctx);
        return imageRenderView;
    }

    /**
     * 语音的路径，判断收发的状态
     * 展现的状态
     * 播放动画相关
     * 获取语音的读取状态/
     * 语音长按事件
     *
     * @param position
     * @param convertView
     * @param parent
     * @param isMine
     * @return
     */
    private View audioMsgRender(final int position, View convertView, final ViewGroup parent, final boolean isMine) {
        AudioRenderView audioRenderView;
        final AudioMessage audioMessage = (AudioMessage) msgObjectList.get(position);
        User entity = imService.getContactManager().findContact(audioMessage.getFromId(),2);
        if (null != convertView && convertView.getClass().equals(AudioRenderView.class)) {
            audioRenderView = (AudioRenderView) convertView;
        } else {
            audioRenderView = AudioRenderView.inflater(ctx, parent, isMine);
        }
        final String audioPath = audioMessage.getAudioPath();

        final View messageLayout = audioRenderView.getMessageLayout();
        if (!TextUtils.isEmpty(audioPath)) {
            // 播放的路径为空,这个消息应该如何展示
            messageLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    MessageOperatePopup popup = getPopMenu(parent, new OperateItemClickListener(audioMessage, position));
                    boolean bResend = audioMessage.getStatus() == MessageConstant.MSG_FAILURE;
                    popup.show(messageLayout, DBConstant.SHOW_AUDIO_TYPE, bResend, isMine);
                    return true;
                }
            });
        }


        audioRenderView.getMessageFailed().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MessageOperatePopup popup = getPopMenu(parent, new OperateItemClickListener(audioMessage, position));
                popup.show(messageLayout, DBConstant.SHOW_AUDIO_TYPE, true, isMine);
            }
        });


        audioRenderView.setBtnImageListener(new AudioRenderView.BtnImageListener() {
            @Override
            public void onClickUnread() {
                logger.d("chat#audio#set audio meessage read status");
                audioMessage.setReadStatus(MessageConstant.AUDIO_READED);
                imService.getDbInterface().insertOrUpdateMessage(audioMessage);
            }

            @Override
            public void onClickReaded() {
            }
        });
        audioRenderView.render(audioMessage, entity, ctx);
        return audioRenderView;
    }


    /**
     * text类型的: 1. 设定内容Emoparser
     * 2. 点击事件  单击跳转、 双击方法、长按pop menu
     * 点击头像的事件 跳转
     *
     * @param position
     * @param convertView
     * @param viewGroup
     * @param isMine
     * @return
     */
    private View textMsgRender(final int position, View convertView, final ViewGroup viewGroup, final boolean isMine) {
        TextRenderView textRenderView;
        final TextMessage textMessage = (TextMessage) msgObjectList.get(position);
        User userEntity = imService.getContactManager().findContact(textMessage.getFromId(),2);

        if (null != convertView && convertView.getClass().equals(TextRenderView.class)) {
            textRenderView = (TextRenderView) convertView;
        } else {
            textRenderView = TextRenderView.inflater(ctx, viewGroup, isMine);
        }

        final TextView textView = textRenderView.getMessageContent();

        // 失败事件添加
        textRenderView.getMessageFailed().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(textMessage, position));
                popup.show(textView, DBConstant.SHOW_ORIGIN_TEXT_TYPE, true, isMine);
            }
        });

        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 弹窗类型
                MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(textMessage, position));
                boolean bResend = textMessage.getStatus() == MessageConstant.MSG_FAILURE;
                popup.show(textView, DBConstant.SHOW_ORIGIN_TEXT_TYPE, bResend, isMine);
                return true;
            }
        });

        // url 路径可以设定 跳转哦哦
        final String content = textMessage.getContent();
        textView.setOnTouchListener(new OnDoubleClickListener() {
            @Override
            public void onClick(View view) {
                //todo
            }

            @Override
            public void onDoubleClick(View view) {
                Intent intent = new Intent(ctx, PreviewTextActivity.class);
                intent.putExtra(IntentConstant.PREVIEW_TEXT_CONTENT, content);
                ctx.startActivity(intent);
            }
        });
        textRenderView.render(textMessage, userEntity, ctx);
        return textRenderView;
    }

    /**
     * 牙牙表情等gif类型的消息: 1. 设定内容Emoparser
     * 2. 点击事件  单击跳转、 双击方法、长按pop menu
     * 点击头像的事件 跳转
     *
     * @param position
     * @param convertView
     * @param viewGroup
     * @param isMine
     * @return
     */
    private View gifMsgRender(final int position, View convertView, final ViewGroup viewGroup, final boolean isMine) {
        EmojiRenderView gifRenderView;
        final TextMessage textMessage = (TextMessage) msgObjectList.get(position);
        User userEntity = imService.getContactManager().findContact(textMessage.getFromId(),2);

        if (null != convertView && convertView.getClass().equals(EmojiRenderView.class)) {
            gifRenderView = (EmojiRenderView) convertView;
        } else {
            gifRenderView = EmojiRenderView.inflater(ctx, viewGroup, isMine);
        }

        final ImageView imageView = gifRenderView.getMessageContent();
        // 失败事件添加
        gifRenderView.getMessageFailed().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(textMessage, position));
                popup.show(imageView, DBConstant.SHOW_GIF_TYPE, true, isMine);
            }
        });

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MessageOperatePopup popup = getPopMenu(viewGroup, new OperateItemClickListener(textMessage, position));
                boolean bResend = textMessage.getStatus() == MessageConstant.MSG_FAILURE;
                popup.show(imageView, DBConstant.SHOW_GIF_TYPE, bResend, isMine);

                return true;
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                final String content = textMessage.getContent();
                Intent intent = new Intent(ctx, PreviewGifActivity.class);
                intent.putExtra(IntentConstant.PREVIEW_TEXT_CONTENT, content);
                ctx.startActivity(intent);
                ((Activity) ctx).overridePendingTransition(R.anim.tt_image_enter, R.anim.tt_stay);
            }
        });

        gifRenderView.render(textMessage, userEntity, ctx);
        return gifRenderView;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        try {

            Object obj = msgObjectList.get(position);

            if (obj instanceof Integer) {
                convertView = timeBubbleRender(position, convertView, parent);
            } else if (obj instanceof Message) {

                Message info = (Message) obj;
                boolean isMine = info.getFromId() == loginUser.getPeerId();


                switch (info.getDisplayType()) {
                    case DBConstant.SHOW_AUDIO_TYPE:
                        convertView = audioMsgRender(position, convertView, parent, isMine);
                        break;
                    case DBConstant.SHOW_IMAGE_TYPE:
                        ImageMessage imageMessage = (ImageMessage) info;
                        if (CommonUtil.gifCheck(imageMessage.getUrl())) {
                            convertView = GifImageMsgRender(position, convertView, parent, isMine);
                        } else {
                            convertView = imageMsgRender(position, convertView, parent, isMine);
                        }
                        break;
                    case DBConstant.SHOW_ORIGIN_TEXT_TYPE: {
                        if (info.isGIfEmo()) {
                            convertView = gifMsgRender(position, convertView, parent, isMine);
                        }  else {

                            String tag=null;
                            try {
                                JSONObject jsonObject = JSON.parseObject(info.getContent());
                                tag=jsonObject.getString("tag");
                            }catch (Exception e){}

                            if(tag!=null)
                            {
                                boolean isfind=false;

                                //从缓存中读取模块
                                if(FindMessageModule.containsKey(tag))
                                {
                                    convertView = ((IMessageModule)FindMessageModule.get(tag)).messageRender(iMessageData, info, position, convertView, parent, isMine);
                                    isfind=true;
                                }
                                else {
                                    for (IMessageModule iMessageModule : messageModules) {
                                        MessageTag messageTag = (MessageTag)iMessageModule.getClass().getAnnotation(MessageTag.class);
                                        if (messageTag != null && tag.equals(messageTag.value())) {
                                          //  try {
                                                convertView = iMessageModule.messageRender(iMessageData, info, position, convertView, parent, isMine);
                                                FindMessageModule.put(tag,iMessageModule);
                                                isfind = true;
                                           // } catch (Exception e) { }
                                            break;
                                        }
                                    }
                                }

                                if(!isfind)
                                {
                                    convertView = textMsgRender(position, convertView, parent, isMine);
                                }
                            }
                            else
                            {
                                convertView = textMsgRender(position, convertView, parent, isMine);
                            }
                        }
                    }
                    break;
                    case DBConstant.SHOW_MIX_TEXT:
                        logger.e("混合的消息类型%s", obj);
                    default:
                        logger.e("[fatal erro] render type:MESSAGE_TYPE_INVALID");
                        break;
                }


            }



/*
        try {
        final int typeIndex = getItemViewType(position);
        RenderType renderType = RenderType.values()[typeIndex];
            // 改用map的形式
            switch (renderType) {
                case MESSAGE_TYPE_INVALID:
                    // 直接返回
                    logger.e("[fatal erro] render type:MESSAGE_TYPE_INVALID");
                    break;

                case MESSAGE_TYPE_TIME_TITLE:
                    convertView = timeBubbleRender(position, convertView, parent);
                    break;

                case MESSAGE_TYPE_MINE_AUDIO:
                    convertView = audioMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_AUDIO:
                    convertView = audioMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_GIF_IMAGE:
                    convertView = GifImageMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_GIF_IMAGE:
                    convertView = GifImageMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_IMAGE:
                    convertView = imageMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_IMAGE:
                    convertView = imageMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_TETX:
                   // convertView = textMsgRender(position, convertView, parent, true);
                {
                    IMessageData iMessageData = new IMessageData();
                    iMessageData.setConversationType(1);
                    iMessageData.setCtx(ctx);
                    iMessageData.setImService(imService);
                    iMessageData.setLoginUser(loginUser);

                    String tag = null;
                    try {
                        JSONObject jsonObject = JSON.parseObject(((Message)msgObjectList.get(position)).getContent());
                        tag = jsonObject.getString("tag");
                    } catch (Exception e) {
                    }


                    if (tag != null) {
                        boolean isfind = false;
                        for (IMessageModule iMessageModule : messageModules) {
                            MessageTag messageTag = (MessageTag) iMessageModule.getClass().getAnnotation(MessageTag.class);
                            if (messageTag != null && tag.equals(messageTag.value())) {

                                try {
                                    convertView = iMessageModule.messageRender(iMessageData, msgObjectList.get(position), position, convertView, parent, true);
                                    isfind = true;
                                } catch (Exception e) {
                                }
                                break;
                            }
                        }
                        if (!isfind) {
                            convertView = textMsgRender(position, convertView, parent, true);
                        }
                    }
                    else
                    {
                        convertView = textMsgRender(position, convertView, parent, true);
                    }
                }
                    break;
                case MESSAGE_TYPE_OTHER_TEXT:
                    convertView = textMsgRender(position, convertView, parent, false);
                    break;
                case MESSAGE_TYPE_MINE_GIF:
                    convertView = gifMsgRender(position, convertView, parent, true);
                    break;
                case MESSAGE_TYPE_OTHER_GIF:
                    convertView = gifMsgRender(position, convertView, parent, false);
                    break;
            }*/

            return convertView;

        } catch (Exception e) {
            logger.e("chat#%s", e);
            return null;
        }
    }

    /**
     * 点击事件的定义
     */
    private MessageOperatePopup getPopMenu(ViewGroup parent, MessageOperatePopup.OnItemClickListener listener) {
        MessageOperatePopup popupView = MessageOperatePopup.instance(ctx, parent);
        currentPop = popupView;
        popupView.setOnItemClickListener(listener);
        return popupView;
    }

    private class OperateItemClickListener
            implements
            MessageOperatePopup.OnItemClickListener {

        private Message mMsgInfo;
        private int mType;
        private int mPosition;

        public OperateItemClickListener(Message msgInfo, int position) {
            mMsgInfo = msgInfo;
            mType = msgInfo.getDisplayType();
            mPosition = position;
        }

        @SuppressWarnings("deprecation")
        @SuppressLint("NewApi")
        @Override
        public void onCopyClick() {
            try {
                ClipboardManager manager = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);

                logger.d("menu#onCopyClick content:%s", mMsgInfo.getContent());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    ClipData data = ClipData.newPlainText("data", mMsgInfo.getContent());
                    manager.setPrimaryClip(data);
                } else {
                    manager.setText(mMsgInfo.getContent());
                }
            } catch (Exception e) {
                logger.e(e.getMessage());
            }
        }

        @Override
        public void onResendClick() {
            try {
                if (mType == DBConstant.SHOW_AUDIO_TYPE
                        || mType == DBConstant.SHOW_ORIGIN_TEXT_TYPE) {

                    if (mMsgInfo.getDisplayType() == DBConstant.SHOW_AUDIO_TYPE) {
                        if (mMsgInfo.getSendContent().length < 4) {
                            return;
                        }
                    }
                } else if (mType == DBConstant.SHOW_IMAGE_TYPE) {
                    logger.d("pic#resend");
                    // 之前的状态是什么 上传没有成功继续上传
                    // 上传成功，发送消息
                    ImageMessage imageMessage = (ImageMessage) mMsgInfo;
                    if (TextUtils.isEmpty(imageMessage.getPath())) {
                        Toast.makeText(ctx, ctx.getString(R.string.image_path_unavaluable), Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                mMsgInfo.setStatus(MessageConstant.MSG_SENDING);
                msgObjectList.remove(mPosition);
                addItem(mMsgInfo);
                if (imService != null) {
                    imService.getMessageManager().resendMessage(mMsgInfo);
                }

            } catch (Exception e) {
                logger.e("chat#exception:" + e.toString());
            }
        }

        @Override
        public void onSpeakerClick() {
            AudioPlayerHandler audioPlayerHandler = AudioPlayerHandler.getInstance();
            if (audioPlayerHandler.getAudioMode(ctx) == AudioManager.MODE_NORMAL) {
                audioPlayerHandler.setAudioMode(AudioManager.MODE_IN_CALL, ctx);
                SpeekerToast.show(ctx, ctx.getText(R.string.audio_in_call), Toast.LENGTH_SHORT);
            } else {
                audioPlayerHandler.setAudioMode(AudioManager.MODE_NORMAL, ctx);
                SpeekerToast.show(ctx, ctx.getText(R.string.audio_in_speeker), Toast.LENGTH_SHORT);
            }
        }
    }
}
