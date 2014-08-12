package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.associations.PlayableInteractionActivity;
import com.soundcloud.android.associations.TrackInteractionActivity;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackInfoPresenter {
    private final Resources resources;

    @Inject
    public TrackInfoPresenter(Resources resources) {
        this.resources = resources;
    }

    public View create(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.track_details_view, container, false);
    }

    public void bind(View view, final PropertySet propertySet) {
        setTextAndShow(view, R.id.title, propertySet.get(PlayableProperty.TITLE));
        setTextAndShow(view, R.id.creator, propertySet.get(PlayableProperty.CREATOR_NAME));

        showView(view, R.id.description_divider);
        showView(view, R.id.description_holder);

        bindUploadedSinceText(view, propertySet);
        bindComments(view, propertySet);
        bindPrivateOrStats(view, propertySet);
    }

    public void showSpinner(View view) {
        hideView(view, R.id.description);
        showView(view, R.id.loading);
    }

    public void bindDescription(View view, PropertySet propertySet) {
        final String source = propertySet.get(TrackProperty.DESCRIPTION);
        if (source.isEmpty()) {
            bindNoDescription(view);
        } else {
            setTextAndShow(view, R.id.description, Html.fromHtml(source.replace(System.getProperty("line.separator"), "<br/>")));
        }
        hideView(view, R.id.loading);
    }

    public void bindNoDescription(View view) {
        showView(view, R.id.no_description);
        hideView(view, R.id.loading);
    }

    private void bindPrivateOrStats(View view, PropertySet propertySet) {
        if (propertySet.get(PlayableProperty.IS_PRIVATE)){
            hideView(view, R.id.stats_holder);
            showView(view, R.id.private_indicator);
        } else {
            showView(view, R.id.stats_holder);
            hideView(view, R.id.private_indicator);
            configureStats(view, propertySet);
        }
    }

    private void bindComments(View view, final PropertySet propertySet) {
        int commentsCount = propertySet.get(TrackProperty.COMMENTS_COUNT);
        if (commentsCount > 0){
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
                final Context context = view.getContext();
                context.startActivity(new Intent(context, TrackInteractionActivity.class)
                        .putExtra(PlayableInteractionActivity.PROPERTY_SET_EXTRA, propertySet)
                        .putExtra(PlayableInteractionActivity.EXTRA_INTERACTION_TYPE, Activity.Type.COMMENT));
            }
        });
    }

    private void bindUploadedSinceText(View view, PropertySet propertySet) {
        final String timeElapsed = ScTextUtils.formatTimeElapsedSince(resources, propertySet.get(PlayableProperty.CREATED_AT).getTime(), true);
        setTextAndShow(view, R.id.uploaded_at, resources.getString(R.string.uploaded_at, timeElapsed));
    }

    private void configureStats(View view, PropertySet propertySet) {
        setStat(view, R.id.plays, propertySet.get(TrackProperty.PLAY_COUNT));
        setStat(view, R.id.likes, propertySet.get(PlayableProperty.LIKES_COUNT));
        setStat(view, R.id.reposts, propertySet.get(PlayableProperty.REPOSTS_COUNT));

        toggleDividers(view);
    }

    private void toggleDividers(View view) {
        boolean showPlays = view.findViewById(R.id.plays).getVisibility() == View.VISIBLE;
        boolean showLikes = view.findViewById(R.id.likes).getVisibility() == View.VISIBLE;
        boolean showReposts = view.findViewById(R.id.reposts).getVisibility() == View.VISIBLE;

        final boolean showDivider1 = (showPlays && showLikes) || (showPlays && showReposts);
        view.findViewById(R.id.divider1).setVisibility(showDivider1 ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.divider2).setVisibility((showLikes && showReposts)? View.VISIBLE : View.GONE);
    }

    private void setStat(View view, int id, Integer count) {
        if (count > 0) {
            setTextAndShow(view, id, String.valueOf(count));
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
