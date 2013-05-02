package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.adapter.IconLayout;
import com.soundcloud.android.view.adapter.UserlistRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class UserAdapter extends ScBaseAdapter<User> implements FollowStatus.Listener {
    public UserAdapter(Uri uri) {
        super(uri);
        FollowStatus.get().requestUserFollowings(this);
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new UserlistRow(context);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id) {
        context.startActivity(new Intent(context, UserBrowser.class).putExtra(UserBrowser.EXTRA_USER,getItem(position)));
        return ItemClickResults.LEAVING;
    }

    @Override
    public void onFollowChanged(boolean success) {
        notifyDataSetChanged();
    }
}
