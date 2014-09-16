package com.soundcloud.android.comments;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.playback.views.PlayablePresenter;
import com.soundcloud.android.playback.views.StatsView;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.screen.ScreenPresenter;
import com.soundcloud.propeller.PropertySet;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackCommentsActivity extends ScActivity {

    public static final String EXTRA_COMMENTED_TRACK = "extra";

    @Inject AdPlayerController adPlayerController;
    @Inject SlidingPlayerController playerController;
    @Inject PlayablePresenter playablePresenter;
    @Inject ScreenPresenter presenter;

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
            final Uri contentUri = Content.TRACK_COMMENTS.forId(commentedTrack.get(TrackProperty.URN).numericId);
            final ScListFragment fragment = ScListFragment.newInstance(contentUri, getCurrentScreen());
            getSupportFragmentManager().beginTransaction().add(R.id.listHolder, fragment).commit();
        }
    }

    private void bindTrackHeaderView(PropertySet commentedTrack) {
        View view = findViewById(R.id.playable_bar);
        playablePresenter.setTitleView((TextView) view.findViewById(R.id.playable_title));
        playablePresenter.setUsernameView((TextView) view.findViewById(R.id.playable_user));
        playablePresenter.setStatsView((StatsView) view.findViewById(R.id.stats));
        playablePresenter.setPrivacyIndicatorView((TextView) view.findViewById(R.id.playable_private_indicator));
        playablePresenter.setCreatedAtView((TextView) view.findViewById(R.id.playable_created_at));
        playablePresenter.setArtwork((ImageView) findViewById(R.id.icon), ApiImageSize.getListItemImageSize(getResources()));
        playablePresenter.setPlayable(commentedTrack);
    }

    @Override
    protected void setContentView() {
        presenter.setBaseLayoutWithContent(R.layout.playable_interaction_activity);
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
