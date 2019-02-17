package com.zhangwuji.im.ui.plugin;

import android.content.Context;

import com.zhangwuji.im.DB.entity.PeerEntity;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.imcore.service.IMService;

import java.io.Serializable;

public class IMessageData implements Serializable {

    private static final long serialVersionUID = 6966763426790160814L;
    private int conversationType=1;
    private User loginUser;
    private IMService imService;
    private Context ctx;

    public int getConversationType() {
        return conversationType;
    }

    public void setConversationType(int conversationType) {
        this.conversationType = conversationType;
    }

    public User getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(User loginUser) {
        this.loginUser = loginUser;
    }

    public IMService getImService() {
        return imService;
    }

    public void setImService(IMService imService) {
        this.imService = imService;
    }

    public Context getCtx() {
        return ctx;
    }

    public void setCtx(Context ctx) {
        this.ctx = ctx;
    }
}
