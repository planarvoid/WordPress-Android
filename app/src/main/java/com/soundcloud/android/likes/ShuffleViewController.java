package com.soundcloud.android.likes;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.utils.CallsiteToken;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.HeaderViewController;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ShuffleViewController extends HeaderViewController implements Observer<List<Urn>> {

    private final Provider<ExpandPlayerSubscriber> subscriberProvider;
    private final PlaybackOperations playbackOperations;
    private final EventBus eventBus;
    private List<Urn> likedTracks;

    private View shuffleView;

    @InjectView(R.id.shuffle_txt) TextView shuffleTextView;
    @InjectView(R.id.shuffle_btn) Button shuffleButton;

    private Subscription subscription = Subscriptions.empty();

    private final CallsiteToken callsiteToken = CallsiteToken.build();

    private final Action0 sendShuffleLikesAnalytics = new Action0() {
        @Override
        public void call() {
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffleMyLikes());
        }
    };

    @Inject
    public ShuffleViewController(Provider<ExpandPlayerSubscriber> subscriberProvider,
                                 PlaybackOperations playbackOperations, EventBus eventBus) {
        this.subscriberProvider = subscriberProvider;
        this.playbackOperations = playbackOperations;
        this.eventBus = eventBus;
        this.likedTracks = new ArrayList<>();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        shuffleView = view.inflate(view.getContext(), R.layout.likes_shuffle_header, null);
        ButterKnife.inject(this, shuffleView);
    }

    @Override
    public void onDestroyView() {
        subscription.unsubscribe();
        shuffleView = null;
    }

    @Override
    public View getHeaderView() {
        return shuffleView;
    }

    private void updateShuffleView(Collection<?> collection) {
        updateShuffleViewText(collection.size());
        updateShuffleButton(collection.size());
    }

    private void updateShuffleViewText(int likedTracks) {
        if (likedTracks == 0) {
            shuffleTextView.setText(shuffleView.getContext().getString(R.string.number_of_liked_tracks_you_liked_zero));
        } else {
            shuffleTextView.setText(shuffleView.getContext().getResources()
                    .getQuantityString(R.plurals.number_of_liked_tracks_you_liked, likedTracks, likedTracks));
        }
    }

    private void updateShuffleButton(int likedTracks) {
        if (likedTracks <= 1) {
            shuffleButton.setVisibility(View.GONE);
            shuffleButton.setEnabled(false);
        } else {
            shuffleButton.setVisibility(View.VISIBLE);
            shuffleButton.setEnabled(true);
        }
    }

    @OnClick(R.id.shuffle_btn)
    public void onShuffleButtonClick() {
        playbackOperations
                .playTracksShuffled(this.likedTracks, new PlaySessionSource(Screen.SIDE_MENU_LIKES))
                .doOnCompleted(sendShuffleLikesAnalytics)
                .subscribe(subscriberProvider.get());
    }

    @Override
    public void onNext(List<Urn> likedTracks) {
        this.likedTracks.clear();
        this.likedTracks.addAll(likedTracks);
        updateShuffleView(likedTracks);
    }

    @Override
    public void onCompleted() {
        // No-op
    }

    @Override
    public void onError(Throwable throwable) {
        ErrorUtils.handleThrowable(throwable, callsiteToken);
    }
}
