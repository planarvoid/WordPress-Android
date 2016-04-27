package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.lightcycle.ActivityLightCycle;

import android.support.v7.app.AppCompatActivity;

/**
 * Only used to provide instance of a presenter
 * based on {@link com.soundcloud.android.properties.Flag#NEW_SEARCH_SUGGESTIONS} feature flag.
 */
public interface FeaturedSearchPresenter extends ActivityLightCycle<AppCompatActivity> {
    void onScrollChanged();
    void dismiss(AppCompatActivity activity);
    void performSearch(String searchQuery);
    void playTrack(Urn trackUrn);
    void showUserProfile(Urn userUrn);
}
