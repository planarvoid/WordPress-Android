package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.service.Playa.*;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscriber;
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
public class PlayerFragment extends Fragment {

    @Inject
    EventBus eventBus;

    private ToggleButton footerToggle;
    private TextView trackTitle;

    private Subscription subscription = Subscriptions.empty();

    public PlayerFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    PlayerFragment(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.player_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        footerToggle = (ToggleButton) view.findViewById(R.id.footer_toggle);
        trackTitle = (TextView) view.findViewById(R.id.footer_title);
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

    private final class PlaybackStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {
            footerToggle.setChecked(stateTransition.isPlaying());
            trackTitle.setText(stateTransition.toString());
        }
    }
}
