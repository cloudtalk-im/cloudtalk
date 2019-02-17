package com.zhangwuji.im.ui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class CTMessageFrameLayout extends FrameLayout {
    private Drawable mOldDrawable;

    public CTMessageFrameLayout(Context context) {
        super(context);
    }

    public CTMessageFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CTMessageFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBackgroundResource(int resid) {
        super.setBackgroundResource(resid);
        this.mOldDrawable = this.getBackground();
        this.setBackgroundDrawable((Drawable)null);
        this.setPadding(0, 0, 0, 0);
    }

    public Drawable getBackgroundDrawable() {
        return this.mOldDrawable;
    }
}

