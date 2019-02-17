//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.zhangwuji.im.ui.plugin;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

import com.zhangwuji.im.R;

import java.util.ArrayList;
import java.util.List;

public class PluginAdapter {
    private static final String TAG = "PluginAdapter";
    private LinearLayout mIndicator;
    private int currentPage = 0;
    private LayoutInflater mLayoutInflater;
    private ViewGroup mPluginPager;
    private List<IPluginModule> mPluginModules = new ArrayList();
    private boolean mInitialized;
    private IPluginClickListener mPluginClickListener;
    private View mCustomPager;
    private ViewPager mViewPager;
    private PluginAdapter.PluginPagerAdapter mPagerAdapter;
    private int mPluginCountPerPage=8;

    public PluginAdapter() {
    }

    public void setOnPluginClickListener(IPluginClickListener clickListener) {
        this.mPluginClickListener = clickListener;
    }

    public boolean isInitialized() {
        return this.mInitialized;
    }

    public int getPluginPosition(IPluginModule pluginModule) {
        return this.mPluginModules.indexOf(pluginModule);
    }

    public IPluginModule getPluginModule(int position) {
        return position >= 0 && position < this.mPluginModules.size() ? (IPluginModule)this.mPluginModules.get(position) : null;
    }

    public List<IPluginModule> getPluginModules() {
        return this.mPluginModules;
    }

    public void addPlugins(List<IPluginModule> plugins) {
        for(int i = 0; plugins != null && i < plugins.size(); ++i) {
            this.mPluginModules.add(plugins.get(i));
        }

    }

    public void addPlugin(IPluginModule pluginModule) {
        this.mPluginModules.add(pluginModule);
        int count = this.mPluginModules.size();
        if (this.mPagerAdapter != null && count > 0 && this.mIndicator != null) {
            int rem = count % this.mPluginCountPerPage;
            if (rem > 0) {
                rem = 1;
            }

            int pages = count / this.mPluginCountPerPage + rem;
            this.mPagerAdapter.setPages(pages);
            this.mPagerAdapter.setItems(count);
            this.mPagerAdapter.notifyDataSetChanged();
            this.initIndicator(pages, this.mIndicator);
        }

    }

    public void removePlugin(IPluginModule pluginModule) {
        this.mPluginModules.remove(pluginModule);
        if (this.mPagerAdapter != null && this.mViewPager != null) {
            int count = this.mPluginModules.size();
            if (count > 0) {
                int rem = count % this.mPluginCountPerPage;
                if (rem > 0) {
                    rem = 1;
                }

                int pages = count / this.mPluginCountPerPage + rem;
                this.mPagerAdapter.setPages(pages);
                this.mPagerAdapter.setItems(count);
                this.mPagerAdapter.notifyDataSetChanged();
                this.removeIndicator(pages, this.mIndicator);
            }
        }

    }

    public void bindView(ViewGroup viewGroup) {
        this.mInitialized = true;
        this.initView(viewGroup.getContext(), viewGroup);
    }

    private void initView(Context context, ViewGroup viewGroup) {
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mPluginPager = (ViewGroup)this.mLayoutInflater.inflate(R.layout.rc_ext_plugin_pager, (ViewGroup)null);
        Integer height = (int)context.getResources().getDimension(R.dimen.rc_extension_board_height);
        this.mPluginPager.setLayoutParams(new LayoutParams(-1, height));
        viewGroup.addView(this.mPluginPager);
        this.mViewPager = (ViewPager)this.mPluginPager.findViewById(R.id.rc_view_pager);
        this.mIndicator = (LinearLayout)this.mPluginPager.findViewById(R.id.rc_indicator);
        this.mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                PluginAdapter.this.onIndicatorChanged(PluginAdapter.this.currentPage, position);
                PluginAdapter.this.currentPage = position;
            }

