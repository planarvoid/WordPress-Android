package com.soundcloud.android.collections.views;

import com.soundcloud.android.R;
import com.soundcloud.android.collections.ListRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.api.legacy.model.Comment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class CommentRow extends IconLayout implements ListRow {
    private final TextView user;
    private final TextView title;
    private final TextView createdAt;
    private Comment comment;

    public CommentRow(Context context, ImageOperations imageOperations) {
        super(context, imageOperations);

        title = (TextView) findViewById(R.id.title);
        user = (TextView) findViewById(R.id.user);
        createdAt = (TextView) findViewById(R.id.created_at);
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
        comment = (Comment) p;
        if (comment == null) return;

        loadIcon();

        title.setText(comment.body);
        if (comment.getUser() != null) user.setText(comment.getUser().username);
        createdAt.setText(ScTextUtils.formatTimeElapsed(getContext().getResources(), comment.getCreatedAt().getTime()));
    }

    @Override
    public Urn getResourceUrn() {
        if (comment != null && comment.getUser() != null) {
            return comment.getUser().getUrn();
        }
        return null;
    }

}
