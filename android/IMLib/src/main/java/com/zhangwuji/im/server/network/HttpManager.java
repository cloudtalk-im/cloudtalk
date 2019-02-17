package com.zhangwuji.im.server.network;

import com.zhangwuji.im.server.network.http.HttpException;
import com.zhangwuji.im.server.network.async.OnDataListener;

public class HttpManager implements OnDataListener {


    @Override
    public Object doInBackground(int requestCode, String parameter) throws HttpException {
        return null;
    }

    @Override
    public void onSuccess(int requestCode, Object result) {

    }

    @Override
    public void onFailure(int requestCode, int state, Object result) {

    }
}
