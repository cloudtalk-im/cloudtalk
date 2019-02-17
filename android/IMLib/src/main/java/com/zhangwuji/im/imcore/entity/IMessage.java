package com.zhangwuji.im.imcore.entity;

import java.io.Serializable;

public class IMessage implements Serializable {
    private static final long serialVersionUID = 6428911321944004482L;
    private String tag;
    private String content;
    private String displaytext;//显示在会话窗口的简称

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDisplaytext() {
        return displaytext;
    }

    public void setDisplaytext(String displaytext) {
        this.displaytext = displaytext;
    }
}