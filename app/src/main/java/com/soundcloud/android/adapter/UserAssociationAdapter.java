package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.view.adapter.IconLayout;
import com.soundcloud.android.view.adapter.UserlistRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class UserAssociationAdapter extends ScBaseAdapter<UserAssociation> implements FollowingOperations.FollowStatusChangedListener {
    public UserAssociationAdapter(Uri uri) {
        super(uri);
        new FollowingOperations().requestUserFollowings(this);
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new UserlistRow(context);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id) {
        context.startActivity(new Intent(context, UserBrowser.class).putExtra(UserBrowser.EXTRA_USER, getItem(position).getUser()));
        return ItemClickResults.LEAVING;
    }

    @Override
    public void onFollowChanged() {
        notifyDataSetChanged();
    }
}