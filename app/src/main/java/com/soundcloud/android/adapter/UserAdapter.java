package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.UserlistRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class UserAdapter extends ScBaseAdapter<User> implements FollowStatus.Listener {
    public UserAdapter(Context context, Uri uri) {
        super(context, uri);
        FollowStatus.get(context).requestUserFollowings(this);
    }

    @Override
    protected LazyRow createRow(int position) {
        return new UserlistRow(mContext, this);
    }

    @Override
    public int handleListItemClick(int position, long id) {
        mContext.startActivity(new Intent(mContext, UserBrowser.class).putExtra(UserBrowser.EXTRA_USER,getItem(position)));
        return ItemClickResults.LEAVING;
    }

    @Override
    public void onChange(boolean success, FollowStatus status) {
        notifyDataSetChanged();
    }
}
