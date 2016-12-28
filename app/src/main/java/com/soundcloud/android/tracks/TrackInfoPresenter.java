package com.soundcloud.android.tracks;

import static android.text.Html.fromHtml;
import static java.lang.System.getProperty;

import com.soundcloud.android.R;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;

import android.content.res.Resources;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Locale;

public class TrackInfoPresenter {

    private final Resources resources;
    private final CondensedNumberFormatter numberFormatter;

    interface CommentClickListener {
        void onCommentsClicked();
    }

    @Inject
    public TrackInfoPresenter(Resources resources, CondensedNumberFormatter numberFormatter) {
        this.resources = resources;
        this.numberFormatter = numberFormatter;
    }

    public View create(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.track_info, container, false);
    }

    public void bind(View view, final PropertySet propertySet, CommentClickListener commentClickListener) {
        bind(view, TrackItem.from(propertySet), commentClickListener);
    }
    public void bind(View view, final TrackItem trackItem, CommentClickListener commentClickListener) {
        setTextAndShow(view, R.id.track_info_title, trackItem.getTitle());
        setTextAndShow(view, R.id.creator, trackItem.getCreatorName());

        showView(view, R.id.description_holder);

        bindUploadedSinceText(view, trackItem);
        bindComments(view, trackItem, commentClickListener);
        bindPrivateOrStats(view, trackItem);
    }

    public void showSpinner(View view) {
        hideView(view, R.id.description);
        showView(view, R.id.loading);
    }

    public void bindDescription(View view, PropertySet propertySet) {
        bindDescription(view, TrackItem.from(propertySet));
    }

    public void bindDescription(View view, TrackItem trackItem) {
        final String source = trackItem.getDescription();
        if (source.isEmpty()) {
            bindNoDescription(view);
        } else {
            hideView(view, R.id.no_description);
            setTextAndShow(view, R.id.description, fromHtml(source.replace(getProperty("line.separator"), "<br/>")));
        }
        hideView(view, R.id.loading);
    }

    public void bindNoDescription(View view) {
        showView(view, R.id.no_description);
        hideView(view, R.id.loading);
        hideView(view, R.id.description);
    }

    private void bindPrivateOrStats(View view, TrackItem trackItem) {
        if (trackItem.isPrivate()) {
            hideView(view, R.id.stats_holder);
            showView(view, R.id.private_indicator);
        } else {
            showView(view, R.id.stats_holder);
            hideView(view, R.id.private_indicator);
            configureStats(view, trackItem);
        }
    }

    private void bindComments(View view, final TrackItem track, final CommentClickListener commentClickListener) {
        final boolean isCommentable = track.isCommentable();
        final int commentsCount = track.getCommentsCount();
        if (isCommentable && commentsCount > 0) {
            String comments = resources.getQuantityString(R.plurals.trackinfo_comments, commentsCount, commentsCount);
            setTextAndShow(view, R.id.comments, comments);
            showView(view, R.id.comments_divider);
        } else {
            hideView(view, R.id.comments);
            hideView(view, R.id.comments_divider);
        }

        view.findViewById(R.id.comments).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commentClickListener.onCommentsClicked();
            }
        });
    }

    private void bindUploadedSinceText(View view, TrackItem trackItem) {
        long createdAt = trackItem.getCreatedAt().getTime();
        final String timeElapsed = ScTextUtils
                .formatTimeElapsedSince(resources, createdAt, true)
                .toLowerCase(Locale.getDefault());
        setTextAndShow(view, R.id.uploaded_at, resources.getString(R.string.uploaded_xtimeago, timeElapsed));
    }

    private void configureStats(View view, TrackItem trackItem) {
        setStat(view, R.id.plays, trackItem.getPlayCount());
        setStat(view, R.id.likes, trackItem.getLikesCount());
        setStat(view, R.id.reposts, trackItem.getRepostCount());

        toggleDividers(view);
    }

    private void toggleDividers(View view) {
        boolean showPlays = view.findViewById(R.id.plays).getVisibility() == View.VISIBLE;
        boolean showLikes = view.findViewById(R.id.likes).getVisibility() == View.VISIBLE;
        boolean showReposts = view.findViewById(R.id.reposts).getVisibility() == View.VISIBLE;

        final boolean showDivider1 = (showPlays && showLikes) || (showPlays && showReposts);
        view.findViewById(R.id.divider1).setVisibility(showDivider1 ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.divider2).setVisibility((showLikes && showReposts) ? View.VISIBLE : View.GONE);
    }

    private void setStat(View view, int id, Integer count) {
        if (count > 0) {
            setTextAndShow(view, id, numberFormatter.format(count));
        } else {
            view.findViewById(id).setVisibility(View.GONE);
        }
    }

    private void setTextAndShow(View view, int id, String text) {
        final TextView textView = ((TextView) view.findViewById(id));
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
    }

    private void setTextAndShow(View view, int id, Spanned text) {
        final TextView textView = ((TextView) view.findViewById(id));
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
    }

    private void showView(View view, int viewId) {
        view.findViewById(viewId).setVisibility(View.VISIBLE);
    }

    private void hideView(View view, int viewId) {
        view.findViewById(viewId).setVisibility(View.GONE);
    }

}
