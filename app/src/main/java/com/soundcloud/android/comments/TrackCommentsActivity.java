package com.soundcloud.android.comments;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackCommentsActivity extends ScActivity {

    public static final String EXTRA_COMMENTED_TRACK = "extra";

    @Inject @LightCycle AdPlayerController adPlayerController;
    @Inject @LightCycle SlidingPlayerController playerController;
    @Inject @LightCycle ActionBarHelper actionBarHelper;

    @Inject FeatureFlags featureFlags;
    @Inject ImageOperations imageOperations;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        final PropertySet commentedTrack = getIntent().getParcelableExtra(EXTRA_COMMENTED_TRACK);
        bindTrackHeaderView(commentedTrack);

        if (bundle == null) {
            attachCommentsFragment(commentedTrack);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        configureMainOptionMenuItems(menu);
        return true;
    }

    private void attachCommentsFragment(PropertySet commentedTrack) {
        final Urn trackUrn = commentedTrack.get(TrackProperty.URN);
        final Fragment fragment= CommentsFragment.create(trackUrn);
        getSupportFragmentManager().beginTransaction().add(R.id.comments_fragment, fragment).commit();
    }

    private void bindTrackHeaderView(PropertySet commentedTrack) {
        ((TextView) findViewById(R.id.title)).setText(commentedTrack.get(PlayableProperty.TITLE));
        ((TextView) findViewById(R.id.username)).setText(commentedTrack.get(PlayableProperty.CREATOR_NAME));
        ((TextView) findViewById(R.id.comments_count)).setText(String.valueOf(commentedTrack.get(TrackProperty.COMMENTS_COUNT)));
        setDate(commentedTrack);
        setIcon(commentedTrack);
    }

    private void setIcon(PropertySet commentedTrack) {
        imageOperations.displayWithPlaceholder(
                commentedTrack.get(TrackProperty.URN),
                ApiImageSize.getListItemImageSize(getResources()),
                (ImageView) findViewById(R.id.icon));
    }

    private void setDate(PropertySet commentedTrack) {
        final long timestamp = commentedTrack.get(PlayableProperty.CREATED_AT).getTime();
        ((TextView) findViewById(R.id.date)).setText(ScTextUtils.formatTimeElapsedSince(getResources(), timestamp, true));
    }

    @Override
    protected void setContentView() {
        presenter.setBaseLayoutWithContent(R.layout.track_comments_activity);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(getCurrentScreen()));
        }
    }

    private Screen getCurrentScreen() {
        return Screen.PLAYER_COMMENTS;
    }

}
