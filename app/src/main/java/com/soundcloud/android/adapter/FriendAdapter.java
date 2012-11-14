package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.UserlistRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class FriendAdapter extends ScBaseAdapter<Friend> implements FollowStatus.Listener {
    public FriendAdapter(Context context, Uri uri) {
        super(context, uri);
        FollowStatus.get(context).requestUserFollowings(this);
    }

    @Override
    protected LazyRow createRow(int position) {
        return new UserlistRow(mContext, this);
    }

    @Override
    public int handleListItemClick(int position, long id) {
        mContext.startActivity(new Intent(mContext, UserBrowser.class).putExtra(UserBrowser.EXTRA_USER, getItem(position).user));
        return ItemClickResults.LEAVING;
    }

    @Override
    public void onChange(boolean success, FollowStatus status) {
        notifyDataSetChanged();
    }
}

