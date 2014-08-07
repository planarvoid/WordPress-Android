package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackInfoPresenter {

    @Inject
    public TrackInfoPresenter() {
    }

    public void bind(View view, PropertySet propertySet) {
        ((TextView) view.findViewById(R.id.title)).setText(propertySet.get(PlayableProperty.TITLE));
        ((TextView) view.findViewById(R.id.creator)).setText(propertySet.get(PlayableProperty.CREATOR_NAME));
    }

    public void bindDescription(View view, PropertySet propertySet) {
        TextView description = (TextView) view.findViewById(R.id.description);

        description.setText(propertySet.get(TrackProperty.DESCRIPTION));
        description.setVisibility(View.VISIBLE);
        view.findViewById(R.id.loading).setVisibility(View.GONE);
    }

    public void showSpinner(View view) {
        view.findViewById(R.id.description).setVisibility(View.GONE);
        view.findViewById(R.id.loading).setVisibility(View.VISIBLE);
    }
}
