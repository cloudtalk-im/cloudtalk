package com.zhangwuji.im.ui.plugin.message.entity;

import com.zhangwuji.im.imcore.entity.IMessage;
import com.zhangwuji.im.imcore.entity.MessageTag;

@MessageTag(value = "cloudtalk:bigmoji")
public class BigMojiMessage extends IMessage {

    private String type;
    private String thumbail;
    private String mainimage;
    private String emotext;
    private String code;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getThumbail() {
        return thumbail;
    }

    public void setThumbail(String thumbail) {
        this.thumbail = thumbail;
    }

    public String getMainimage() {
        return mainimage;
    }

    public void setMainimage(String mainimage) {
        this.mainimage = mainimage;
    }

    public String getEmotext() {
        return emotext;
    }

    public void setEmotext(String emotext) {
        this.emotext = emotext;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
