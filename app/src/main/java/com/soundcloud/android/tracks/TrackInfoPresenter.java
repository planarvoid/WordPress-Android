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
        ((TextView) view.findViewById(R.id.title)).setText(propertySet.get(PlayableProperty.TITLE));
        ((TextView) view.findViewById(R.id.creator)).setText(propertySet.get(PlayableProperty.CREATOR_NAME));

        bindUploadedSinceText(view, propertySet);
        bindComments(view, propertySet);
    }

    private void bindComments(View view, final PropertySet propertySet) {
        int commentsCount = propertySet.get(TrackProperty.COMMENTS_COUNT);
        final TextView commentsView = (TextView) view.findViewById(R.id.comments);
        if (commentsCount > 0){
            String comments = resources.getQuantityString(R.plurals.trackinfo_comments, commentsCount, commentsCount);
            commentsView.setText(comments);
            commentsView.setVisibility(View.VISIBLE);
            view.findViewById(R.id.comments_divider).setVisibility(View.VISIBLE);
        } else {
            commentsView.setVisibility(View.GONE);
            view.findViewById(R.id.comments_divider).setVisibility(View.GONE);
        }
        configureStats(view, propertySet);

        view.findViewById(R.id.comments_holder).setOnClickListener(new View.OnClickListener() {
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
        ((TextView) view.findViewById(R.id.uploaded_at)).setText(resources.getString(R.string.uploaded_at, timeElapsed));
    }

    private void configureStats(View view, PropertySet propertySet) {
        final TextView playsView = (TextView) view.findViewById(R.id.plays);
        final TextView likesView = (TextView) view.findViewById(R.id.likes);
        final TextView repostsView = (TextView) view.findViewById(R.id.reposts);

        setStat(playsView, propertySet.get(TrackProperty.PLAY_COUNT));
        setStat(likesView, propertySet.get(PlayableProperty.LIKES_COUNT));
        setStat(repostsView, propertySet.get(PlayableProperty.REPOSTS_COUNT));

        toggleDividers(view, playsView, likesView, repostsView);
    }

    private void toggleDividers(View view, TextView playsView, TextView likesView, View repostsView) {
        boolean showPlays = playsView.getVisibility() == View.VISIBLE;
        boolean showLikes = likesView.getVisibility() == View.VISIBLE;
        boolean showReposts = repostsView.getVisibility() == View.VISIBLE;

        final boolean showDivider1 = (showPlays && showLikes) || (showPlays && showReposts);
        view.findViewById(R.id.divider1).setVisibility(showDivider1 ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.divider2).setVisibility((showLikes && showReposts)? View.VISIBLE : View.GONE);
    }

    private void setStat(TextView textView, Integer count) {
        if (count > 0) {
            textView.setVisibility(View.VISIBLE);
            textView.setText(String.valueOf(count));
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    public void bindDescription(View view, PropertySet propertySet) {
        TextView description = (TextView) view.findViewById(R.id.description);

        final String source = propertySet.get(TrackProperty.DESCRIPTION);
        description.setText(Html.fromHtml(source.replace(System.getProperty("line.separator"), "<br/>")));
        description.setVisibility(View.VISIBLE);
        view.findViewById(R.id.loading).setVisibility(View.GONE);
    }

    public void showSpinner(View view) {
        view.findViewById(R.id.description).setVisibility(View.GONE);
        view.findViewById(R.id.loading).setVisibility(View.VISIBLE);
    }
}
