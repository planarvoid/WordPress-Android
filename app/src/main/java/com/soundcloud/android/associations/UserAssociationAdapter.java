package com.soundcloud.android.associations;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.collections.views.UserlistRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class UserAssociationAdapter extends ScBaseAdapter<UserAssociation> implements FollowingOperations.FollowStatusChangedListener {

    private final Screen originScreen;
    private final ImageOperations imageOperations;

    public UserAssociationAdapter(Uri uri, Screen originScreen, ImageOperations imageOperations) {
        super(uri);
        this.originScreen = originScreen;
        this.imageOperations = imageOperations;
        new FollowingOperations().requestUserFollowings(this);
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new UserlistRow(context, originScreen, imageOperations);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        context.startActivity(new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER, getItem(position).getUser()));
        return ItemClickResults.LEAVING;
    }

    @Override
    public void onFollowChanged() {
        notifyDataSetChanged();
    }
}