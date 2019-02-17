package com.zhangwuji.im.ui.plugin.message;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.melink.baseframe.utils.DensityUtils;
import com.melink.bqmmsdk.widget.BQMMMessageText;
import com.zhangwuji.im.DB.entity.Message;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.R;
import com.zhangwuji.im.imcore.entity.MessageTag;
import com.zhangwuji.im.imcore.entity.TextMessage;
import com.zhangwuji.im.server.utils.json.JsonMananger;
import com.zhangwuji.im.ui.activity.AMAPLocationActivity;
import com.zhangwuji.im.ui.plugin.IMessageData;
import com.zhangwuji.im.ui.plugin.IMessageModule;
import com.zhangwuji.im.ui.plugin.message.entity.BigMojiMessage;
import com.zhangwuji.im.ui.plugin.message.entity.LocationMessage;
import com.zhangwuji.im.ui.widget.BubbleImageView;
import com.zhangwuji.im.ui.widget.CTMessageFrameLayout;
import com.zhangwuji.im.ui.widget.IMBaseImageView;
import com.zhangwuji.im.ui.widget.message.BaseMsgRenderView;

import org.json.JSONArray;


@MessageTag(value = "cloudtalk:location",messageContent=LocationMessage.class)
public class LocationRenderView extends BaseMsgRenderView implements IMessageModule {

    public IMessageData iMessageData;
    private TextView title;
    private BubbleImageView rc_img;
    private CTMessageFrameLayout mLayout;
    public static LocationRenderView inflater(Context context, ViewGroup viewGroup, boolean isMine){

        int resource = isMine? R.layout.tt_mine_location_message_item :R.layout.tt_other_location_message_item;
        LocationRenderView gifRenderView = (LocationRenderView) LayoutInflater.from(context).inflate(resource, viewGroup, false);
        gifRenderView.setMine(isMine);
        return gifRenderView;
    }

    public LocationRenderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();

    }

    /**
     * 控件赋值
     * @param messageEntity
     * @param userEntity
     */
    @Override
    public void render(Message messageEntity, User userEntity, Context context) {
        super.render(messageEntity, userEntity,context);

        rc_img= (BubbleImageView)findViewById(R.id.rc_img);
        title = (TextView)findViewById(R.id.rc_content);
        mLayout = (CTMessageFrameLayout)findViewById(R.id.message_layout);

       String content=messageEntity.getContent();
        final LocationMessage locationMessage= JsonMananger.jsonToBean(content,LocationMessage.class);

        if (locationMessage.getmImgUri() != null) {
            rc_img.setDefaultImageRes(R.drawable.rc_ic_location_item_default);
            rc_img.setImageUrl(locationMessage.getmImgUri());
        }
        if (isMine)
        {
            mLayout.setBackgroundResource(R.drawable.rc_ic_bubble_no_right);
        } else {
            mLayout.setBackgroundResource(R.drawable.rc_ic_bubble_no_left);
        }

        title.setText(locationMessage.getmPoi());

        mLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), AMAPLocationActivity.class);
                intent.putExtra("location", locationMessage);
                view.getContext().startActivity(intent);
            }
        });

    }


    @Override
    public void msgFailure(Message messageEntity) {
        super.msgFailure(messageEntity);
    }


    /**----------------set/get---------------------------------*/
    public void setMine(boolean isMine) {
        this.isMine = isMine;
    }


    public void setParentView(ViewGroup parentView) {
        this.parentView = parentView;
    }

    @Override
    public  View messageRender(IMessageData iMessageData, Object message, int position, View convertView, ViewGroup parent, boolean isMine) {
        this.iMessageData=iMessageData;
        this.isMine=isMine;

        LocationRenderView locationRenderView;
        final TextMessage imageMessage = (TextMessage)message;

        User userEntity = iMessageData.getImService().getContactManager().findContact(imageMessage.getFromId(),2);
        if (null != convertView && convertView.getClass().equals(LocationRenderView.class)) {
            locationRenderView = (LocationRenderView) convertView;
        } else {
            locationRenderView = LocationRenderView.inflater(iMessageData.getCtx(), parent, isMine);
        }

        locationRenderView.render(imageMessage, userEntity, iMessageData.getCtx());
        return locationRenderView;
    }
}
