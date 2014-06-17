package com.soundcloud.android.actionbar;

import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import org.jetbrains.annotations.NotNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBar;
import android.view.Gravity;
import android.view.View;

public class NowPlayingActionBarController extends ActionBarController implements View.OnClickListener {

    private final NowPlayingProgressBar nowPlaying;
    private final View nowPlayingHolder;
    private final PlaybackStateProvider playbackStateProvider = new PlaybackStateProvider();

    private boolean isListening;

    public NowPlayingActionBarController(@NotNull ActionBarOwner owner, @NotNull EventBus eventBus) {
        super(owner, eventBus);

        View customView = activity.getLayoutInflater().inflate(R.layout.action_bar_now_playing_custom_view, null);
        owner.getActivity().getSupportActionBar().setCustomView(customView, new ActionBar.LayoutParams(Gravity.RIGHT));
        nowPlaying = (NowPlayingProgressBar) customView.findViewById(R.id.waveform_progress);
        nowPlayingHolder = customView.findViewById(R.id.waveform_holder);
        nowPlayingHolder.setOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListening();
        nowPlaying.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        nowPlaying.resume();
        startListening();

        if (playbackStateProvider.getCurrentTrackId() < 0) {
            nowPlayingHolder.setVisibility(View.GONE);
        } else {
            nowPlayingHolder.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        nowPlaying.destroy();
        super.onDestroy();
    }

    @Override
    protected void setActionBarDefaultOptions(ActionBar actionBar) {
        super.setActionBarDefaultOptions(actionBar);
        actionBar.setDisplayShowCustomEnabled(true);
    }

    private void startListening() {
        if (!isListening) {
            isListening = true;
            IntentFilter f = new IntentFilter();
            f.addAction(Broadcasts.PLAYSTATE_CHANGED);
            f.addAction(Broadcasts.META_CHANGED);
            owner.getActivity().registerReceiver(mStatusListener, new IntentFilter(f));
        }
    }

    private void stopListening() {
        if (isListening) {
            owner.getActivity().unregisterReceiver(mStatusListener);
        }
        isListening = false;
    }

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            nowPlaying.getStatusListener().onReceive(context, intent);

            if (nowPlayingHolder.getVisibility() == View.GONE && playbackStateProvider.isSupposedToBePlaying()) {
                nowPlayingHolder.setVisibility(View.VISIBLE);
            }

            if (intent.getAction().equals(Broadcasts.PLAYSTATE_CHANGED)) {
                owner.getActivity().supportInvalidateOptionsMenu();
            }
        }
    };

    @Override
    public void onClick(View v) {
        activity.startActivity(new Intent(Actions.PLAYER));
        eventBus.publish(EventQueue.UI, UIEvent.fromPlayerShortcut());
    }

}
