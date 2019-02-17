package com.zhangwuji.im.ui.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.zhangwuji.im.DB.sp.SystemConfigSp;
import com.zhangwuji.im.server.network.BaseAction;
import com.zhangwuji.im.server.network.http.HttpException;

import java.util.HashMap;

public class ApiAction extends BaseAction {

    private final String CONTENT_TYPE = "application/json";
    private final String ENCODING = "utf-8";
    private SharedPreferences sp;

    /**
     * 构造方法
     *
     * @param context 上下文
     */
    public ApiAction(Context context) {
        super(context);
        sp = context.getSharedPreferences("config", context.MODE_PRIVATE);
    }

    public void UserLogin(String username,String password,final ResultCallback<String> callback)
    {
        String url = getURL("/api/checkLogin");
        HashMap parms=new HashMap();
        parms.put("appid",SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.APPID));
        parms.put("username",username);
        parms.put("password",password);
        IMHttpPostCallBack(url, parms,callback);
    }
    public void UserReg(String nickname,String username,String password,String code,final ResultCallback<String> callback)
    {
        String url = getURL("/api/reg");
        HashMap parms=new HashMap();
        parms.put("appid",SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.APPID));
        parms.put("username",username);
        parms.put("password",password);
        parms.put("code",code);
        parms.put("nickname",nickname);
        //parms.put("outid","0");
        IMHttpPostCallBack(url, parms,callback);
    }

    public void getNearByUser(String citycode,String lng,String lat,int page,final ResultCallback<String> callback)
    {
        String url = getURL("/api/getNearByUser");
        HashMap parms=new HashMap();
        parms.put("lng",lng);
        parms.put("lat",lat);
        parms.put("citycode",citycode);
        parms.put("page",page+"");
        parms.put("pagesize","20");
        IMHttpPostCallBack(url, parms,callback);
    }

    public void addFriend(int userid,final ResultCallback<String> callback)
    {
        String url = getURL("/api/addFriend");
        HashMap parms=new HashMap();
        parms.put("friuid",userid+"");
        IMHttpPostCallBack(url, parms,callback);
    }

    public void beFriend(String userid,final ResultCallback<String> callback)
    {
        String url = getURL("/api/agreeFriend");
        HashMap parms=new HashMap();
        parms.put("friuid",userid);
        IMHttpPostCallBack(url, parms,callback);
    }

}
