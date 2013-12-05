package com.soundcloud.android.collections;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.collections.views.UserlistRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class UserAdapter extends ScBaseAdapter<User> implements FollowingOperations.FollowStatusChangedListener {
    public UserAdapter(Uri uri) {
        super(uri);
        new FollowingOperations().requestUserFollowings(this);
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new UserlistRow(context);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        context.startActivity(new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER,getItem(position)));
        return ItemClickResults.LEAVING;
    }

    @Override
    public void onFollowChanged() {
        notifyDataSetChanged();
    }
}
