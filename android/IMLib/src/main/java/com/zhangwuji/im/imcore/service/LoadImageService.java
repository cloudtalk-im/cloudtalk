package com.zhangwuji.im.imcore.service;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.zhangwuji.im.DB.sp.SystemConfigSp;
import com.zhangwuji.im.config.SysConstant;
import com.zhangwuji.im.imcore.entity.ImageMessage;
import com.zhangwuji.im.imcore.event.MessageEvent;
import com.zhangwuji.im.server.network.BaseAction;
import com.zhangwuji.im.server.network.IMAction;
import com.zhangwuji.im.utils.FileUtil;
import com.zhangwuji.im.utils.Logger;
import com.zhangwuji.im.utils.PhotoHelper;

import java.io.File;
import java.io.IOException;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

public class LoadImageService extends IntentService {
    private String result = null;
    private static Logger logger = Logger.getLogger(LoadImageService.class);

    public LoadImageService(){
        super("LoadImageService");
    }

    public LoadImageService(String name) {
        super(name);
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(android.content.Intent)}.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        final ImageMessage messageInfo = (ImageMessage)intent.getSerializableExtra(SysConstant.UPLOAD_IMAGE_INTENT_PARAMS);
            Bitmap bitmap;
            try {
                File file= new File(messageInfo.getPath());
                int type=0;

                IMAction imAction=new IMAction(getBaseContext());
                if(file.exists() && FileUtil.getExtensionName(messageInfo.getPath()).toLowerCase().equals(".gif"))
                {
                    SystemConfigSp.instance().init(getApplicationContext());
                    result = imAction.uploadBigImage(SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.MSFSSERVER), FileUtil.File2byte(messageInfo.getPath()), messageInfo.getPath());
                }
                else
                {
                    bitmap = PhotoHelper.revitionImage(messageInfo.getPath());
                    if (null != bitmap) {
                        byte[] bytes = PhotoHelper.getBytes(bitmap);
                        result = imAction.uploadBigImage(SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.MSFSSERVER), bytes, messageInfo.getPath());
                    }
                }

                if (TextUtils.isEmpty(result)) {
                    logger.i("upload image faild,cause by result is empty/null");
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.Event.IMAGE_UPLOAD_FAILD
                            ,messageInfo));
                } else {
                    logger.i("upload image succcess,imageUrl is %s",result);
                    String imageUrl = result;
                    messageInfo.setUrl(imageUrl);
                    EventBus.getDefault().post(new MessageEvent(
                            MessageEvent.Event.IMAGE_UPLOAD_SUCCESS
                            ,messageInfo));
                }

/*
                if(FileUtil.getExtensionName(messageInfo.getPath()).toLowerCase().equals(".gif"))
                {
                    type=1;
                }
                if(file.exists())
                {
                        IMAction imAction=new IMAction(getBaseContext());
                        imAction.postFile(file,type,file.getName(), new BaseAction.ResultCallback<String>() {
                            @Override
                            public void onSuccess(String s) {
                                try {
                                    JSONObject root = null;
                                    root = new JSONObject(s);
                                    if(root.getInt("code")==200) {
                                        JSONObject data = root.getJSONObject("data");
                                        result=data.getString("file_url");
                                        logger.i("upload image succcess,imageUrl is %s",result);
                                        String imageUrl = result;
                                        messageInfo.setUrl(imageUrl);
                                        EventBus.getDefault().post(new MessageEvent(
                                                MessageEvent.Event.IMAGE_UPLOAD_SUCCESS,messageInfo));
                                    }
                                    else
                                    {
                                        logger.i("upload image faild,cause by result is empty/null");
                                        EventBus.getDefault().post(new MessageEvent(MessageEvent.Event.IMAGE_UPLOAD_FAILD,messageInfo));
                                    }
                                } catch (JSONException e) { }
                            }
                            @Override
                            public void onError(String errString) {
                                logger.i("upload image faild,cause by result is empty/null");
                                EventBus.getDefault().post(new MessageEvent(MessageEvent.Event.IMAGE_UPLOAD_FAILD,messageInfo));
                            }
                        });
                    }

*/
            } catch (Exception e) {
                logger.e(e.getMessage());
            }
    }
}
