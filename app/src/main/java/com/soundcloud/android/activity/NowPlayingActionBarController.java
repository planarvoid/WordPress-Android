package com.soundcloud.android.activity;

import static com.soundcloud.android.service.playback.CloudPlaybackService.Broadcasts;
import static com.soundcloud.android.service.playback.CloudPlaybackService.getCurrentTrackId;

import com.soundcloud.android.Actions;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.view.NowPlayingIndicator;
import com.soundcloud.android.view.RootView;
import org.jetbrains.annotations.NotNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;

public class NowPlayingActionBarController extends ActionBarController {

    private NowPlayingIndicator mNowPlaying;
    private View mNowPlayingHolder;

    private boolean mListening;

    public NowPlayingActionBarController(@NotNull ActionBarOwner owner, @NotNull RootView rootView, AndroidCloudAPI androidCloudAPI) {
        super(owner, rootView, androidCloudAPI);
        mNowPlaying = (NowPlayingIndicator) getActionBarCustomView().findViewById(R.id.waveform_progress);
        mNowPlayingHolder = getActionBarCustomView().findViewById(R.id.waveform_holder);
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
    protected View createCustomView() {
        View customView = View.inflate(mActivity, R.layout.action_bar_now_playing_custom_view, null);
        setupHomeButton(customView.findViewById(R.id.custom_home));
        customView.findViewById(R.id.waveform_holder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRootView.isExpanded()) {
                    ScActivity.startNavActivity(mActivity, ScPlayer.class, mRootView.getMenuBundle());
                } else {
                    mActivity.startActivity(new Intent(Actions.PLAYER));
                }
            }
        });
        return customView;
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
                mOwner.invalidateOptionsMenu();
            }
        }
    };

}
