package com.zhangwuji.im.server.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.zhangwuji.im.DB.sp.SystemConfigSp;
import com.zhangwuji.im.server.network.http.HttpException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IMAction extends BaseAction {
    private SharedPreferences sp;

    /**
     * 构造方法
     *
     * @param context 上下文
     */
    public IMAction(Context context) {
        super(context);
        sp = context.getSharedPreferences("config", context.MODE_PRIVATE);
    }

    /**
     * 获取 服务器IP 地址
     * @param callback 回调方法
     */
    public void getMsgServerAddrs(final ResultCallback<String> callback)
    {
        String url = getURL("/api/getSrvInfo");
        IMHttpGetCallBack(url,callback);
    }

    public void getFriendList(final ResultCallback<String> callback)
    {
        String url = getURL("/api/getFriends");
        HashMap parms=new HashMap();
        IMHttpPostCallBack(url, parms,callback);
    }
    public void getGroupList(final ResultCallback<String> callback)
    {
        String url = getURL("/api/getGroupList");
        HashMap parms=new HashMap();
        IMHttpPostCallBack(url, parms,callback);
    }
    public void getGroupInfo(String groupIds,final ResultCallback<String> callback)
    {
        String url = getURL("/api/getGroupInfo");
        HashMap parms=new HashMap();
        parms.put("groupIds",groupIds);
        IMHttpPostCallBack(url, parms,callback);
    }

    public void getNewFriendList(final ResultCallback<String> callback)
    {
        String url = getURL("/api/getNewFriends");
        HashMap parms=new HashMap();
        IMHttpPostCallBack(url, parms,callback);
    }

    public void getUserInfo(String uids,final ResultCallback<String> callback)
    {
        String url = getURL("/api/getUserInfo");
        HashMap parms=new HashMap();
        parms.put("friuids",uids);
        IMHttpPostCallBack(url, parms,callback);
    }

    public void postFile(File file,int type,String fileName,final ResultCallback<String> callback)
    {
        MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
        String url = getURL("/api/uploadImg");
        if (file != null && file.exists()) {
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("type",type+"")
                    .addFormDataPart("files", fileName,
                            RequestBody.create(MEDIA_TYPE_PNG, file));
            IMHttpPostFileCallBack(url, builder.build(),callback);
        }
    }


}
