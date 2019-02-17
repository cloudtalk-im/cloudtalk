package com.zhangwuji.im.ui.widget;

import android.net.Uri;

public interface IImageLoadingListener {
    void onLoadingComplete(Uri var1);

    void onLoadingFail();
}

