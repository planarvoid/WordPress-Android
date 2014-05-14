package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class PlayerFragment extends Fragment implements View.OnClickListener {

    @Inject
    EventBus eventBus;
    @Inject
    PlayQueueManager playQueueManager;
    @Inject
    PlaybackOperations playbackOperations;

    private ToggleButton footerToggle;
    private ToggleButton playerToggle;

    private TextView trackTitle;

    private Subscription subscription = Subscriptions.empty();

    public PlayerFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    PlayerFragment(EventBus eventBus, PlayQueueManager playQueueManager, PlaybackOperations playbackOperations) {
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.playbackOperations = playbackOperations;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.player_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        footerToggle = (ToggleButton) view.findViewById(R.id.footer_toggle);
        playerToggle = (ToggleButton) view.findViewById(R.id.player_toggle);
        trackTitle = (TextView) view.findViewById(R.id.footer_title);
        footerToggle.setOnClickListener(this);
        playerToggle.setOnClickListener(this);
        view.findViewById(R.id.player_next).setOnClickListener(this);
        view.findViewById(R.id.player_previous).setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        subscription = eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber());
    }

    @Override
    public void onPause() {
        super.onPause();
        subscription.unsubscribe();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.footer_toggle:
            case R.id.player_toggle:
                playbackOperations.togglePlayback(getActivity());
                break;
            case R.id.player_next:
                playbackOperations.nextTrack();
                break;
            case R.id.player_previous:
                playbackOperations.previousTrack();
                break;
        }
    }

    private final class PlaybackStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {
            footerToggle.setChecked(stateTransition.isPlaying());
            playerToggle.setChecked(stateTransition.isPlaying());
            trackTitle.setText(Urn.forTrack(playQueueManager.getCurrentTrackId()).toString());
        }
    }
}
