package com.zhangwuji.im.ui.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.handmark.pulltorefresh.library.ILoadingLayout;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;
import com.zhangwuji.im.R;
import com.zhangwuji.im.imcore.manager.IMContactManager;
import com.zhangwuji.im.protobuf.helper.EntityChangeEngine;
import com.zhangwuji.im.server.network.BaseAction;
import com.zhangwuji.im.server.utils.json.JsonMananger;
import com.zhangwuji.im.ui.base.TTBaseActivity;
import com.zhangwuji.im.ui.entity.NearByUser;
import com.zhangwuji.im.ui.helper.ApiAction;
import com.zhangwuji.im.ui.helper.IMUIHelper;
import com.zhangwuji.im.ui.widget.IMBaseImageView;
import com.zhangwuji.im.utils.AvatarGenerate;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by AMing on 16/3/8.
 * YuChen
 */
public class NearByPeopleInfoActivity extends TTBaseActivity implements View.OnClickListener{


    private ApiAction apiAction=null;
    private NearByUser userinfo=new NearByUser();
    private    Button bt_addfriend, bt_chats;
    private  QMUITipDialog tipDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater.from(this).inflate(R.layout.activity_nearbypeople_detail, topContentView);

        apiAction=new ApiAction(this);
        setLeftButton(R.drawable.ac_back_icon);
        topLeftBtn.setOnClickListener(this);

        setTitle(R.string.nearbyinfo);

        userinfo=(NearByUser)getIntent().getSerializableExtra("userinfo");

        tipDialog = new QMUITipDialog.Builder(this)
                .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                .setTipWord("正在提交")
                .create();
        initData();
    }

    private void initData() {
        if(userinfo==null && userinfo.getId()<=0)finish();

        IMBaseImageView new_header=(IMBaseImageView)findViewById(R.id.new_header);
        new_header.setCorner(8);
        new_header.setImageUrl(AvatarGenerate.generateAvatar(userinfo.getAvatar(),userinfo.getMainName(),userinfo.getId()+""));


        TextView tv_names=(TextView)findViewById(R.id.tv_names);
        tv_names.setText(userinfo.getNickname());

        ImageView seximg=(ImageView)findViewById(R.id.iv_sexs);
        int sex=userinfo.getSex();
        if(sex==1)
        {
         seximg.setImageResource(R.drawable.userinfo_male);
        }
        else
        {
            seximg.setImageResource(R.drawable.userinfo_female);
        }

        TextView tv_signs=(TextView)findViewById(R.id.tv_signs);
        tv_signs.setText(userinfo.getSign_info());

        TextView tv_diatances=(TextView)findViewById(R.id.tv_diatances);
        String distance=userinfo.getDists();
        try {
            if(Float.parseFloat(distance)>1000)
            {
                distance = (IMUIHelper.floatMac1(Float.parseFloat(distance) / 1000)) + "公里以内.";
            }
            else
            {
                distance = (IMUIHelper.floatMac(distance)) + "米以内.";
            }
        } catch (Exception e) {
        }
        tv_diatances.setText(distance);

         bt_addfriend=(Button)findViewById(R.id.bt_addfriend);
        bt_addfriend.setOnClickListener(this);

         bt_chats=(Button)findViewById(R.id.bt_chats);
        bt_chats.setOnClickListener(this);

        if(IMContactManager.instance().getUserMap().containsKey(userinfo.getPeerId()))
        {
            bt_addfriend.setVisibility(View.GONE);
            bt_chats.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(final View view) {
        final int id = view.getId();
        switch (id) {
            case R.id.left_btn:
            case R.id.left_txt:
                finish();
                break;
            case R.id.bt_addfriend: {
                tipDialog.show();
                apiAction = new ApiAction(this);
                apiAction.addFriend(userinfo.getPeerId(), new BaseAction.ResultCallback<String>() {
                    @Override
                    public void onSuccess(String s)
                    {
                        JSONObject objec=JSON.parseObject(s);
                        if(objec.getIntValue("code")==200)
                        {
                            bt_addfriend.setVisibility(View.GONE);
                            tipDialog.dismiss();
                            tipDialog = new QMUITipDialog.Builder(NearByPeopleInfoActivity.this)
                                    .setIconType(QMUITipDialog.Builder.ICON_TYPE_SUCCESS)
                                    .setTipWord("申请成功!")
                                    .create();
                            tipDialog.show();
                        }
                        else  if(objec.getIntValue("code")==201)
                        {
                            tipDialog.dismiss();
                            tipDialog = new QMUITipDialog.Builder(NearByPeopleInfoActivity.this)
                                    .setIconType(QMUITipDialog.Builder.ICON_TYPE_FAIL)
                                    .setTipWord("已经是好友!")
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
            case R.id.bt_chats:
            {
                IMUIHelper.openChatActivity(this, EntityChangeEngine.getSessionKey(userinfo.getId(),1));
                finish();
            }break;
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
