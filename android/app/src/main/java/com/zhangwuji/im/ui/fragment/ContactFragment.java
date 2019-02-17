package com.zhangwuji.im.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zhangwuji.im.DB.entity.Department;
import com.zhangwuji.im.DB.entity.Group;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.R;
import com.zhangwuji.im.config.HandlerConstant;
import com.zhangwuji.im.imcore.event.GroupEvent;
import com.zhangwuji.im.imcore.event.PriorityEvent;
import com.zhangwuji.im.imcore.event.UserInfoEvent;
import com.zhangwuji.im.imcore.service.IMServiceConnector;
import com.zhangwuji.im.imcore.manager.IMContactManager;
import com.zhangwuji.im.imcore.service.IMService;
import com.zhangwuji.im.protobuf.IMSwitchService;
import com.zhangwuji.im.ui.activity.GroupListActivity;
import com.zhangwuji.im.ui.activity.NewFriendListActivity;
import com.zhangwuji.im.ui.activity.VideoChatViewActivity2;
import com.zhangwuji.im.ui.adapter.ContactAdapter;
import com.zhangwuji.im.ui.adapter.DeptAdapter;
import com.zhangwuji.im.ui.entity.IMCallAction;
import com.zhangwuji.im.ui.helper.IMUIHelper;
import com.zhangwuji.im.ui.widget.SortSideBar;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import java.util.List;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 通讯录 （全部、部门）
 */
public class ContactFragment extends MainFragment implements SortSideBar.OnTouchingLetterChangedListener,View.OnClickListener{
    private View curView = null;
    private static Handler uiHandler = null;
    private ListView allContactListView;
    private ListView departmentContactListView;
    private SortSideBar sortSideBar;
    private TextView dialog;

    private ContactAdapter contactAdapter;
    private DeptAdapter departmentAdapter;

    private IMService imService;
    private IMContactManager contactMgr;
    private int curTabIndex = 0;
    private View mHeadView;
    private TextView mNoFriends;
    private TextView mUnreadTextView;

