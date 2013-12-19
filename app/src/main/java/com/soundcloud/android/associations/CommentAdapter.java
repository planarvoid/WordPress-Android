package com.soundcloud.android.associations;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.collections.views.CommentRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.collections.views.IconLayout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class CommentAdapter extends ScBaseAdapter<Comment> {

    private ImageOperations mImageOperations;

    public CommentAdapter(Uri uri, ImageOperations imageOperations) {
        super(uri);
        mImageOperations = imageOperations;
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new CommentRow(context, mImageOperations);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        context.startActivity(new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER,getItem(position).user));
        return ItemClickResults.LEAVING;
    }
}
