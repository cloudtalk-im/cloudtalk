package com.zhangwuji.im.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.zhangwuji.im.DB.entity.User;
import com.zhangwuji.im.R;
import com.zhangwuji.im.imcore.service.IMService;
import com.zhangwuji.im.ui.activity.GroupListActivity;
import com.zhangwuji.im.ui.entity.UserRelationship;
import com.zhangwuji.im.ui.helper.IMUIHelper;
import com.zhangwuji.im.ui.widget.IMBaseImageView;
import com.zhangwuji.im.ui.widget.SelectableRoundedImageView;
import com.zhangwuji.im.utils.AvatarGenerate;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class FriendListAdapter extends BaseAdapter {

    private Context ctx;
    public List<UserRelationship> userList = new ArrayList<>();


    public FriendListAdapter(Context context,List<UserRelationship>list){
        this.ctx = context;
        this.userList=list;
    }

    @Override
    public int getCount() {
        return userList.size();
    }

    @Override
    public Object getItem(int i) {
        return userList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(ctx).inflate(R.layout.rs_ada_user_ship, parent, false);
            holder.mName = (TextView) convertView.findViewById(R.id.ship_name);
            holder.mMessage = (TextView) convertView.findViewById(R.id.ship_message);
            holder.mHead = (IMBaseImageView) convertView.findViewById(R.id.new_header);
            holder.mState = (TextView) convertView.findViewById(R.id.ship_state);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final UserRelationship bean = (UserRelationship)getItem(position);
        holder.mName.setText(bean.getNickname());

        holder.mHead.setCorner(8);
        holder.mHead.setImageUrl(AvatarGenerate.generateAvatar(bean.getAvatar(),bean.getNickname(),bean.getId()+""));


        holder.mMessage.setText(bean.getMessage());
        holder.mMessage.setVisibility(View.VISIBLE);
        holder.mState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    mOnItemButtonClick.onButtonClick(position, v, bean.getStatus());
            }
        });
        
        holder.mName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userList.get(position).getStatus()==1)
                {
                    IMUIHelper.openUserProfileActivity(ctx,Integer.parseInt(userList.get(position).getId()));
                }

            }
        });
        holder.mHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userList.get(position).getStatus()==1)
                {
                    IMUIHelper.openUserProfileActivity(ctx,Integer.parseInt(userList.get(position).getId()));
                }
            }
        });
        
        switch (bean.getStatus()) {
            case 22: //收到了好友邀请
                holder.mState.setText(R.string.agree);
                holder.mState.setBackgroundDrawable(ctx.getResources().getDrawable(R.drawable.de_add_friend_selector));
                break;
            case 21: // 发出了好友邀请
                holder.mState.setText(R.string.request);
                holder.mState.setBackgroundDrawable(null);
                break;
            case 11: // 忽略好友邀请
                holder.mState.setText(R.string.ignore);
                holder.mState.setBackgroundDrawable(null);
                break;
            case 1: // 已是好友
                holder.mState.setText(R.string.added);
                holder.mState.setBackgroundDrawable(null);
                break;
            case 0: // 删除了好友关系
                holder.mState.setText(R.string.deleted);
                holder.mState.setBackgroundDrawable(null);
                break;
        }

        return convertView;
    }

    /**
     * displayName :
     * message : 手机号:18622222222昵称:的用户请求添加你为好友
     * status : 11
     * updatedAt : 2016-01-07T06:22:55.000Z
     * user : {"id":"i3gRfA1ml","nickname":"nihaoa","portraitUri":""}
     */

    class ViewHolder {
        IMBaseImageView mHead;
        TextView mName;
        TextView mState;
        //        TextView mtime;
        TextView mMessage;
    }

    OnItemButtonClick mOnItemButtonClick;


    public void setOnItemButtonClick(OnItemButtonClick onItemButtonClick) {
        this.mOnItemButtonClick = onItemButtonClick;
    }

    public interface OnItemButtonClick {
        boolean onButtonClick(int position, View view, int status);

    }
}
