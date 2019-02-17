package com.zhangwuji.im.ui.bqmmgif;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import com.melink.baseframe.utils.DensityUtils;
import com.melink.bqmmsdk.bean.BQMMGif;
import com.melink.bqmmsdk.sdk.BQMM;
import com.melink.bqmmsdk.sdk.IBQMMGifCallback;
import com.melink.bqmmsdk.sdk.IBQMMGifManager;

import java.util.List;

/**
 * Created by syh on 07/12/2017.
 */

public class BQMMGifManager implements IBQMMGifManager {
    public final static int LOADSIZE = 20;
    public final static int LOAD_DATA = 101, DISMISS_POPUP = 102;

    private static BQMMGifManager mInstance;
    private boolean mSearchEnabled = false;
    private boolean mUIVisible = false;
    private boolean mShowingTrending = true;
    private String mSearchKeyword = null;
    private BQMMSearchPopupWindow mPopupWindow;
    private int mCurrentTaskId = 0;
    private int mCurrentPage = 0;
    private boolean mNeedLoadMore = true;
    private boolean mIsLoading = false;
    private IBqmmSendGifListener mIBqmmSendGifListener;

    public final static int BQMM_SEARCH_MODE_STATUS_KEYBOARD_HIDE = 10001;//收起软键盘
    public final static int BQMM_SEARCH_MODE_STATUS_INPUT_TEXT_CHANGE = 10002;//输入框变化
    public final static int BQMM_SEARCH_MODE_STATUS_INPUT_BECOME_EMPTY = 10003;//清空输入框
    public final static int BQMM_SEARCH_MODE_STATUS_GIF_MESSAGE_SENT = 10004;//发送了gif消息
    public final static int BQMM_SEARCH_MODE_STATUS_SHOWTRENDING_TRIGGERED = 10005;//触发了流行表情
    public final static int BQMM_SEARCH_MODE_STATUS_GIFS_DATA_RECEIVED_WITH_RESULT = 10006;//搜索收到gif数据
    public final static int BQMM_SEARCH_MODE_STATUS_GIFS_DATA_RECEIVED_WITH_EMPTY_RESULT = 10007;//搜索结果为空

