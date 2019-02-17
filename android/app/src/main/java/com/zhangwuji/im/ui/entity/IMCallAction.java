package com.zhangwuji.im.ui.entity;

/**
 * Created by jackkim on 2018/8/8.
 */

public enum IMCallAction {
    ACTION_OUTGOING_CALL(1, "ACTION_OUTGOING_CALL"),
    ACTION_INCOMING_CALL(2, "ACTION_INCOMING_CALL"),
    ACTION_ADD_MEMBER(3, "ACTION_ADD_MEMBER"),
    ACTION_RESUME_CALL(4, "ACTION_RESUME_CALL");

    int value;
    String msg;
    IMCallAction(int v, String msg) {
        this.value = v;
        this.msg = msg;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return msg;
    }
}
