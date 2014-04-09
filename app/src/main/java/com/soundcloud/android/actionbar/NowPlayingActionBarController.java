package com.soundcloud.android.actionbar;

import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventBus;
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

    private final NowPlayingProgressBar mNowPlaying;
    private final View mNowPlayingHolder;
    private final PlaybackStateProvider mPlaybackStateProvider = new PlaybackStateProvider();

    private boolean mListening;

    public NowPlayingActionBarController(@NotNull ActionBarOwner owner, @NotNull EventBus eventBus) {
        super(owner, eventBus);

        View customView = mActivity.getLayoutInflater().inflate(R.layout.action_bar_now_playing_custom_view, null);
        mOwner.getActivity().getSupportActionBar().setCustomView(customView, new ActionBar.LayoutParams(Gravity.RIGHT));
        mNowPlaying = (NowPlayingProgressBar) customView.findViewById(R.id.waveform_progress);
        mNowPlayingHolder = customView.findViewById(R.id.waveform_holder);
        mNowPlayingHolder.setOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListening();
    }

    @Override
    public void onResume() {
        super.onResume();

        mNowPlaying.resume();
        startListening();

        if (mPlaybackStateProvider.getCurrentTrackId() < 0) {
            mNowPlayingHolder.setVisibility(View.GONE);
        } else {
            mNowPlayingHolder.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        mNowPlaying.destroy();
        super.onDestroy();
    }

    @Override
    protected void setActionBarDefaultOptions(ActionBar actionBar) {
        super.setActionBarDefaultOptions(actionBar);
        actionBar.setDisplayShowCustomEnabled(true);
    }

    private void startListening() {
        if (!mListening) {
            mListening = true;
            IntentFilter f = new IntentFilter();
            f.addAction(Broadcasts.PLAYSTATE_CHANGED);
            f.addAction(Broadcasts.META_CHANGED);
            mOwner.getActivity().registerReceiver(mStatusListener, new IntentFilter(f));
        }
    }

    private void stopListening() {
        if (mListening) {
            mOwner.getActivity().unregisterReceiver(mStatusListener);
        }
        mListening = false;
    }

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mNowPlaying.getStatusListener().onReceive(context, intent);

            if (mNowPlayingHolder.getVisibility() == View.GONE && mPlaybackStateProvider.isSupposedToBePlaying()) {
                mNowPlayingHolder.setVisibility(View.VISIBLE);
            }

            if (intent.getAction().equals(Broadcasts.PLAYSTATE_CHANGED)) {
                mOwner.getActivity().supportInvalidateOptionsMenu();
            }
        }
    };

    @Override
    public void onClick(View v) {
        mActivity.startActivity(new Intent(Actions.PLAYER));
        mEventBus.publish(EventQueue.UI, UIEvent.fromPlayerShortcut());
    }

}
