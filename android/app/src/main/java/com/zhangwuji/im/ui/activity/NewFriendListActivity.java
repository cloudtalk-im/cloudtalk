package com.zhangwuji.im.ui.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;
import com.zhangwuji.im.R;
import com.zhangwuji.im.imcore.event.UserInfoEvent;
import com.zhangwuji.im.imcore.manager.IMContactManager;
import com.zhangwuji.im.server.network.BaseAction;
import com.zhangwuji.im.server.network.IMAction;
import com.zhangwuji.im.server.utils.json.JsonMananger;
import com.zhangwuji.im.ui.adapter.FriendListAdapter;
import com.zhangwuji.im.ui.base.TTBaseActivity;
import com.zhangwuji.im.ui.entity.UserRelationship;
import com.zhangwuji.im.ui.helper.ApiAction;
import com.zhangwuji.im.ui.helper.IMUIHelper;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by AMing on 16/3/8.
 * YuChen
 */
public class NewFriendListActivity extends TTBaseActivity implements  FriendListAdapter.OnItemButtonClick,  View.OnClickListener {

    private ListView mGroupListView;
    private FriendListAdapter adapter;
    private TextView mNoGroups;
    private List<UserRelationship> mList;
    private ProgressBar progress_bar;
    private ApiAction apiAction=null;
    private QMUITipDialog tipDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定布局资源(注意放所有资源初始化之前)
        LayoutInflater.from(this).inflate(R.layout.activity_new_friendlist, topContentView);

        //TOP_CONTENT_VIEW
        setLeftButton(R.drawable.ac_back_icon);

        mGroupListView=(ListView)findViewById(R.id.shiplistview);
        mNoGroups = (TextView) findViewById(R.id.isData);
        progress_bar = (ProgressBar)findViewById(R.id.progress_bar);
        topLeftBtn.setOnClickListener(this);
        letTitleTxt.setOnClickListener(this);
        topRightBtn.setOnClickListener(this);
        setTitle(R.string.new_friends);
        initData();
        tipDialog = new QMUITipDialog.Builder(this)
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                .setTipWord("请稍后..")
                .create();

    }

    private void initData() {

        IMAction imAction=new IMAction(this);
        imAction.getNewFriendList(new BaseAction.ResultCallback<String>() {
            @Override
            public void onSuccess(String s) {
                JSONObject objec=JSON.parseObject(s);
                if(objec.getIntValue("code")==200)
                {
                    mList=JsonMananger.jsonToList(objec.getJSONArray("data").toJSONString(),UserRelationship.class);
                    if (mList != null && mList.size() > 0)
                    {
                        adapter = new FriendListAdapter(NewFriendListActivity.this, mList);
                        adapter.setOnItemButtonClick(NewFriendListActivity.this);
                        mGroupListView.setAdapter(adapter);
                        mNoGroups.setVisibility(View.GONE);

                    }
                    else
                    {
                        mGroupListView.setVisibility(View.GONE);
                        mNoGroups.setVisibility(View.VISIBLE);
                    }
                }
                progress_bar.setVisibility(View.GONE);

                IMUIHelper.setConfigName(NewFriendListActivity.this,"newfriendinvite",0);


            }

            @Override
            public void onError(String errString) {
                progress_bar.setVisibility(View.GONE);
            }
        });

    }

    @Override
    public boolean onButtonClick(final int position, final View view, int status) {
        switch (status) {
            case 22: //收到了好友邀请
            {
                tipDialog.show();
               final UserRelationship user=mList.get(position);
                apiAction=new ApiAction(this);
                apiAction.beFriend(user.getId(), new BaseAction.ResultCallback<String>() {
                    @Override
                    public void onSuccess(String s) {
                        JSONObject objec=JSON.parseObject(s);
                        if(objec.getIntValue("code")==200)
                        {
                            tipDialog.dismiss();
                            tipDialog = new QMUITipDialog.Builder(NewFriendListActivity.this)
                                    .setIconType(QMUITipDialog.Builder.ICON_TYPE_SUCCESS)
                                    .setTipWord("处理成功!")
                                    .create();
                            tipDialog.show();

                            initData();

                            ArrayList<Integer> userId = new ArrayList<>();
                            userId.add(Integer.parseInt(user.getId()));

                            IMContactManager.instance().reqGetDetaillUsersCallbck(userId, 1, new BaseAction.ResultCallback<String>() {
                                @Override
                                public void onSuccess(String s) {
                                    EventBus.getDefault().postSticky(UserInfoEvent.USER_INFO_UPDATE);
                                }
                                @Override
                                public void onError(String errString) {
                                }
                            });
                        }
                        else   if(objec.getIntValue("code")==201)
                        {
                            tipDialog.dismiss();
                            tipDialog = new QMUITipDialog.Builder(NewFriendListActivity.this)
                                    .setIconType(QMUITipDialog.Builder.ICON_TYPE_FAIL)
                                    .setTipWord("数据异常!")
                                    .create();
                            tipDialog.show();
                        }

                        view.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                tipDialog.dismiss();
                            }
                        }, 2000);
                    }

                    @Override
                    public void onError(String errString) {
                        tipDialog.dismiss();
                    }
                });
            }
                break;
            case 21: // 发出了好友邀请
                break;
            case 11: // 忽略好友邀请
                break;
            case 1: // 已是好友
                break;
            case 0: // 删除了好友关系
                break;
            case 999:
                finish();
                break;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        switch (id) {
            case R.id.left_btn:
            case R.id.left_txt:
                finish();
                break;
        }
    }


    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
