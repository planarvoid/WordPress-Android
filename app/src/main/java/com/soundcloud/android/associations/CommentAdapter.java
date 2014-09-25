package com.soundcloud.android.associations;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.collections.views.CommentRow;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.profile.ProfileActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;

public class CommentAdapter extends ScBaseAdapter<PublicApiComment> {

    private final ImageOperations imageOperations;

    public CommentAdapter(Uri uri, ImageOperations imageOperations) {
        super(uri);
        this.imageOperations = imageOperations;
    }

    @Override
    protected IconLayout createRow(Context context, int position, ViewGroup parent) {
        return new CommentRow(context, imageOperations);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        context.startActivity(new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER,getItem(position).user));
        return ItemClickResults.LEAVING;
    }
}
