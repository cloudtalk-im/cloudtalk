package com.zhangwuji.im.ui.widget;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.zhangwuji.im.utils.CommonUtil;
import com.zhangwuji.im.utils.NetworkUtil;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import okhttp3.internal.cache.DiskLruCache;

/**
 * Created by zhujian on 15/3/26.
 */
public class GifLoadTask extends AsyncTask<String, Void, byte[]> {

    public GifLoadTask() {
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected byte[] doInBackground(final String... params) {
        final String gifUrl = params[0];
        if (gifUrl == null)
            return null;
        byte[] gif = null;
        try {
            gif = byteArrayHttpClient(gifUrl);
        } catch (OutOfMemoryError e) {
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gif;
    }

    private FilterInputStream getFromCache(String url) throws Exception {
//        DiskLruCache cache = DiskLruCache.open(CommonUtil.getImageSavePath(), 1, 2, 2*1024*1024);
//        cache.flush();
//        String key = Util.hash(url);
//        final DiskLruCache.Snapshot snapshot;
//        try {
//            snapshot = cache.get(key);
//            if (snapshot == null) {
//                return null;
//            }
//        } catch (IOException e) {
//            return null;
//        }
//        FilterInputStream bodyIn = new FilterInputStream(snapshot.getInputStream(1)) {
//            @Override
//            public void close() throws IOException {
//                snapshot.close();
//                super.close();
//            }
//        };
//        return bodyIn;
        return null;
    }

    public byte[] byteArrayHttpClient(final String urlString) throws Exception {
        OkHttpClient client = null;
        if (client == null) {

            client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .cache(new Cache(CommonUtil.getImageSavePath(), 20 * 1024 * 1024))
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        }

        FilterInputStream inputStream = getFromCache(urlString);
        if (inputStream != null) {
            return IOUtils.toByteArray(inputStream);
        }
        InputStream in = null;
        try {
            final String decodedUrl = URLDecoder.decode(urlString, "UTF-8");
            final URL url = new URL(decodedUrl);
            final Request request = new Request.Builder().url(url).build();
            final Response response = client.newCall(request).execute();
            in = response.body().byteStream();
            return IOUtils.toByteArray(in);
        } catch (final MalformedURLException e) {
        } catch (final OutOfMemoryError e) {
        } catch (final UnsupportedEncodingException e) {
        } catch (final IOException e) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException ignored) {
                }
            }
        }
        return null;
    }


    Interceptor interceptor = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);

            String cacheControl = request.cacheControl().toString();
            if (TextUtils.isEmpty(cacheControl)) {
                cacheControl = "public, max-age=6000";
            }
            return response.newBuilder()
                    .header("Cache-Control", cacheControl)
                    .removeHeader("Pragma")
                    .build();
        }
    };

    final Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response originResponse = chain.proceed(chain.request());
                // 5 分钟后过期
                CacheControl.Builder builder = new CacheControl.Builder()
                        .maxAge(5, TimeUnit.MINUTES);

                return originResponse.newBuilder()
                        .header("Cache-Control", builder.build().toString())
                        .build();
            }
    };
}


