package com.zhangwuji.im.ui.bqmmgif;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.melink.baseframe.utils.DensityUtils;
import com.melink.bqmmsdk.bean.BQMMGif;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by syh on 07/12/2017.
 */

public class BQMMSearchPopupWindow extends PopupWindow {
    private Context mContext;
    private RecyclerView mRecyclerView;
    private BQMMSearchContentAdapter mAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private int totalItemCount;
    private int lastVisiableItemPosition;
    private LoadMoreListener mLoadMoreListener;
    private int[] mParentLocation = new int[]{0, 0};
    private WeakReference<View> mParentViewWeakReference;
    private LinearLayout mLinearLayout;
    public BQMMSearchPopupWindow(Context context, int height) {
        super();
        mContext = context;
        mRecyclerView = new RecyclerView(context);
        mLinearLayoutManager = new LinearLayoutManager(mContext);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mLinearLayout = new LinearLayout(mContext);
        mLinearLayout.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mLinearLayout.setLayoutParams(layoutParams);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mAdapter = new BQMMSearchContentAdapter();
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = mLinearLayoutManager.getItemCount();
                lastVisiableItemPosition = mLinearLayoutManager.findLastVisibleItemPosition();
                if (totalItemCount <= (lastVisiableItemPosition + 2)) {
                    if (mLoadMoreListener != null) {
                        mLoadMoreListener.loadMore();
                    }
                }
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        mLinearLayout.addView(mRecyclerView);
        setContentView(mLinearLayout);
        setHeight(height);
        setFocusable(false);
    }

    public void setParentView(View parent) {
        parent.getLocationOnScreen(mParentLocation);
        mParentViewWeakReference = new WeakReference<>(parent);
    }

    public void show(final List<BQMMGif> stickers) {
        mRecyclerView.scrollToPosition(0);
        if (mParentViewWeakReference != null) {
            final View parent = mParentViewWeakReference.get();
            if (parent != null) {
                if (isShowing()) dismiss();
                mAdapter.setMMWebStickerList(stickers);
                int screenWidth = DensityUtils.getScreenW();
                setWidth(screenWidth);
                showAtLocation(parent, Gravity.NO_GRAVITY, 0, mParentLocation[1] - getHeight()-20);
            }
        }
    }

    public void showMore(final List<BQMMGif> stickers) {
        mAdapter.addMMWebStickerList(stickers);
    }

    public void setLoadMoreListener(LoadMoreListener mLoadMoreListener) {
        this.mLoadMoreListener = mLoadMoreListener;
    }

    public BQMMSearchContentAdapter getAdapter() {
        return mAdapter;
    }

    interface LoadMoreListener {
        void loadMore();
    }
}
