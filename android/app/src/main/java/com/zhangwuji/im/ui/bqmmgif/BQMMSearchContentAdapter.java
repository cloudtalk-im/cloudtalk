package com.zhangwuji.im.ui.bqmmgif;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.melink.baseframe.utils.DensityUtils;
import com.melink.bqmmsdk.bean.BQMMGif;
import com.melink.bqmmsdk.widget.BQMMMessageText;

import java.util.List;

/**
 * Created by syh on 07/12/2017.
 */

public class BQMMSearchContentAdapter extends RecyclerView.Adapter<BQMMSearchContentAdapter.ViewHolder> {
    private List<BQMMGif> mBQMMGifList;
    private OnSearchContentClickListener mSearchContentClickListener;
    private LinearLayout mLayout;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mLayout = new LinearLayout(parent.getContext());
        mLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mLayout.setBackgroundColor(Color.WHITE);
        mLayout.setPadding(DensityUtils.dip2px(10), DensityUtils.dip2px(7.5f), DensityUtils.dip2px(10), DensityUtils.dip2px(7.5f));
        BQMMMessageText bqmmMessageText = new BQMMMessageText(parent.getContext());
        bqmmMessageText.setClickable(false);
        mLayout.addView(bqmmMessageText);
        ViewHolder viewHolder = new ViewHolder(mLayout, bqmmMessageText);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final BQMMGif item = mBQMMGifList.get(position);
        int pixels = DensityUtils.dip2px(60);
        if (!TextUtils.isEmpty(item.getGif_thumb())) {
            holder.bqmmMessageTextView.showBQMMGif(item.getSticker_id(), item.getGif_thumb(), pixels, pixels, item.getIs_gif());
        } else if (!TextUtils.isEmpty(item.getThumb())) {
            holder.bqmmMessageTextView.showBQMMGif(item.getSticker_id(), item.getThumb(), pixels, pixels, 0);
        } else {
            holder.bqmmMessageTextView.showBQMMGif(item.getSticker_id(), item.getSticker_url(), pixels, pixels, item.getIs_gif());
        }
        holder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSearchContentClickListener != null) {
                    mSearchContentClickListener.onSearchContentClick(item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mBQMMGifList.size();
    }

    public void setSearchContentClickListener(OnSearchContentClickListener listener) {
        this.mSearchContentClickListener = listener;
    }

    public void setMMWebStickerList(List<BQMMGif> bqmmGifList) {
        this.mBQMMGifList = bqmmGifList;
        notifyDataSetChanged();
    }

    public void addMMWebStickerList(List<BQMMGif> bqmmGifList) {
        mBQMMGifList.addAll(bqmmGifList);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout;
        BQMMMessageText bqmmMessageTextView;

        public ViewHolder(LinearLayout view, BQMMMessageText text) {
            super(view);
            linearLayout = view;
            bqmmMessageTextView = text;
        }
    }

    interface OnSearchContentClickListener {
        void onSearchContentClick(BQMMGif webSticker);
    }
}
