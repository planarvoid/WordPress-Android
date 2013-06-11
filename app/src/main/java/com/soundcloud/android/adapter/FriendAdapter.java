package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.operations.following.FollowStatus;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.view.adapter.IconLayout;
import com.soundcloud.android.view.adapter.UserlistRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class FriendAdapter extends ScBaseAdapter<Friend> implements FollowStatus.Listener {
    public FriendAdapter(Uri uri) {
        super(uri);
        FollowStatus.get().requestUserFollowings(this);
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new UserlistRow(context);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id) {
        context.startActivity(new Intent(context, UserBrowser.class).putExtra(UserBrowser.EXTRA_USER, getItem(position).user));
        return ItemClickResults.LEAVING;
    }

    @Override
    public void onFollowChanged() {
        notifyDataSetChanged();
    }
}

