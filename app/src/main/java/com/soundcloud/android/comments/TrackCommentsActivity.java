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
import com.soundcloud.android.rx.observers.DefaultMaybeObserver;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

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

    private Disposable trackDisposable = Disposables.disposed();

    public TrackCommentsActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        ButterKnife.bind(this);

        final Urn trackUrn = Urns.urnFromIntent(getIntent(), EXTRA_COMMENTED_TRACK_URN);
        trackDisposable = trackRepository.track(trackUrn)
                                         .observeOn(AndroidSchedulers.mainThread())
                                         .subscribeWith(new TrackObserver());

        if (bundle == null) {
            attachCommentsFragment(trackUrn);
        }
    }

    @Override
    protected void onDestroy() {
        trackDisposable.dispose();
        super.onDestroy();
    }

    private void attachCommentsFragment(Urn trackUrn) {
        final Fragment fragment = CommentsFragment.create(trackUrn);
        getSupportFragmentManager().beginTransaction().add(R.id.comments_fragment, fragment).commit();
    }

    public void setCount(Track commentedTrack) {
        final int numberOfComments = commentedTrack.commentsCount();
        if (numberOfComments > 0) {
            count.setVisibility(View.VISIBLE);
            count.setText(String.valueOf(numberOfComments));
        } else {
            count.setVisibility(View.GONE);
        }
    }

    private void setDate(Track commentedTrack) {
        final long timestamp = commentedTrack.createdAt().getTime();
        date.setText(ScTextUtils.formatTimeElapsedSince(getResources(), timestamp, true));
    }

    private void setIcon(Track commentedTrack) {
        imageOperations.displayWithPlaceholder(
                commentedTrack.urn(),
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

    private class TrackObserver extends DefaultMaybeObserver<Track> {
        @Override
        public void onSuccess(Track track) {
            title.setText(track.title());
            username.setText(track.creatorName());
            setCount(track);
            setDate(track);
            setIcon(track);
        }
    }
}