    private IMServiceConnector imServiceConnector = new IMServiceConnector() {
        @Override
        public void onIMServiceConnected() {
            logger.d("contactUI#onIMServiceConnected");

            imService = imServiceConnector.getIMService();
            if (imService == null) {
                logger.e("ContactFragment#onIMServiceConnected# imservice is null!!");
                return;
            }
            contactMgr = imService.getContactManager();

            // 初始化视图
            initAdapter();
            renderEntityList();
            EventBus.getDefault().register(ContactFragment.this);
        }

        @Override
        public void onServiceDisconnected() {
            if (EventBus.getDefault().isRegistered(ContactFragment.this)) {
                EventBus.getDefault().unregister(ContactFragment.this);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imServiceConnector.connect(getActivity());
        initHandler();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(ContactFragment.this)) {
            EventBus.getDefault().unregister(ContactFragment.this);
        }
        imServiceConnector.disconnect(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        if(contactMgr!=null) {
            contactMgr.onLocalNetOk();
        }

        int oldnum=IMUIHelper.getConfigNameAsInt(getActivity(),"newfriendinvite");
        if(oldnum>0)
        {
            mUnreadTextView.setVisibility(View.VISIBLE);
            if(oldnum>99)
            {
                mUnreadTextView.setText("99+");
            }
            else
            {
                mUnreadTextView.setText(oldnum+"");
            }
        }
        else
        {
            mUnreadTextView.setVisibility(View.GONE);
        }
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void initHandler() {
        uiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case HandlerConstant.HANDLER_CHANGE_CONTACT_TAB:
                        if (null != msg.obj) {
                            curTabIndex = (Integer) msg.obj;
                            if (0 == curTabIndex) {
                                allContactListView.setVisibility(View.VISIBLE);
                                departmentContactListView.setVisibility(View.GONE);
                            } else {
                                departmentContactListView.setVisibility(View.VISIBLE);
                                allContactListView.setVisibility(View.GONE);
                            }
                        }
                        break;
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (null != curView) {
            ((ViewGroup) curView.getParent()).removeView(curView);
            return curView;
        }
        curView = inflater.inflate(R.layout.tt_fragment_contact, topContentView);
        initRes();
        return curView;
    }

    /**
     * @Description 初始化界面资源
     */
    private void initRes() {
        // 设置顶部标题栏
        showContactTopBar();
        hideTopBar();

        super.init(curView);
        showProgressBar();

        sortSideBar = (SortSideBar) curView.findViewById(R.id.sidrbar);
        dialog = (TextView) curView.findViewById(R.id.dialog);
        sortSideBar.setTextView(dialog);
        sortSideBar.setOnTouchingLetterChangedListener(this);
        mNoFriends = (TextView) curView.findViewById(R.id.show_no_friend);

        allContactListView = (ListView) curView.findViewById(R.id.all_contact_list);
        departmentContactListView = (ListView) curView.findViewById(R.id.department_contact_list);

        LayoutInflater mLayoutInflater = LayoutInflater.from(getActivity());
        mHeadView = mLayoutInflater.inflate(R.layout.item_contact_list_header,
                null);
        mUnreadTextView = (TextView)mHeadView.findViewById(R.id.tv_unread);
        RelativeLayout newFriendsLayout = (RelativeLayout) mHeadView.findViewById(R.id.re_newfriends);
        RelativeLayout groupLayout = (RelativeLayout) mHeadView.findViewById(R.id.re_grouplist);
        mUnreadTextView= (TextView) mHeadView.findViewById(R.id.tv_unread);
        allContactListView.addHeaderView(mHeadView);

        groupLayout.setOnClickListener(this);
        newFriendsLayout.setOnClickListener(this);

        //this is critical, disable loading when finger sliding, otherwise you'll find sliding is not very smooth
        allContactListView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, true));
        departmentContactListView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(), true, true));
        // todo eric
        // showLoadingProgressBar(true);

    }

    private void initAdapter(){
        contactAdapter = new ContactAdapter(getActivity(),imService);
        departmentAdapter = new DeptAdapter(getActivity(),imService);
        allContactListView.setAdapter(contactAdapter);
        departmentContactListView.setAdapter(departmentAdapter);

        // 单击视图事件
        allContactListView.setOnItemClickListener(contactAdapter);
        allContactListView.setOnItemLongClickListener(contactAdapter);

        departmentContactListView.setOnItemClickListener(departmentAdapter);
        departmentContactListView.setOnItemLongClickListener(departmentAdapter);
    }

    public void locateDepartment(int departmentId) {
        logger.d("department#locateDepartment id:%s", departmentId);

        if (topContactTitle == null) {
            logger.e("department#TopTabButton is null");
            return;
        }
        Button tabDepartmentBtn = topContactTitle.getTabDepartmentBtn();
        if (tabDepartmentBtn == null) {
            return;
        }
        tabDepartmentBtn.performClick();
        locateDepartmentImpl(departmentId);
    }

    private void locateDepartmentImpl(int departmentId) {
        if (imService == null) {
            return;
        }
        Department department = imService.getContactManager().findDepartment(departmentId);
        if (department == null) {
            logger.e("department#no such id:%s", departmentId);
            return;
        }

        logger.d("department#go to locate department:%s", department);
        final int position = departmentAdapter.locateDepartment(department.getDepartName());
        logger.d("department#located position:%d", position);

        if (position < 0) {
            logger.i("department#locateDepartment id:%s failed", departmentId);
            return;
        }
        //the first time locate works
        //from the second time, the locating operations fail ever since
        departmentContactListView.post(new Runnable() {

            @Override
            public void run() {
                departmentContactListView.setSelection(position);
            }
        });
    }


    /**
     * 刷新单个entity
     * 很消耗性能
     */
    private void renderEntityList() {
        hideProgressBar();
        logger.d("contact#renderEntityList");

        if (contactMgr.isUserDataReady() ) {
            renderUserList();
            renderDeptList();
        }
        if (imService.getGroupManager().isGroupReady()) {
         //   renderGroupList();
        }
        showSearchFrameLayout();
    }


    private void renderDeptList(){
        /**---------------------部门数据的渲染------------------------------------------*/
        List<User> departmentList = contactMgr.getDepartmentTabSortedList();
        departmentAdapter.putUserList(departmentList);
    }

    private void renderUserList(){
        List<User> contactList = contactMgr.getContactSortedList();
        // 没有任何的联系人数据
        if (contactList.size() <= 0) {
            return;
        }
        contactAdapter.putUserList(contactList);
    }

    private void renderGroupList() {
        logger.d("group#onGroupReady");
        List<Group> originList = imService.getGroupManager().getNormalGroupSortedList();
        if(originList.size() <= 0){
            return;
        }
        contactAdapter.putGroupList(originList);
    }

    private ListView getCurListView() {
        if (0 == curTabIndex) {
            return allContactListView;
        } else {
            return departmentContactListView;
        }
    }

    @Override
    public void onTouchingLetterChanged(String s) {
        int position = -1;
        if (0 == curTabIndex) {
            position =  contactAdapter.getPositionForSection(s.charAt(0));
        } else {
            position =  departmentAdapter.getPositionForSection(s.charAt(0));
        }
        if (position != -1) {
            getCurListView().setSelection(position);
        }
    }

    public static Handler getHandler() {
        return uiHandler;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onUserEvent(GroupEvent event) {
        switch (event.getEvent()) {
            case GROUP_INFO_UPDATED:
            case GROUP_INFO_OK:
                renderGroupList();
                searchDataReady();
                break;
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onUserEvent(UserInfoEvent event) {
        switch (event) {
            case USER_INFO_UPDATE:
            case USER_INFO_OK:
                renderDeptList();
                renderUserList();
                searchDataReady();
                contactAdapter.notifyDataSetChanged();
                break;
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUserEvent(PriorityEvent event){
        switch (event.event){
            case MSG_FRIEND_INVITE:
            {
                int oldnum=IMUIHelper.getConfigNameAsInt(getActivity(),"newfriendinvite");
                if(oldnum>0)
                {
                    mUnreadTextView.setVisibility(View.VISIBLE);
                    if(oldnum>99)
                    {
                        mUnreadTextView.setText("99+");
                    }
                    else
                    {
                        mUnreadTextView.setText(oldnum+"");
                    }
                }
            }
            break;
        }
    }

    public void searchDataReady() {
        if (imService.getContactManager().isUserDataReady() &&
                imService.getGroupManager().isGroupReady()) {
            showSearchFrameLayout();
        }
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        switch (id) {
            case R.id.re_grouplist:
                Intent intent=new Intent(getActivity(),GroupListActivity.class);
                getActivity().startActivity(intent);
                break;
            case R.id.re_newfriends:
                Intent intent2=new Intent(getActivity(),NewFriendListActivity.class);
                getActivity().startActivity(intent2);
                break;
        }

    }
}
