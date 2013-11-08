package com.soundcloud.android.associations;

import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.collections.views.CommentRow;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.collections.views.IconLayout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class CommentAdapter extends ScBaseAdapter<Comment> {
    public CommentAdapter(Uri uri) {
        super(uri);
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new CommentRow(context);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id) {
        context.startActivity(new Intent(context, ProfileActivity.class).putExtra(ProfileActivity.EXTRA_USER,getItem(position).user));
        return ItemClickResults.LEAVING;
    }
}
