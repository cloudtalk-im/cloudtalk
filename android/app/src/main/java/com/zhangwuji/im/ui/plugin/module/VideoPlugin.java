package com.zhangwuji.im.ui.plugin.module;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.zhangwuji.im.R;
import com.zhangwuji.im.imcore.event.MessageEvent;
import com.zhangwuji.im.ui.activity.VideoChatViewActivity2;
import com.zhangwuji.im.ui.adapter.album.AlbumHelper;
import com.zhangwuji.im.ui.adapter.album.ImageBucket;
import com.zhangwuji.im.ui.entity.IMCallAction;
import com.zhangwuji.im.ui.plugin.ExtensionModule;
import com.zhangwuji.im.ui.plugin.IPluginData;
import com.zhangwuji.im.ui.plugin.IPluginModule;

import org.greenrobot.eventbus.EventBus;

import java.util.List;


/**
 * 拍照发送的插件
 */
public class VideoPlugin implements IPluginModule {

	public IPluginData pluginData=null;
	public Activity curActivity=null;
    static public final int REQUEST_CODE_ASK_PERMISSIONS = 101;

	public Drawable obtainDrawable(Context context) {
		return context.getResources().getDrawable(R.drawable.rc_voip_icon_input_video);
	}
	
	public String obtainTitle(Context context) {
		return context.getString(R.string.video_btn_text);
	}

	public void onClick(Activity currentActivity, IPluginData pluginData,int position) {
		this.pluginData=pluginData;
		this.curActivity=currentActivity;


        if (Build.VERSION.SDK_INT >= 23) {
            int checkPermission = currentActivity.checkSelfPermission(Manifest.permission.CAMERA);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                if (currentActivity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    currentActivity.requestPermissions(new String[] {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
                } else {
                    new AlertDialog.Builder(currentActivity)
                            .setMessage("您需要在设置里打录摄像头权限。")
                            .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    curActivity.requestPermissions(new String[] {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            })
                            .create().show();
                }
                return;
            }
        }


		String roomid="roomid_"+pluginData.getLoginUser().getPeerId()+"_"+pluginData.getPeerEntity().getPeerId();
		pluginData.getImService().getMessageManager().sendVideoMessage(pluginData.getPeerEntity().getPeerId(),pluginData.getLoginUser().getPeerId(),66666,roomid);

		Intent intent = new Intent(curActivity,VideoChatViewActivity2.class);
		intent.putExtra("roomid", roomid);
		intent.putExtra("tageruserId", pluginData.getPeerEntity().getPeerId());
		intent.putExtra("callAction", IMCallAction.ACTION_OUTGOING_CALL.getName());

		//注意。如果需要回调，必需用下面的方法，且requestCode要小于255 否则不能回调到本plugin内部
		ExtensionModule.startActivityForPluginResult(currentActivity,intent,77,this,position);


		MessageEvent messageEvent=new MessageEvent();
		messageEvent.setEvent(MessageEvent.Event.PLUGINCOMPLETE);
		EventBus.getDefault().post(messageEvent);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK)
			return;
		if(requestCode==77) {
		}
	}
}