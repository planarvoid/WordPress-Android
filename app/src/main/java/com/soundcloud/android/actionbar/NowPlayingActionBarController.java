package com.soundcloud.android.actionbar;

import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;
import static com.soundcloud.android.playback.service.PlaybackService.getCurrentTrackId;

import com.soundcloud.android.Actions;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.R;
import org.jetbrains.annotations.NotNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBar;
import android.view.Gravity;
import android.view.View;

public class NowPlayingActionBarController extends ActionBarController {

    private NowPlayingProgressBar mNowPlaying;
    private View mNowPlayingHolder;

    private boolean mListening;

    public NowPlayingActionBarController(@NotNull ActionBarOwner owner, PublicCloudAPI publicCloudAPI) {
        super(owner, publicCloudAPI);

        View customView = View.inflate(mActivity, R.layout.action_bar_now_playing_custom_view, null);
        mOwner.getActivity().getSupportActionBar().setCustomView(customView, new ActionBar.LayoutParams(Gravity.RIGHT));
        mNowPlaying = (NowPlayingProgressBar) customView.findViewById(R.id.waveform_progress);
        mNowPlayingHolder = customView.findViewById(R.id.waveform_holder);
        mNowPlayingHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.startActivity(new Intent(Actions.PLAYER));
            }
        });


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

        if (getCurrentTrackId() < 0) {
            mNowPlayingHolder.setVisibility(View.GONE);
        } else {
            mNowPlayingHolder.setVisibility(View.VISIBLE);
        }
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
            f.addAction(Broadcasts.SEEK_COMPLETE);
            f.addAction(Broadcasts.SEEKING);
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

            if (intent.getAction().equals(Broadcasts.PLAYSTATE_CHANGED)) {
                mOwner.getActivity().supportInvalidateOptionsMenu();
            }
        }
    };

}