    private Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_DATA:
                    if (msg.arg2 != mCurrentTaskId) return;
                    List<BQMMGif> stickers = (List<BQMMGif>) msg.obj;
                    if (mCurrentPage == 0) {
                        mPopupWindow.show(stickers);
                    } else {
                        mPopupWindow.showMore(stickers);
                    }
                    ++mCurrentPage;
                    mIsLoading = false;
                    break;
                case DISMISS_POPUP:
                    mPopupWindow.dismiss();
                    break;
            }
        }
    };

    public static BQMMGifManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BQMMGifManager(context);
        }
        return mInstance;
    }

    public BQMMGifManager(Context context) {
        mPopupWindow = new BQMMSearchPopupWindow(context, DensityUtils.dip2px(95));
        mPopupWindow.setLoadMoreListener(new BQMMSearchPopupWindow.LoadMoreListener() {
            @Override
            public void loadMore() {
                if (mIsLoading || !mNeedLoadMore || mCurrentPage > 5) return;
                if (mShowingTrending) {
                    getTrendingData();
                } else {
                    getSearchData();
                }
            }
        });
        mPopupWindow.getAdapter().setSearchContentClickListener(new BQMMSearchContentAdapter.OnSearchContentClickListener() {
            @Override
            public void onSearchContentClick(BQMMGif bqmmGif) {
                mIBqmmSendGifListener.onSendBQMMGif(bqmmGif);
                // 清空输入框中文字
                BQMM.getInstance().getEditView().setText("");
                // 通知关闭popwindow
                updateSearchModeAndSearchUIWithStatus(BQMM_SEARCH_MODE_STATUS_GIF_MESSAGE_SENT);
            }
        });
    }

    /**
     * 在输入框实例化后调用，两个监听事件分别是文字清空和输入框距离底部小于40即（键盘关闭的时候），隐藏弹窗。
     */
    public void addEditViewListeners() {
        BQMM.getInstance().getEditView().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSearchModeAndSearchUIWithStatus(BQMM_SEARCH_MODE_STATUS_INPUT_TEXT_CHANGE);
                if (s.length() > 1) {
                    setSearchUIVisible(true);
                    showSearchContent(s.toString());
                } else {
                    updateSearchModeAndSearchUIWithStatus(BQMM_SEARCH_MODE_STATUS_INPUT_BECOME_EMPTY);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        BQMM.getInstance().getEditView().getRootView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (BQMM.getInstance().getEditView() != null) {
                    int[] position = new int[2];
                    BQMM.getInstance().getEditView().getLocationOnScreen(position);
                    int h = position[1];
                    int i = DensityUtils.getScreenH() - h - BQMM.getInstance().getEditView().getMeasuredHeight();
                    if (i < 40) {
                        updateSearchModeAndSearchUIWithStatus(BQMM_SEARCH_MODE_STATUS_KEYBOARD_HIDE);
                    }
                }
            }
        });
    }

    public void updateSearchModeAndSearchUIWithStatus(int status) {
        switch (status) {
            case BQMM_SEARCH_MODE_STATUS_KEYBOARD_HIDE://收起软键盘
                setSearchUIVisible(false);
                break;
            case BQMM_SEARCH_MODE_STATUS_INPUT_TEXT_CHANGE://输入框变化
                break;
            case BQMM_SEARCH_MODE_STATUS_INPUT_BECOME_EMPTY://清空输入框
                setSearchUIVisible(false);
                //setSearchModeEnabled(false);
                break;
            case BQMM_SEARCH_MODE_STATUS_GIF_MESSAGE_SENT://发送了gif消息
                setSearchUIVisible(false);
                //setSearchModeEnabled(false);
                break;
            case BQMM_SEARCH_MODE_STATUS_SHOWTRENDING_TRIGGERED://触发了流行表情
                setSearchUIVisible(true);
                setSearchModeEnabled(true);
                break;
            case BQMM_SEARCH_MODE_STATUS_GIFS_DATA_RECEIVED_WITH_RESULT://搜索到gif数据
                break;
            case BQMM_SEARCH_MODE_STATUS_GIFS_DATA_RECEIVED_WITH_EMPTY_RESULT://搜索结果为空
                break;
            default:
                break;
        }
    }

    @Override
    public IBQMMGifManager setSearchModeEnabled(boolean enabled) {
        mSearchEnabled = enabled;
        if (!enabled && mPopupWindow.isShowing()) {
            mainHandler.sendEmptyMessage(DISMISS_POPUP);
        }
        return this;
    }

    @Override
    public IBQMMGifManager setSearchUIVisible(boolean visible) {
        mUIVisible = visible;
        if (!visible && mPopupWindow.isShowing()) {
            mainHandler.sendEmptyMessage(DISMISS_POPUP);
        }
        return this;
    }

    @Override
    public void showTrending() {
        updateSearchModeAndSearchUIWithStatus(BQMM_SEARCH_MODE_STATUS_SHOWTRENDING_TRIGGERED);
        if (mSearchEnabled && mUIVisible) {
            ++mCurrentTaskId;
            mCurrentPage = 0;
            mPopupWindow.setParentView(BQMM.getInstance().getEditView());
            mShowingTrending = true;
            getTrendingData();
        }
    }

    @Override
    public void showSearchContent(String keyword) {
        if (mSearchEnabled && mUIVisible) {
            ++mCurrentTaskId;
            mCurrentPage = 0;
            mPopupWindow.setParentView(BQMM.getInstance().getEditView());
            mShowingTrending = false;
            mSearchKeyword = keyword;
            if (!TextUtils.isEmpty(keyword)) {
                getSearchData();
            }
        }
    }

    private void getTrendingData() {
        mIsLoading = true;
        final int currentTaskId = mCurrentTaskId;
        final int currentPage = mCurrentPage + 1;
        BQMM.trendingGifsAt(currentPage, LOADSIZE, new IBQMMGifCallback<BQMMGif>() {
            @Override
            public void onSuccess(List<BQMMGif> result) {
                Message message = Message.obtain();
                message.what = LOAD_DATA;
                message.arg1 = currentPage;
                message.obj = result;
                mainHandler.sendMessage(message);
                message.arg2 = currentTaskId;
                mNeedLoadMore = result.size() >= LOADSIZE;
            }

            @Override
            public void onError(String errorInfo) {
            }
        });
    }

    private void getSearchData() {
        mIsLoading = true;
        final int currentTaskId = mCurrentTaskId;
        final int currentPage = mCurrentPage + 1;
        BQMM.searchGifsWithKey(mSearchKeyword, currentPage, LOADSIZE, new IBQMMGifCallback<BQMMGif>() {
            @Override
            public void onSuccess(List<BQMMGif> result) {
                Message message = Message.obtain();
                message.what = LOAD_DATA;
                message.arg1 = currentPage;
                message.obj = result;
                mainHandler.sendMessage(message);
                message.arg2 = currentTaskId;
                mNeedLoadMore = result.size() >= LOADSIZE;
            }

            @Override
            public void onError(String errorInfo) {
            }
        });
    }

    public void setBQMMSendGifListener(IBqmmSendGifListener iBqmmSendGifListener) {
        this.mIBqmmSendGifListener = iBqmmSendGifListener;
    }
}
