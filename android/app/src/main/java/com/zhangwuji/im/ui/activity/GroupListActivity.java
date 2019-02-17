package com.zhangwuji.im.ui.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.zhangwuji.im.DB.entity.Group;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.R;
import com.zhangwuji.im.config.SysConstant;
import com.zhangwuji.im.imcore.manager.IMContactManager;
import com.zhangwuji.im.imcore.manager.IMGroupManager;
import com.zhangwuji.im.imcore.service.IMService;
import com.zhangwuji.im.imcore.service.IMServiceConnector;
import com.zhangwuji.im.ui.base.TTBaseActivity;
import com.zhangwuji.im.ui.fragment.ContactFragment;
import com.zhangwuji.im.ui.helper.IMUIHelper;
import com.zhangwuji.im.ui.widget.IMGroupAvatar;
import com.zhangwuji.im.ui.widget.SelectableRoundedImageView;
import com.zhangwuji.im.utils.AvatarGenerate;
import com.zhangwuji.im.utils.ScreenUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by AMing on 16/3/8.
 * YuChen
 */
public class GroupListActivity extends TTBaseActivity implements View.OnClickListener {

    private ListView mGroupListView;
    private GroupAdapter adapter;
    private TextView mNoGroups;
    private EditText mSearch;
    private List<Group> mList;
    private TextView mTextView;
    private boolean isShareSelectFriend=false;
    private IMService imService;
    private IMGroupManager groupMgr;
    private ProgressBar progress_bar;

    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
        @Override
        public void onIMServiceConnected() {
            logger.d("contactUI#onIMServiceConnected");

            imService = imServiceConnector.getIMService();
            if (imService == null) {
                logger.e("ContactFragment#onIMServiceConnected# imservice is null!!");
                return;
            }
            groupMgr = imService.getGroupManager();
          //  EventBus.getDefault().register(GroupListActivity.this);
            initData();
        }

        @Override
        public void onServiceDisconnected() {
            if (EventBus.getDefault().isRegistered(GroupListActivity.this)) {
                EventBus.getDefault().unregister(GroupListActivity.this);
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imServiceConnector.connect(this);
        // 绑定布局资源(注意放所有资源初始化之前)
        LayoutInflater.from(this).inflate(R.layout.fr_group_list, topContentView);

        //TOP_CONTENT_VIEW
        setLeftButton(R.drawable.ac_back_icon);
       // setLeftText(getResources().getString(R.string.top_left_back));
        setRightButton(R.drawable.de_ic_add);
        mGroupListView=(ListView)findViewById(R.id.all_group_list);

        mNoGroups = (TextView) findViewById(R.id.show_no_group);
        mTextView = (TextView)findViewById(R.id.foot_group_size);
        progress_bar = (ProgressBar)findViewById(R.id.progress_bar);

        topLeftBtn.setOnClickListener(this);
        letTitleTxt.setOnClickListener(this);
        topRightBtn.setOnClickListener(this);

        setTitle("我的群组");
    }

    private void initData() {
     mList=groupMgr.getNormalGroupList();
      if (mList != null && mList.size() > 0) {
            adapter = new GroupAdapter(this, mList);
            mGroupListView.setAdapter(adapter);
            mNoGroups.setVisibility(View.GONE);
            mTextView.setVisibility(View.VISIBLE);
            mTextView.setText(getString(R.string.ac_group_list_group_number, mList.size()));
            mGroupListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Group bean = (Group) adapter.getItem(position);
                    if (isShareSelectFriend) {
                        Intent intent = new Intent();
                        intent.putExtra("targetId", bean.getId());
                        setResult(RESULT_OK, intent);
                        finish();
                    } else {
                       //进入群聊天会话
                        IMUIHelper.openChatActivity(GroupListActivity.this,bean.getSessionKey());
                    }

                }
            });
       }
       else
       {
           mGroupListView.setVisibility(View.GONE);
           mNoGroups.setVisibility(View.VISIBLE);
       }
    }

    private void filterData(String s) {
        List<Group> filterDataList = new ArrayList<>();
        if (TextUtils.isEmpty(s)) {
            filterDataList = mList;
        } else {
            for (Group groups : mList) {
                if (groups.getMainName().contains(s)) {
                    filterDataList.add(groups);
                }
            }
        }
        adapter.updateListView(filterDataList);
       // mTextView.setText(getString(R.string.ac_group_list_group_number, filterDataList.size()));
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


    class GroupAdapter extends BaseAdapter {

        private Context context;

        private List<Group> list;

        public GroupAdapter(Context context, List<Group> list) {
            this.context = context;
            this.list = list;
        }

        /**
         * 传入新的数据 刷新UI的方法
         */
        public void updateListView(List<Group> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (list != null) return list.size();
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (list == null)
                return null;

            if (position >= list.size())
                return null;

            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            final Group mContent = list.get(position);
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.group_item_new, parent, false);
                viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.groupname);
                viewHolder.mImageView = (IMGroupAvatar) convertView.findViewById(R.id.contact_portrait);
                viewHolder.groupId = (TextView) convertView.findViewById(R.id.group_id);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.tvTitle.setText(mContent.getMainName());
           // String portraitUri = IMUserInfoManager.getInstance().getPortraitUri(mContent);
          //  ImageLoader.getInstance().displayImage(portraitUri, viewHolder.mImageView, App.getOptions());
            List<String> avatarUrlList = new ArrayList<>();
            Set<Integer> userIds = mContent.getlistGroupMemberIds();
            int i = 0;
            for(Integer buddyId:userIds){
                User entity = imService.getContactManager().findContact(buddyId);
                if (entity == null) {
                    continue;
                }
                avatarUrlList.add(AvatarGenerate.generateAvatar(entity.getAvatar(),entity.getMainName(),entity.getPeerId()+""));
                if (i >= 3) {
                    break;
                }
                i++;
            }
            setGroupAvatar(viewHolder.mImageView,avatarUrlList);

            return convertView;
        }


        class ViewHolder {
            /**
             * 昵称
             */
            TextView tvTitle;
            /**
             * 头像
             */
            IMGroupAvatar mImageView;
            /**
             * userId
             */
            TextView groupId;
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
    private void setGroupAvatar(IMGroupAvatar avatar, List<String> avatarUrlList){
        try {
            avatar.setViewSize(ScreenUtil.instance(this).dip2px(38));
            avatar.setChildCorner(2);
            avatar.setAvatarUrlAppend(SysConstant.AVATAR_APPEND_32);
            avatar.setParentPadding(3);
            avatar.setAvatarUrls((ArrayList<String>) avatarUrlList);
        }catch (Exception e){
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
