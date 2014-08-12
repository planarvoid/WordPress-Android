package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.propeller.PropertySet;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackInfoPresenter {

    @Inject
    }

    public View create(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.track_details_view, container, false);
    }

    public void bind(View view, PropertySet propertySet) {
        ((TextView) view.findViewById(R.id.title)).setText(propertySet.get(PlayableProperty.TITLE));
        ((TextView) view.findViewById(R.id.creator)).setText(propertySet.get(PlayableProperty.CREATOR_NAME));

        configureStats(view, propertySet);
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
