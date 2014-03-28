package com.soundcloud.android.collections.views;

import com.soundcloud.android.R;
import com.soundcloud.android.collections.ListRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class CommentRow extends IconLayout implements ListRow {
    private final TextView mUser;
    private final TextView mTitle;
    private final TextView mCreatedAt;
    private Comment mComment;

    public CommentRow(Context context, ImageOperations imageOperations) {
        super(context, imageOperations);

        mTitle = (TextView) findViewById(R.id.title);
        mUser = (TextView) findViewById(R.id.user);
        mCreatedAt = (TextView) findViewById(R.id.created_at);
    }

    @Override
    protected View addContent(AttributeSet attributeSet) {
        return View.inflate(getContext(), R.layout.activity_list_row, this);
    }

    @Override
    public void display(Cursor cursor) {
    }

    @Override
    public void display(int position, Parcelable p) {
        mComment = (Comment) p;
        if (mComment == null) return;

        loadIcon();

        mTitle.setText(mComment.body);
        if (mComment.getUser() != null) mUser.setText(mComment.getUser().username);
        mCreatedAt.setText(ScTextUtils.getTimeElapsed(getContext().getResources(), mComment.created_at.getTime()));
    }

    @Override
    public String getResourceUrn() {
        if (mComment != null && mComment.getUser() != null) {
            return mComment.getUser().getUrn();
        }
        return null;
    }

}
