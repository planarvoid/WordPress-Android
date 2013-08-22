package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.view.adapter.CommentRow;
import com.soundcloud.android.view.adapter.IconLayout;

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
        context.startActivity(new Intent(context, UserBrowser.class).putExtra(UserBrowser.EXTRA_USER,getItem(position).user));
        return ItemClickResults.LEAVING;
    }
}
