package com.zhangwuji.im.ui.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class LoginInfoSp {

    private final String fileName = "logininfo.ini";
    private Context ctx;
    private final String KEY_LOGIN_NAME = "loginName";
    private final String KEY_PWD = "password";
    private final String KEY_LOGIN_ID = "loginId";
    private final String KEY_TOKEN = "token";

    private final String BQMM_APPID = "bqmm_appid";
    private final String BQMM_APPSECRET = "bqmm_appsecret";


    SharedPreferences sharedPreferences;

    private static LoginInfoSp loginSp = null;
    public static LoginInfoSp instance(){
        if(loginSp ==null){
            synchronized (LoginInfoSp.class){
                loginSp = new LoginInfoSp();
            }
        }
        return loginSp;
    }
    private LoginInfoSp(){
    }


    public void  init(Context ctx){
        this.ctx = ctx;
        sharedPreferences= ctx.getSharedPreferences(fileName,ctx.MODE_PRIVATE);
    }

    public void setBqmmPlug(String appid,String secret)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(BQMM_APPID, appid);
        editor.putString(BQMM_APPSECRET, secret);
        //提交当前数据
        editor.commit();
    }

    public bqmmplugSpIdentity getbqmmplug()
    {
        String appid =  sharedPreferences.getString(BQMM_APPID,null);
        String secret = sharedPreferences.getString(BQMM_APPSECRET,null);
        return new bqmmplugSpIdentity(appid,secret);
    }

    public  void setLoginInfo(String userName,String pwd,String token,int loginId){
        // 横写
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LOGIN_NAME, userName);
        editor.putString(KEY_PWD, pwd);
        editor.putString(KEY_TOKEN, token);
        editor.putInt(KEY_LOGIN_ID, loginId);
        //提交当前数据
        editor.commit();
    }

    public void clearLoginInfo()
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
      //  editor.putString(KEY_LOGIN_NAME, "");
        editor.putString(KEY_PWD, "");
        editor.putString(KEY_TOKEN, "");
        editor.putInt(KEY_LOGIN_ID, 0);
        //提交当前数据
        editor.commit();
    }

    public LoginInfoSp.LoginInfoSpIdentity getLoginInfoIdentity(){
        String userName =  sharedPreferences.getString(KEY_LOGIN_NAME,null);
        String token = sharedPreferences.getString(KEY_TOKEN,null);
        String loginpwd = sharedPreferences.getString(KEY_PWD,null);
        int loginId = sharedPreferences.getInt(KEY_LOGIN_ID,0);
        /**pwd不判空: loginOut的时候会将pwd清空*/
        if(TextUtils.isEmpty(token))
        {
            return null;
        }
        return new LoginInfoSp.LoginInfoSpIdentity(userName,loginpwd,token,loginId);
    }
    public class bqmmplugSpIdentity{
        private String appid;
        private String secret;

        public bqmmplugSpIdentity(String appid, String secret) {
            this.appid = appid;
            this.secret = secret;
        }

        public String getAppid() {
            return appid;
        }

        public void setAppid(String appid) {
            this.appid = appid;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    public class LoginInfoSpIdentity{
        private String loginName;
        private String loginPwd;
        private String token;
        private int loginId;

        public LoginInfoSpIdentity(String mUserName,String mUserPWD,String mToken,int mLoginId){
            loginName = mUserName;
            loginPwd = mUserPWD;
            token=mToken;
            loginId = mLoginId;
        }

        public int getLoginId() {
            return loginId;
        }

        public void setLoginId(int loginId) {
            this.loginId = loginId;
        }

        public String getLoginName() {
            return loginName;
        }

        public String getLoginPwd() {
            return loginPwd;
        }
        public String getToken() {
            return token;
        }
    }
}
