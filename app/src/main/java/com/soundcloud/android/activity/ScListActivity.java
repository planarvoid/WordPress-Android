package com.soundcloud.android.activity;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.view.AddCommentDialog;
import com.soundcloud.android.view.ScListView;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;


public abstract class ScListActivity extends ScActivity {

    protected Object[] mPreviousState;
    protected List<ScListView> mLists;
    private boolean mIgnorePlaybackStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(CloudPlaybackService.META_CHANGED);
        playbackFilter.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        playbackFilter.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
        registerReceiver(mPlaybackStatusListener, new IntentFilter(playbackFilter));

        IntentFilter generalIntentFilter = new IntentFilter();
        generalIntentFilter.addAction(Actions.CONNECTION_ERROR);
        generalIntentFilter.addAction(Actions.LOGGING_OUT);
        registerReceiver(mGeneralIntentListener, generalIntentFilter);

        mLists = new ArrayList<ScListView>();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPlaybackStatusListener);
        unregisterReceiver(mGeneralIntentListener);

        if (findViewById(R.id.container) != null){
            unbindDrawables(findViewById(R.id.container));
            System.gc();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        final long trackId = CloudPlaybackService.getCurrentTrackId();
        if (trackId != -1) {
            setPlayingTrack(trackId, CloudPlaybackService.getState().isSupposedToBePlaying());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIgnorePlaybackStatus = false;
    }

    @Override
    protected Dialog onCreateDialog(int which) {
        switch (which) {
            case Consts.Dialogs.DIALOG_ADD_COMMENT:
                final AddCommentDialog dialog = new AddCommentDialog(this);
                dialog.getWindow().setGravity(Gravity.TOP);
                return dialog;
            default:
                return super.onCreateDialog(which);
        }
    }

    protected void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            try {
                ((ViewGroup) view).removeAllViews();
            } catch (UnsupportedOperationException ignore){ }
        }
    }

    public void playTrack(Playable.PlayInfo info) {
        playTrack(info, true, false);
    }

    public void playTrack(Playable.PlayInfo info, boolean goToPlayer, boolean commentMode) {
        final Track t = info.getTrack();
        Intent intent = new Intent(this, CloudPlaybackService.class).setAction(CloudPlaybackService.PLAY_ACTION);
        if (CloudPlaybackService.getCurrentTrackId() != t.id) {
            // changing tracks
            intent.putExtra(CloudPlaybackService.PlayExtras.trackId, t.id);
            if (info.uri != null) {
                SoundCloudApplication.TRACK_CACHE.put(info.getTrack(), false);
                intent.putExtra(CloudPlaybackService.PlayExtras.trackId, info.getTrack().id)
                      .putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                      .setData(info.uri);
            } else {
                CloudPlaybackService.playlistXfer = info.playables;
                    intent.putExtra(CloudPlaybackService.PlayExtras.playPosition, info.position)
                    .putExtra(CloudPlaybackService.PlayExtras.playFromXferCache, true);
            }
        }
        startService(intent);

        if (goToPlayer) {
            launchPlayer(commentMode);
        }

    }

    private void launchPlayer(boolean commentMode) {
        Intent i = new Intent(this, ScPlayer.class);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.putExtra("commentMode",commentMode);
        startActivity(i);
        mIgnorePlaybackStatus = true;
    }

    public void addNewComment(final Comment comment) {
        getApp().pendingComment = comment;
        safeShowDialog(Consts.Dialogs.DIALOG_ADD_COMMENT);
    }

    private BroadcastReceiver mGeneralIntentListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Actions.CONNECTION_ERROR.equals(intent.getAction())) {
                safeShowDialog(Consts.Dialogs.DIALOG_ERROR_LOADING);
            } else if (Actions.LOGGING_OUT.equals(intent.getAction())) {
                // alert lists?
            }
        }
    };

    private BroadcastReceiver mPlaybackStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIgnorePlaybackStatus)
                return;

            final String action = intent.getAction();
            if (CloudPlaybackService.META_CHANGED.equals(action)) {
                setPlayingTrack(intent.getLongExtra("id", -1), true);
            } else if (CloudPlaybackService.PLAYBACK_COMPLETE.equals(action)) {
                setPlayingTrack(-1, false);
            } else if (CloudPlaybackService.PLAYSTATE_CHANGED.equals(action)) {
                setPlayingTrack(intent.getLongExtra("id", -1), intent.getBooleanExtra("isPlaying", false));
            }
        }
    };

    private void setPlayingTrack(long id, boolean isPlaying) {
        if (mLists == null || mLists.isEmpty())
            return;

        // todo, notify lists
    }

    public void addFavorite(Track track) {
        FavoriteAddTask f = new FavoriteAddTask(getApp());
        f.setOnFavoriteListener(mFavoriteListener);
        f.execute(track);
    }

    public void removeFavorite(Track track) {
        FavoriteRemoveTask f = new FavoriteRemoveTask(getApp());
        f.setOnFavoriteListener(mFavoriteListener);
        f.execute(track);
    }


    private FavoriteTask.FavoriteListener mFavoriteListener = new FavoriteTask.FavoriteListener() {
        @Override
        public void onNewFavoriteStatus(long trackId, boolean isFavorite) {
            // todo, notify lists
        }

        @Override
        public void onException(long trackId, Exception e) {
            // todo, notify lists
        }
    };

    protected void handleRecordingClick(Recording recording) {
    }



}
