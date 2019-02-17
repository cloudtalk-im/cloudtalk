package com.zhangwuji.im.DB.sp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class LoginSp {

    private final String fileName = "imloginuser.ini";
    private Context ctx;
    private final String KEY_LOGIN_NAME = "loginName";
    private final String KEY_TOKEN = "token";
    private final String KEY_LOGIN_ID = "loginId";


    SharedPreferences sharedPreferences;

    private static LoginSp loginSp = null;
    public static LoginSp instance(){
        if(loginSp ==null){
            synchronized (LoginSp.class){
                loginSp = new LoginSp();
            }
        }
        return loginSp;
    }
    private LoginSp(){
    }


    public void  init(Context ctx){
        this.ctx = ctx;
        sharedPreferences= ctx.getSharedPreferences
                (fileName,ctx.MODE_PRIVATE);
    }

    public  void setLoginInfo(String userName,String token,int loginId){
        // 横写
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LOGIN_NAME, userName);
        editor.putString(KEY_TOKEN, token);
        editor.putInt(KEY_LOGIN_ID, loginId);
        //提交当前数据
        editor.commit();
        editor.apply();
    }

    public  void clearIMUser()
    {
        // 横写
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LOGIN_NAME, "");
        editor.putString(KEY_TOKEN, "");
        editor.putInt(KEY_LOGIN_ID, 0);
        //提交当前数据
        editor.commit();
    }

    public SpLoginIdentity getLoginIdentity(){
        String userName =  sharedPreferences.getString(KEY_LOGIN_NAME,null);
        String token = sharedPreferences.getString(KEY_TOKEN,null);
        int loginId = sharedPreferences.getInt(KEY_LOGIN_ID,0);
        /**pwd不判空: loginOut的时候会将pwd清空*/
        if(TextUtils.isEmpty(userName) || TextUtils.isEmpty(token) || loginId == 0){
            return null;
        }
        return new SpLoginIdentity(userName,token,loginId);
    }

    public class SpLoginIdentity{
        private String loginName;
        private String token;
        private int loginId;

        public SpLoginIdentity(String mUserName,String mToken,int mLoginId){
            loginName = mUserName;
            token = mToken;
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

        public String getToken() {
            return token;
        }
    }
}
