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
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.view.View;

import com.zhangwuji.im.BuildConfig;
import com.zhangwuji.im.R;
import com.zhangwuji.im.config.SysConstant;
import com.zhangwuji.im.imcore.entity.ImageMessage;
import com.zhangwuji.im.imcore.event.MessageEvent;
import com.zhangwuji.im.ui.activity.AMAPLocationActivity;
import com.zhangwuji.im.ui.plugin.ExtensionModule;
import com.zhangwuji.im.ui.plugin.IPluginData;
import com.zhangwuji.im.ui.plugin.IPluginModule;
import com.zhangwuji.im.utils.CommonUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * 拍照发送的插件
 */
public class TakeCameraPlugin implements IPluginModule {

	public IPluginData pluginData=null;
	public Activity curActivity=null;
	public String takePhotoSavePath="";
	static public final int REQUEST_CODE_ASK_PERMISSIONS = 101;

	public Drawable obtainDrawable(Context context) {
		return context.getResources().getDrawable(R.drawable.tt_take_camera_btn_bg);
	}
	
	public String obtainTitle(Context context) {
		return context.getString(R.string.take_camera_btn_text);
	}

	public void onClick(Activity currentActivity, IPluginData pluginData,int position) {
		this.pluginData=pluginData;
		this.curActivity=currentActivity;

		if (Build.VERSION.SDK_INT >= 23) {
			int checkPermission = currentActivity.checkSelfPermission(Manifest.permission.CAMERA);
			if (checkPermission != PackageManager.PERMISSION_GRANTED) {
				if (currentActivity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
					currentActivity.requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CODE_ASK_PERMISSIONS);
				} else {
					new AlertDialog.Builder(currentActivity)
							.setMessage("您需要在设置里打开相机权限。")
							.setPositiveButton("确认", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									curActivity.requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CODE_ASK_PERMISSIONS);
								}
							})
							.setNegativeButton("取消", null)
							.create().show();
				}
				return;
			}
		}

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		takePhotoSavePath = CommonUtil.getImageSavePath(String.valueOf(System
				.currentTimeMillis())
				+ ".jpg");
		intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(currentActivity, BuildConfig.APPLICATION_ID + ".provider", new File(takePhotoSavePath)));


		//注意。如果需要回调，必需用下面的方法，且requestCode要小于255 否则不能回调到本plugin内部
		ExtensionModule.startActivityForPluginResult(curActivity,intent,SysConstant.CAMERA_WITH_DATA,this,position);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK)
			return;
		if(requestCode==SysConstant.CAMERA_WITH_DATA) {
			handleTakePhotoData(data);
		}
	}



	/**
	 * @param data
	 * @Description 处理拍照后的数据
	 * 应该是从某个 activity回来的
	 */
	private void handleTakePhotoData(Intent data) {
		ImageMessage imageMessage = ImageMessage.buildForSend(takePhotoSavePath, pluginData.getLoginUser(), pluginData.getPeerEntity());
		List<ImageMessage> sendList = new ArrayList<>(1);
		sendList.add(imageMessage);
		pluginData.getImService().getMessageManager().sendImages(sendList);

		MessageEvent messageEvent=new MessageEvent();
		messageEvent.setEvent(MessageEvent.Event.SENDPUSHLIST);
		messageEvent.setMessageEntity(imageMessage);
		EventBus.getDefault().post(messageEvent);
	}
}