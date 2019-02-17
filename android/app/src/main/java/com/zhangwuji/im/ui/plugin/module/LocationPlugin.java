package com.zhangwuji.im.ui.plugin.module;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.Fragment;

import com.zhangwuji.im.R;
import com.zhangwuji.im.config.SysConstant;
import com.zhangwuji.im.imcore.entity.TextMessage;
import com.zhangwuji.im.imcore.event.MessageEvent;
import com.zhangwuji.im.ui.activity.AMAPLocationActivity;
import com.zhangwuji.im.ui.plugin.ExtensionModule;
import com.zhangwuji.im.ui.plugin.IPluginData;
import com.zhangwuji.im.ui.plugin.IPluginModule;
import com.zhangwuji.im.ui.plugin.message.entity.LocationMessage;

import org.greenrobot.eventbus.EventBus;

import java.net.URL;

/**
 * 发送位置消息的功能插件
 */
public class LocationPlugin implements IPluginModule {

	IPluginData pluginData=null;
	public Drawable obtainDrawable(Context context) {
		return context.getResources().getDrawable(R.drawable.rc_ic_location_normal);
	}
	
	public String obtainTitle(Context context) {
		return context.getString(R.string.mylocation);
	}

	public void onClick(Activity currentActivity, IPluginData pluginData,int position) {
		this.pluginData=pluginData;

		Intent intent = new Intent();
        intent.setClass(currentActivity.getBaseContext(),AMAPLocationActivity.class);

		//注意。如果需要回调，必需用下面的方法，且requestCode要小于255 否则不能回调到本plugin内部
		ExtensionModule.startActivityForPluginResult(currentActivity,intent,112,this,position);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK)
			return;

		if(requestCode==112) {
			if (data != null) {
				double latitude = data.getDoubleExtra("latitude",0.00);
				double longitude = data.getDoubleExtra("longitude",0.00);
				String address = data.getStringExtra("address");
                String uri = data.getStringExtra("locuri");


				//自定义消息就这样发出去了，是不是很简单。
				LocationMessage locationMessage=new LocationMessage(latitude,longitude,address,uri);
				locationMessage.setDisplaytext("位置信息");//简称不能丢。如果不设置。在将返整个代码

				TextMessage textMessage = TextMessage.buildIMessageFosend(locationMessage, this.pluginData.getLoginUser(), this.pluginData.getPeerEntity());
				this.pluginData.getImService().getMessageManager().sendText(textMessage);

				//通知聊天窗口刷新消息
				MessageEvent messageEvent=new MessageEvent();
				messageEvent.setEvent(MessageEvent.Event.SENDPUSHLIST);
				messageEvent.setMessageEntity(textMessage);
				EventBus.getDefault().post(messageEvent);
			}
		}

	}
}