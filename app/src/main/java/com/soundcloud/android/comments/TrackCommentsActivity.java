package com.soundcloud.android.comments;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.LightCycle;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackCommentsActivity extends ScActivity {

    public static final String EXTRA_COMMENTED_TRACK_URN = "extra";

    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle ActionBarHelper actionBarHelper;

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject ImageOperations imageOperations;
    @Inject TrackRepository trackRepository;

    @Bind(R.id.title) TextView title;
    @Bind(R.id.username) TextView username;
    @Bind(R.id.comments_count) TextView count;
    @Bind(R.id.date) TextView date;
    @Bind(R.id.header_artwork) ImageView artwork;

    private Subscription trackSubscription;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        ButterKnife.bind(this);

        final Urn trackUrn = getIntent().getParcelableExtra(EXTRA_COMMENTED_TRACK_URN);
        trackSubscription = trackRepository.track(trackUrn)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new TrackSubscriber());

        if (bundle == null) {
            attachCommentsFragment(trackUrn);
        }
    }

    @Override
    protected void onDestroy() {
        trackSubscription.unsubscribe();
        super.onDestroy();
    }

    private void attachCommentsFragment(Urn trackUrn) {
        final Fragment fragment = CommentsFragment.create(trackUrn);
        getSupportFragmentManager().beginTransaction().add(R.id.comments_fragment, fragment).commit();
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
                artwork);
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayoutWithContent(this, R.layout.track_comments_activity);
    }

    @Override
    public Screen getScreen() {
        return Screen.PLAYER_COMMENTS;
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }

    private class TrackSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet track) {
            title.setText(track.get(PlayableProperty.TITLE));
            username.setText(track.get(PlayableProperty.CREATOR_NAME));
            setCount(track);
            setDate(track);
            setIcon(track);
        }
    }
}
