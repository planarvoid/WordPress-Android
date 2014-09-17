package com.soundcloud.android.comments;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.screen.ScreenPresenter;
import com.soundcloud.propeller.PropertySet;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackCommentsActivity extends ScActivity {

    public static final String EXTRA_COMMENTED_TRACK = "extra";

    @Inject AdPlayerController adPlayerController;
    @Inject SlidingPlayerController playerController;
    @Inject ScreenPresenter presenter;
    @Inject FeatureFlags featureFlags;
    @Inject ImageOperations imageOperations;

    public TrackCommentsActivity() {
        addLifeCycleComponent(playerController);
        addLifeCycleComponent(adPlayerController);
        presenter.attach(this);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        final PropertySet commentedTrack = getIntent().getParcelableExtra(EXTRA_COMMENTED_TRACK);
        bindTrackHeaderView(commentedTrack);

        if (bundle == null) {
            attachCommentsFragment(commentedTrack);
        }
    }

    private void attachCommentsFragment(PropertySet commentedTrack) {
        final TrackUrn trackUrn = commentedTrack.get(TrackProperty.URN);
        Fragment fragment;
        if (featureFlags.isEnabled(Feature.COMMENTS_REDESIGN)) {
            fragment = CommentsFragment.create(trackUrn);
        } else {
            final Uri contentUri = Content.TRACK_COMMENTS.forId(trackUrn.numericId);
            fragment = ScListFragment.newInstance(contentUri, getCurrentScreen());
        }
        getSupportFragmentManager().beginTransaction().add(R.id.list_container, fragment).commit();
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
            eventBus.publish(EventQueue.SCREEN_ENTERED, getCurrentScreen().get());
        }
    }

    private Screen getCurrentScreen() {
        return Screen.PLAYER_COMMENTS;
    }

}