            public void onPageScrollStateChanged(int state) {
            }
        });
        int pages = 0;
        int count = this.mPluginModules.size();
        if (count > 0) {
            int rem = count % this.mPluginCountPerPage;
            if (rem > 0) {
                rem = 1;
            }

            pages = count / this.mPluginCountPerPage + rem;
        }

        this.mPagerAdapter = new PluginAdapter.PluginPagerAdapter(pages, count);
        this.mViewPager.setAdapter(this.mPagerAdapter);
        this.mViewPager.setOffscreenPageLimit(1);
        this.initIndicator(pages, this.mIndicator);
        this.onIndicatorChanged(-1, 0);
    }

    public void setVisibility(int visibility) {
        if (this.mPluginPager != null) {
            this.mPluginPager.setVisibility(visibility);
            if (this.mCustomPager != null) {
                this.mCustomPager.setVisibility(View.GONE);
            }
        }

    }

    public int getVisibility() {
        return this.mPluginPager != null ? this.mPluginPager.getVisibility() : 8;
    }

    private void initIndicator(int pages, LinearLayout indicator) {
        for(int i = 0; i < pages; ++i) {
            ImageView imageView = (ImageView)this.mLayoutInflater.inflate(R.layout.rc_ext_indicator, (ViewGroup)null);
            imageView.setImageResource(R.drawable.rc_ext_indicator);
            indicator.addView(imageView);
            if (pages <= 1) {
                indicator.setVisibility(View.INVISIBLE);
            } else {
                indicator.setVisibility(View.VISIBLE);
            }
        }

    }

    private void removeIndicator(int totalPages, LinearLayout indicator) {
        int index = indicator.getChildCount();
        if (index > totalPages && index - 1 >= 0) {
            indicator.removeViewAt(index - 1);
            this.onIndicatorChanged(index, index - 1);
            if (totalPages <= 1) {
                indicator.setVisibility(View.INVISIBLE);
            }
        }

    }

    private void onIndicatorChanged(int pre, int cur) {
        int count = this.mIndicator.getChildCount();
        if (count > 0 && pre < count && cur < count) {
            ImageView curView;
            if (pre >= 0) {
                curView = (ImageView)this.mIndicator.getChildAt(pre);
                curView.setImageResource(R.drawable.rc_ext_indicator);
            }

            if (cur >= 0) {
                curView = (ImageView)this.mIndicator.getChildAt(cur);
                curView.setImageResource(R.drawable.rc_ext_indicator_hover);
            }
        }

    }

    public void addPager(View v) {
        this.mCustomPager = v;
        LayoutParams params = new LayoutParams(-1, -1);
        params.addRule(13, -1);
        this.mPluginPager.addView(v, params);
    }

    public View getPager() {
        return this.mCustomPager;
    }

    public void removePager(View view) {
        if (this.mCustomPager != null && this.mCustomPager == view) {
            this.mPluginPager.removeView(view);
            this.mCustomPager = null;
        }

    }

    private class PluginItemAdapter extends BaseAdapter {
        int count;
        int index;

        public PluginItemAdapter(int index, int count) {
            this.count = Math.min(PluginAdapter.this.mPluginCountPerPage, count - index);
            this.index = index;
        }

        public int getCount() {
            return this.count;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0L;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            Context context = parent.getContext();
            PluginAdapter.PluginItemAdapter.ViewHolder holder;
            if (convertView == null) {
                holder = new PluginAdapter.PluginItemAdapter.ViewHolder();
                convertView = PluginAdapter.this.mLayoutInflater.inflate(R.layout.rc_ext_plugin_item, (ViewGroup)null);
                holder.imageView = (ImageView)convertView.findViewById(R.id.rc_ext_plugin_icon);
                holder.textView = (TextView)convertView.findViewById(R.id.rc_ext_plugin_title);
                convertView.setTag(holder);
            }

            convertView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    IPluginModule plugin = (IPluginModule)PluginAdapter.this.mPluginModules.get(PluginAdapter.this.currentPage * PluginAdapter.this.mPluginCountPerPage + position);
                    PluginAdapter.this.mPluginClickListener.onClick(plugin, PluginAdapter.this.currentPage * PluginAdapter.this.mPluginCountPerPage + position);
                }
            });
            holder = (PluginAdapter.PluginItemAdapter.ViewHolder)convertView.getTag();
            IPluginModule plugin = (IPluginModule)PluginAdapter.this.mPluginModules.get(position + this.index);
            holder.imageView.setImageDrawable(plugin.obtainDrawable(context));
            holder.textView.setText(plugin.obtainTitle(context));
            return convertView;
        }

        class ViewHolder {
            ImageView imageView;
            TextView textView;

            ViewHolder() {
            }
        }
    }

    private class PluginPagerAdapter extends PagerAdapter {
        int pages;
        int items;

        public PluginPagerAdapter(int pages, int items) {
            this.pages = pages;
            this.items = items;
        }

        public Object instantiateItem(ViewGroup container, int position) {
            GridView gridView = (GridView)PluginAdapter.this.mLayoutInflater.inflate(R.layout.rc_ext_plugin_grid_view, (ViewGroup)null);
            gridView.setAdapter(PluginAdapter.this.new PluginItemAdapter(position * PluginAdapter.this.mPluginCountPerPage, this.items));
            container.addView(gridView);
            return gridView;
        }

        public int getItemPosition(Object object) {
            return -2;
        }

        public int getCount() {
            return this.pages;
        }

        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public void destroyItem(ViewGroup container, int position, Object object) {
            View layout = (View)object;
            container.removeView(layout);
        }

        public void setPages(int value) {
            this.pages = value;
        }

        public void setItems(int value) {
            this.items = value;
        }
    }
}
