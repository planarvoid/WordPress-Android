package com.soundcloud.android.comments;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackCommentsActivity extends ScActivity {

    public static final String EXTRA_COMMENTED_TRACK = "extra";

    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle ActionBarHelper actionBarHelper;

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject ImageOperations imageOperations;

    @Bind(R.id.title) TextView title;
    @Bind(R.id.username) TextView username;
    @Bind(R.id.comments_count) TextView count;
    @Bind(R.id.date) TextView date;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        final PropertySet commentedTrack = getIntent().getParcelableExtra(EXTRA_COMMENTED_TRACK);
        ButterKnife.bind(this);
        bindTrackHeaderView(commentedTrack);

        if (bundle == null) {
            attachCommentsFragment(commentedTrack);
        }
    }

    private void attachCommentsFragment(PropertySet commentedTrack) {
        final Urn trackUrn = commentedTrack.get(TrackProperty.URN);
        final Fragment fragment = CommentsFragment.create(trackUrn);
        getSupportFragmentManager().beginTransaction().add(R.id.comments_fragment, fragment).commit();
    }

    private void bindTrackHeaderView(PropertySet commentedTrack) {
        title.setText(commentedTrack.get(PlayableProperty.TITLE));
        username.setText(commentedTrack.get(PlayableProperty.CREATOR_NAME));
        setCount(commentedTrack);
        setDate(commentedTrack);
        setIcon(commentedTrack);
    }

    public void setCount(PropertySet commentedTrack) {
        final int numberOfComments = commentedTrack.get(TrackProperty.COMMENTS_COUNT);
        if (numberOfComments > 0) {
            count.setVisibility(View.VISIBLE);
            count.setText(String.valueOf(numberOfComments));
        } else {
            count.setVisibility(View.GONE);
        }
    }

    private void setDate(PropertySet commentedTrack) {
        final long timestamp = commentedTrack.get(PlayableProperty.CREATED_AT).getTime();
        date.setText(ScTextUtils.formatTimeElapsedSince(getResources(), timestamp, true));
    }

    private void setIcon(PropertySet commentedTrack) {
        imageOperations.displayWithPlaceholder(
                commentedTrack.get(TrackProperty.URN),
                ApiImageSize.getListItemImageSize(getResources()),
                (ImageView) findViewById(R.id.icon));
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayoutWithContent(this, R.layout.track_comments_activity);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(getCurrentScreen()));
        }
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }

    private Screen getCurrentScreen() {
        return Screen.PLAYER_COMMENTS;
    }

}
