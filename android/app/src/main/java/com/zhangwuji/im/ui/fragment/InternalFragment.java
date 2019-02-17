package com.zhangwuji.im.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.zhangwuji.im.R;
import com.zhangwuji.im.config.IntentConstant;
import com.zhangwuji.im.ui.activity.NearByPeopleListActivity;
import com.zhangwuji.im.ui.activity.NewFriendListActivity;
import com.zhangwuji.im.ui.activity.WebViewFragmentActivity;
import com.zhangwuji.im.ui.adapter.InternalAdapter;
import com.zhangwuji.im.ui.base.TTBaseFragment;

public class InternalFragment extends TTBaseFragment implements View.OnClickListener{
    private View curView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (null != curView) {
            ((ViewGroup) curView.getParent()).removeView(curView);
            return curView;
        }
        curView = inflater.inflate(R.layout.fragment_discover,
                topContentView);

        initRes();
        return curView;
    }

    private void initRes() {
        // 设置顶部标题栏
        setTopTitle(getActivity().getString(R.string.main_innernet));

        LinearLayout ll_nearby=(LinearLayout)curView.findViewById(R.id.ll_nearby);
        ll_nearby.setOnClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void initHandler() {
    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.ll_nearby)
        {
            Intent intent2=new Intent(getActivity(),NearByPeopleListActivity.class);
            getActivity().startActivity(intent2);
        }
    }
}
