package com.zhangwuji.im.ui.plugin;

import com.zhangwuji.im.DB.entity.Group;
import com.zhangwuji.im.DB.entity.PeerEntity;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.imcore.service.IMService;

import java.io.Serializable;

public class IPluginData implements Serializable {

    private static final long serialVersionUID = 6696105826418755028L;
    private int conversationType=1;
    private PeerEntity peerEntity;
    private User loginUser;
    private IMService imService;
    private String currentSessionKey;

    public int getConversationType() {
        return conversationType;
    }

    public void setConversationType(int conversationType) {
        this.conversationType = conversationType;
    }

    public PeerEntity getPeerEntity() {
        return peerEntity;
    }

    public void setPeerEntity(PeerEntity peerEntity) {
        this.peerEntity = peerEntity;
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

    public String getCurrentSessionKey() {
        return currentSessionKey;
    }

    public void setCurrentSessionKey(String currentSessionKey) {
        this.currentSessionKey = currentSessionKey;
    }


}
