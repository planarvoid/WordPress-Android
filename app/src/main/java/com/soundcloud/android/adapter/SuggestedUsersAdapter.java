package com.soundcloud.android.adapter;

import com.soundcloud.android.R;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.SuggestedUser;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class SuggestedUsersAdapter extends BaseAdapter {

    private final List<SuggestedUser> mSuggestedUsers;

    public SuggestedUsersAdapter(List<SuggestedUser> suggestedUsers) {
        mSuggestedUsers = suggestedUsers;
    }

    @Override
    public int getCount() {
        return mSuggestedUsers.size();
    }

    @Override
    public SuggestedUser getItem(int position) {
        return mSuggestedUsers.get(0);
    }

    @Override
    public long getItemId(int position) {
        return mSuggestedUsers.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemViewHolder viewHolder;
        if (convertView == null) {
            convertView = View.inflate(parent.getContext(), R.layout.suggested_user_grid_item,null);
            viewHolder = new ItemViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.suggested_user_image);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ItemViewHolder) convertView.getTag();
        }
        final SuggestedUser suggestedUser = getItem(position);
        ImageLoader.get(parent.getContext()).bind(this, viewHolder.imageView, suggestedUser.getAvatarUrl());
        return convertView;
    }

    private static class ItemViewHolder {
        public TextView username, city, country;
        public ImageView imageView;
    }
}
