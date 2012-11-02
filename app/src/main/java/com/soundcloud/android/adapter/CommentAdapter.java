package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.view.adapter.CommentRow;
import com.soundcloud.android.view.adapter.LazyRow;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class CommentAdapter extends ScBaseAdapter<Comment> {
    public CommentAdapter(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected LazyRow createRow(int position) {
        return new CommentRow(mContext, this);
    }

    @Override
    public int handleListItemClick(int position, long id) {
        mContext.startActivity(new Intent(mContext, UserBrowser.class).putExtra(UserBrowser.EXTRA_USER,getItem(position).user));
        return ItemClickResults.LEAVING;
    }


}
