package com.soundcloud.android.adapter;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.UserlistRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class UserAdapter extends ScBaseAdapter<User> implements FollowStatus.Listener {
    public UserAdapter(Context context, Uri uri) {
        super(context, uri);

        FollowStatus.get().requestUserFollowings(SoundCloudApplication.fromContext(context),this, false);
    }

    @Override
    protected LazyRow createRow(int position) {
        return new UserlistRow(mContext, this);
    }

    @Override
    public void handleListItemClick(int position, long id) {
        mContext.startActivity(new Intent(mContext, UserBrowser.class).putExtra(UserBrowser.EXTRA_USER,getItem(position)));
    }

    @Override
    public void onChange(boolean success, FollowStatus status) {
        notifyDataSetChanged();
    }
}
