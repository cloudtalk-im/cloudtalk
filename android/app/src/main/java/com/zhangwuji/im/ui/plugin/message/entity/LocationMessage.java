package com.zhangwuji.im.ui.plugin.message.entity;


import android.net.Uri;

import com.zhangwuji.im.imcore.entity.IMessage;
import com.zhangwuji.im.imcore.entity.MessageTag;

@MessageTag(value = "cloudtalk:location")
public class LocationMessage extends IMessage {

    double mLat;
    double mLng;
    String mPoi;
    String mBase64;
    String mImgUri;
    protected String extra;

    public LocationMessage()
    {

    }
    public LocationMessage(double mLat, double mLng, String mPoi, String mImgUri) {
        this.mLat = mLat;
        this.mLng = mLng;
        this.mPoi = mPoi;
        this.mImgUri = mImgUri;
    }

    public double getmLat() {
        return mLat;
    }

    public void setmLat(double mLat) {
        this.mLat = mLat;
    }

    public double getmLng() {
        return mLng;
    }

    public void setmLng(double mLng) {
        this.mLng = mLng;
    }

    public String getmPoi() {
        return mPoi;
    }

    public void setmPoi(String mPoi) {
        this.mPoi = mPoi;
    }

    public String getmBase64() {
        return mBase64;
    }

    public void setmBase64(String mBase64) {
        this.mBase64 = mBase64;
    }

    public String getmImgUri() {
        return mImgUri;
    }

    public void setmImgUri(String mImgUri) {
        this.mImgUri = mImgUri;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
