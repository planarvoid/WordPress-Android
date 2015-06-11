package com.soundcloud.android.comments;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.res.Resources;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommentRenderer implements CellRenderer<Comment> {

    private final Resources resources;
    private final ImageOperations imageOperations;

    @Inject
    public CommentRenderer(Resources resources, ImageOperations imageOperations) {
        this.resources = resources;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.engagement_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<Comment> items) {
        final Comment comment = items.get(position);

        setUserName(itemView, comment);
        textView(itemView, R.id.body).setText(comment.getText());
        setUserAvatar(itemView, comment.getUserUrn());
        setDate(itemView, comment.getDate());
    }

    private void setUserName(View itemView, Comment comment) {
        final TextView userName = textView(itemView, R.id.username);
        userName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        userName.setText(resources.getString(R.string.user_commented_at, comment.getUsername(),
                ScTextUtils.formatTimestamp(comment.getTimeStamp(), TimeUnit.MILLISECONDS)));
    }

    private void setUserAvatar(View itemView, Urn userUrn) {
        imageOperations.displayInAdapterView(
                userUrn,
                ApiImageSize.getListItemImageSize(resources),
                (ImageView) itemView.findViewById(R.id.image));
    }

    private void setDate(View itemView, Date date) {
        final String formattedTime = ScTextUtils.formatTimeElapsedSince(resources, date.getTime(), true);
        ((TextView) itemView.findViewById(R.id.date)).setText(formattedTime);
    }

    private TextView textView(View view, int id) {
        return (TextView) view.findViewById(id);
    }
}
