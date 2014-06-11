package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlayerFragment extends Fragment {

    @Inject
    EventBus eventBus;
    @Inject
    PlayQueueManager playQueueManager;
    @Inject
    PlayerPresenter.Factory playerPresenterFactory;

    private PlayerPresenter presenter;

    private final CompositeSubscription viewLifetimeSubscription = new CompositeSubscription();
    private CompositeSubscription foregroundLifetimeSubscription;

    public PlayerFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.player_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        presenter = playerPresenterFactory.create(view);
        if (!playQueueManager.isQueueEmpty()) {
            presenter.onPlayQueueChanged();
            presenter.setQueuePosition(playQueueManager.getCurrentPosition());
        }
        subscribeViewLifetimeEvents();
    }

    @Override
    public void onResume() {
        super.onResume();
        subscribeForegroundLifetimeEvents();
    }

    @Override
    public void onPause() {
        unsubscribeForegroundLifetimeEvents();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        unsubscribeViewLifetimeEvents();
        super.onDestroyView();
    }

    private void subscribeViewLifetimeEvents() {
        viewLifetimeSubscription.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber()));
        viewLifetimeSubscription.add(eventBus.subscribe(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressSubscriber()));
    }

    private void subscribeForegroundLifetimeEvents() {
        foregroundLifetimeSubscription = new CompositeSubscription();
        foregroundLifetimeSubscription.add(eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber()));
        foregroundLifetimeSubscription.add(eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerUISubscriber()));
    }

    private void unsubscribeViewLifetimeEvents() {
        viewLifetimeSubscription.unsubscribe();
    }

    private void unsubscribeForegroundLifetimeEvents() {
        foregroundLifetimeSubscription.unsubscribe();
    }

    private final class PlaybackStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {
            if (presenter != null) {
                presenter.onPlayStateChanged(stateTransition.playSessionIsActive());
            }
        }
    }

    private final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        @Override
        public void onNext(PlaybackProgressEvent progress) {
            if (presenter != null) {
                presenter.onPlayerProgress(progress);
            }
        }
    }

    private final class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            if (presenter != null) {
                if (event.getKind() == PlayQueueEvent.NEW_QUEUE
                        || event.getKind() == PlayQueueEvent.QUEUE_UPDATE) {
                    presenter.onPlayQueueChanged();
                }
                presenter.setQueuePosition(playQueueManager.getCurrentPosition());
            }
        }
    }
    private final class PlayerUISubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (presenter != null) {
                if (event.getKind() == PlayerUIEvent.PLAYER_EXPANDED) {
                    presenter.setFullScreenPlayer(true);
                } else if (event.getKind() == PlayerUIEvent.PLAYER_COLLAPSED) {
                    presenter.setFullScreenPlayer(false);
                }
            }
        }
    }

}
