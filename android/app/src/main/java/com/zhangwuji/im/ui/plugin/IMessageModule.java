package com.zhangwuji.im.ui.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

public interface IMessageModule {

      View messageRender(IMessageData iMessageData,Object message,final int position, View convertView, final ViewGroup parent, final boolean isMine);

}
