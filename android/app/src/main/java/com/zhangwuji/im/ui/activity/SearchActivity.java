package com.zhangwuji.im.ui.activity;

import android.os.Bundle;

import com.zhangwuji.im.R;
import com.zhangwuji.im.imcore.manager.IMStackManager;
import com.zhangwuji.im.ui.base.TTBaseFragmentActivity;

public class SearchActivity extends TTBaseFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
        IMStackManager.getStackManager().pushActivity(this);
		setContentView(R.layout.tt_fragment_activity_search);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
        IMStackManager.getStackManager().popActivity(this);
		super.onDestroy();
	}

}
