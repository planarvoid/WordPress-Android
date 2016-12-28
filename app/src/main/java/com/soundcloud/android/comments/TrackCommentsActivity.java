package com.soundcloud.android.comments;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackCommentsActivity extends PlayerActivity {

    public static final String EXTRA_COMMENTED_TRACK_URN = "extra";

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject ImageOperations imageOperations;
    @Inject TrackRepository trackRepository;

    @BindView(R.id.title) TextView title;
    @BindView(R.id.username) TextView username;
    @BindView(R.id.comments_count) TextView count;
    @BindView(R.id.date) TextView date;
    @BindView(R.id.header_artwork) ImageView artwork;

    private Subscription trackSubscription;

    public TrackCommentsActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

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

    public void setCount(TrackItem commentedTrack) {
        final int numberOfComments = commentedTrack.getCommentsCount();
        if (numberOfComments > 0) {
            count.setVisibility(View.VISIBLE);
            count.setText(String.valueOf(numberOfComments));
        } else {
            count.setVisibility(View.GONE);
        }
    }

    private void setDate(TrackItem commentedTrack) {
        final long timestamp = commentedTrack.getCreatedAt().getTime();
        date.setText(ScTextUtils.formatTimeElapsedSince(getResources(), timestamp, true));
    }

    private void setIcon(TrackItem commentedTrack) {
        imageOperations.displayWithPlaceholder(
                commentedTrack.getUrn(),
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

    private class TrackSubscriber extends DefaultSubscriber<TrackItem> {
        @Override
        public void onNext(TrackItem track) {
            title.setText(track.getTitle());
            username.setText(track.getCreatorName());
            setCount(track);
            setDate(track);
            setIcon(track);
        }
    }
}
