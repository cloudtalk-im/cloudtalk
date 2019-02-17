package com.zhangwuji.im.imcore.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageTag {
    int NONE = 0;
    int STATUS = 0;
    String value();
    int flag() default 0;
    Class<? extends IMessage> messageContent() default IMessage.class;
}