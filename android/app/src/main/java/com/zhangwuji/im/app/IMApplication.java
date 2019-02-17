package com.zhangwuji.im.app;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.tencent.bugly.crashreport.CrashReport;
//import com.zhangwuji.im.MyEventBusIndex;
//import com.zhangwuji.im.MyLibEventBusIndex;
import com.zhangwuji.im.imcore.service.IMService;
import com.zhangwuji.im.utils.ImageLoaderUtil;
import com.zhangwuji.im.utils.Logger;

import org.greenrobot.eventbus.EventBus;


public class IMApplication extends MultiDexApplication {

	private Logger logger = Logger.getLogger(IMApplication.class);
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		logger.i("Application starts");
		startIMService();
		ImageLoaderUtil.initImageLoaderConfig(getApplicationContext());
	//	EventBus.builder().addIndex(new MyEventBusIndex()).addIndex(new MyLibEventBusIndex()).installDefaultEventBus();

		//腾讯异常上报平台,配置成自已的appid即可
		CrashReport.initCrashReport(getApplicationContext(), "d38a1aacfc", true);
	}

	private void startIMService() {
		logger.i("start IMService");
		Intent intent = new Intent();
		intent.setClass(this, IMService.class);
		startService(intent);
	}

	public static boolean gifRunning = true;//gif是否运行
}
