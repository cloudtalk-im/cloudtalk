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
import android.widget.Toast;

import com.zhangwuji.im.BuildConfig;
import com.zhangwuji.im.R;
import com.zhangwuji.im.config.IntentConstant;
import com.zhangwuji.im.config.SysConstant;
import com.zhangwuji.im.imcore.entity.ImageMessage;
import com.zhangwuji.im.imcore.event.MessageEvent;
import com.zhangwuji.im.ui.activity.MessageActivity;
import com.zhangwuji.im.ui.activity.PickPhotoActivity;
import com.zhangwuji.im.ui.adapter.album.AlbumHelper;
import com.zhangwuji.im.ui.adapter.album.ImageBucket;
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
public class TakePhotoPlugin implements IPluginModule {

	private List<ImageBucket> albumList = null;
	private AlbumHelper albumHelper = null;
	public IPluginData pluginData=null;
	public Activity curActivity=null;
    static public final int REQUEST_CODE_ASK_PERMISSIONS = 101;

	public Drawable obtainDrawable(Context context) {
		return context.getResources().getDrawable(R.drawable.rc_ext_plugin_image);
	}
	
	public String obtainTitle(Context context) {
		return context.getString(R.string.take_photo_btn_text);
	}

	public void onClick(Activity currentActivity, IPluginData pluginData,int position) {
		this.pluginData=pluginData;
		this.curActivity=currentActivity;


        if (Build.VERSION.SDK_INT >= 23) {
            int checkPermission = currentActivity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                if (currentActivity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    currentActivity.requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
                } else {
                    new AlertDialog.Builder(currentActivity)
                            .setMessage("您需要在设置里打开储存权限。")
                            .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    curActivity.requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
                                }
                            })
                            .create().show();
                }
                return;
            }
        }

		initAlbumHelper();
		if (albumList.size() < 1) {
			Toast.makeText(currentActivity, currentActivity.getResources().getString(R.string.not_found_album), Toast.LENGTH_LONG)
					.show();
			return;
		}
		// 选择图片的时候要将session的整个回话 传过来
		Intent intent = new Intent(currentActivity, PickPhotoActivity.class);
		intent.putExtra(IntentConstant.KEY_SESSION_KEY, pluginData.getCurrentSessionKey());

		//注意。如果需要回调，必需用下面的方法，且requestCode要小于255 否则不能回调到本plugin内部
		ExtensionModule.startActivityForPluginResult(currentActivity,intent,SysConstant.ALBUM_BACK_DATA,this,position);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK)
			return;
		if(requestCode==SysConstant.CAMERA_WITH_DATA) {
            curActivity.setIntent(data);

			MessageEvent messageEvent=new MessageEvent();
			messageEvent.setEvent(MessageEvent.Event.PLUGINCOMPLETE);
			EventBus.getDefault().post(messageEvent);
		}
	}
	/**
	 * @Description 初始化数据（相册,表情,数据库相关）
	 */
	private void initAlbumHelper() {
		albumHelper = AlbumHelper.getHelper(curActivity);
		albumList = albumHelper.getImagesBucketList(false);
	}
}