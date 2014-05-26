package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlayerFragment extends Fragment implements PlayerPresenter.Listener {

    @Inject
    EventBus eventBus;
    @Inject
    PlayQueueManager playQueueManager;
    @Inject
    PlaybackOperations playbackOperations;
    @Inject
    PlayerPresenter.Factory playerPresenterFactory;

    private PlayerPresenter presenter;

    private final CompositeSubscription eventSubscription = new CompositeSubscription();

    public PlayerFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.player_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        presenter = playerPresenterFactory.create(view, this);
        presenter.setQueuePosition(playQueueManager.getCurrentPosition());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventSubscription.add(eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber()));
        eventSubscription.add(eventBus.subscribe(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressSubscriber()));
        eventSubscription.add(eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber()));
        eventSubscription.add(eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerUISubscriber()));
    }

    @Override
    public void onDestroy() {
        eventSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onTogglePlay() {
        playbackOperations.togglePlayback();
    }

    @Override
    public void onNext() {
        playbackOperations.nextTrack();
    }

    @Override
    public void onPrevious() {
        playbackOperations.previousTrack();
    }

    @Override
    public void onTrackChanged(int position) {
        playbackOperations.setPlayQueuePosition(position);
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
                if (event.getKind() == PlayQueueEvent.QUEUE_CHANGE
                        || event.getKind() == PlayQueueEvent.RELATED_TRACKS_CHANGE) {
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
