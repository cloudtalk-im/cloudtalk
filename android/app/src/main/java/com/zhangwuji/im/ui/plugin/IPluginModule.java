package com.zhangwuji.im.ui.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;

public interface IPluginModule {
    Drawable obtainDrawable(Context var1);

    String obtainTitle(Context var1);

    void onClick(Activity var1, IPluginData pluginData,int position);

    void onActivityResult(int var1, int var2, Intent var3);
}
