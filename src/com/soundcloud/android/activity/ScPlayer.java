
package com.soundcloud.android.activity;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.FocusHelper;
import com.soundcloud.android.view.PlayerTrackView;
import com.soundcloud.android.view.WaveformController;
import com.soundcloud.android.view.WorkspaceView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class ScPlayer extends ScActivity implements WorkspaceView.OnScreenChangeListener, WorkspaceView.OnScrollListener {
    private static final String TAG = "ScPlayer";
    private static final int REFRESH = 1;
    private static final int SEND_CURRENT_QUEUE_POSITION = 2;

    public static final int REFRESH_DELAY = 1000;
    private static final long TRACK_SWIPE_UPDATE_DELAY = 1000;
    private static final long TRACK_NAV_DELAY = 500;

    private long mSeekPos = -1;
    private boolean mWaveformLoaded, mActivityPaused, mIsCommenting, mIsPlaying, mChangeTrackFast;
    private Drawable mPlayState, mPauseState;
    private ImageButton mPauseButton, mFavoriteButton, mCommentButton;
    private Track mPlayingTrack;
    private RelativeLayout mContainer;
    private WorkspaceView mTrackWorkspace;
    private int mCurrentQueuePosition = -1;
    private Drawable mFavoriteDrawable, mFavoritedDrawable;


    public interface PlayerError {
        int PLAYBACK_ERROR = 0;
        int STREAM_ERROR = 1;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.sc_player);

        mContainer = (RelativeLayout) findViewById(R.id.container);

        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnClickListener(mPauseListener);
        findViewById(R.id.prev).setOnClickListener(mPrevListener);
        findViewById(R.id.next).setOnClickListener(mNextListener);

        mTrackWorkspace = (WorkspaceView) findViewById(R.id.track_view);
        mTrackWorkspace.setVisibility(View.GONE);
        mTrackWorkspace.setOnScreenChangeListener(this);
        mTrackWorkspace.setOnScrollListener(this, false);

        mCommentButton = (ImageButton) findViewById(R.id.btn_comment);
        if (mCommentButton != null) {

            mCommentButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleCommentMode(getCurrentTrackView().getPlayPosition());
                }
            });

            mFavoriteButton = (ImageButton) findViewById(R.id.btn_favorite);
            mFavoriteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleFavorite(mPlayingTrack);
                }
            });
        }


        mPauseState = getResources().getDrawable(R.drawable.ic_pause_states);
        mPlayState = getResources().getDrawable(R.drawable.ic_play_states);

        final Object[] saved = (Object[]) getLastNonConfigurationInstance();
        if (saved != null && saved[0] != null) mPlayingTrack = (Track) saved[0];

        if (getIntent().hasExtra("showTrackId")){
            setTrackDisplaySingleTrack(getIntent().getLongExtra("showTrackId", -1), true);
        }

        // this is to make sure keyboard is hidden after commenting
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void toggleCommentMode(int playPos) {

        PlayerTrackView ptv = getTrackView(playPos);
        if (ptv != null) {
            mIsCommenting = !mIsCommenting;
            ptv.setCommentMode(mIsCommenting);
            if (mCommentButton != null) {
                if (mIsCommenting) {
                    mCommentButton.setImageResource(R.drawable.ic_commenting_states);
                } else {
                    mCommentButton.setImageResource(R.drawable.ic_comment_states);
                }
            }
            if (mPlaybackService != null) try {
                mPlaybackService.setAutoAdvance(!mIsCommenting);
            } catch (RemoteException ignored) {
            }
        }

    }

    public ViewGroup getCommentHolder() {
        return mContainer;
    }

    @Override
    public void onScroll(float screenFraction) {
        if (screenFraction != Math.round(screenFraction)){
            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
        }
    }

    @Override public void onScreenChanging(View newScreen, int newScreenIndex) {}

    @Override public void onNextScreenVisible(View newScreen, int newScreenIndex) {}

    @Override
    public void onScreenChanged(View newScreen, int newScreenIndex) {
        if (newScreen == null) return;
        try {
            final int newQueuePos = ((PlayerTrackView) newScreen).getPlayPosition();

            mCurrentQueuePosition = newQueuePos;
            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

            mHandler.sendMessageDelayed(mHandler.obtainMessage(SEND_CURRENT_QUEUE_POSITION),
                        mChangeTrackFast ? TRACK_NAV_DELAY : TRACK_SWIPE_UPDATE_DELAY);
            mChangeTrackFast = false;

            final long prevTrackId = newQueuePos > 0
                    ? mPlaybackService.getTrackIdAt(newQueuePos -1) : -1;
            final long nextTrackId = newQueuePos < mPlaybackService.getQueueLength() - 1
                    ? mPlaybackService.getTrackIdAt(newQueuePos + 1) : -1;

            PlayerTrackView ptv;

            if (newScreenIndex == 0 && prevTrackId != -1) {
                final Track prevTrack = getTrackById(prevTrackId, newQueuePos - 1);
                if (prevTrack != null){
                    if (mTrackWorkspace.getScreenCount() > 2) {
                        ptv = (PlayerTrackView) mTrackWorkspace.getScreenAt(2);
                        ptv.getWaveformController().reset(true);
                        mTrackWorkspace.removeViewFromBack();
                    } else {
                        ptv = new PlayerTrackView(this);
                    }
                    mTrackWorkspace.addViewToFront(ptv);
                    ptv.setTrack(prevTrack, newQueuePos - 1, false);
                    mTrackWorkspace.setCurrentScreenNow(1, false);
                }

            } else if (newScreenIndex == mTrackWorkspace.getScreenCount() - 1 && nextTrackId != -1) {
                final Track nextTrack = getTrackById(nextTrackId, newQueuePos + 1);
                if (nextTrack != null){
                    if (mTrackWorkspace.getScreenCount() > 2) {
                        ptv = (PlayerTrackView) mTrackWorkspace.getScreenAt(0);
                        ptv.getWaveformController().reset(true);
                        mTrackWorkspace.removeViewFromFront();
                    } else {
                        ptv = new PlayerTrackView(this);
                    }
                    mTrackWorkspace.addViewToBack(ptv);
                    ptv.setTrack(nextTrack, newQueuePos + 1, false);
                    mTrackWorkspace.setCurrentScreenNow(1, false);
                }
            }

        } catch (RemoteException ignored) {
        }

    }

    public long setSeekMarker(float seekPercent) {
        try {
            if (mPlaybackService != null) {
                if (!mPlaybackService.isSeekable()) {
                    mSeekPos = -1;
                    return mPlaybackService.getPosition();
                } else {
                    if (mPlayingTrack != null) {
                        // where would we be if we had seeked
                        mSeekPos = mPlaybackService.seek((long) (mPlayingTrack.duration * seekPercent), false);
                        return mSeekPos;
                    }
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        return 0;
    }

    public long sendSeek(float seekPercent) {
        try {
            if (mPlaybackService == null || !mPlaybackService.isSeekable()) {
                return -1;
            }
            mSeekPos = -1;
            return mPlaybackService.seek(
                    (long) (mPlayingTrack.duration * seekPercent),
                    true);

        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        return -1;
    }

    public void onWaveformLoaded(){
        mWaveformLoaded = true;
        try {
            if (mPlaybackService != null) mPlaybackService.setClearToPlay(true);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    public boolean isSeekable() {
        try {
            return !(mPlaybackService == null || !mPlaybackService.isSeekable());
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean toggleFavorite(Track track) {
        if (track == null) return false;
        try {
            mPlaybackService.setFavoriteStatus(track.id, !track.user_favorite);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
            return false;
        }
        return true;
    }

    public void onNewComment(Comment comment) {
        final PlayerTrackView ptv = getTrackViewById(comment.track_id);
        if (ptv != null){
            ptv.onNewComment(comment);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putBoolean("paused", mActivityPaused);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        mActivityPaused = state.getBoolean("paused");
        super.onRestoreInstanceState(state);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[] {mPlayingTrack};
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.REFRESH:
                mPlayingTrack.info_loaded = false;
                mPlayingTrack.comments_loaded = false;
                mPlayingTrack.comments = null;
                getCurrentTrackView().onRefresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onServiceBound() {
        super.onServiceBound();

        if (getIntent().hasExtra("playIntent")) {
            startService(getIntent().<Intent>getParcelableExtra("playIntent"));
            getIntent().getExtras().clear();
        } else {
            long trackId = -1;
            try {
                trackId = mPlaybackService.getCurrentTrackId();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            if (trackId == -1) {
                Intent intent = new Intent(this, Main.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else {

                setTrackDisplayFromService(trackId);
                setPauseButtonImage();
                long next = refreshNow();
                queueNextRefresh(next);

                if (getIntent().getBooleanExtra("commentMode", false)) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            toggleCommentMode(getCurrentTrackView().getPlayPosition());
                        }
                    }, 200l);
                    getIntent().putExtra("commentMode", false);
                }

            }
        }
    }

    @Override
    protected void onDataConnectionChanged(boolean isConnected) {
        super.onDataConnectionChanged(isConnected);
        if (mPlayingTrack != null && isConnected) {
            if (mTrackWorkspace != null) {
                for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++){
                    ((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).onDataConnected();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++) {
            ((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).onDestroy();
        }
        super.onDestroy();
    }

    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };

    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlaybackService == null) return;
            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
            try {
                if (mPlaybackService.getPosition() < 2000 && mCurrentQueuePosition > 0) {
                    mChangeTrackFast = true;
                    mTrackWorkspace.scrollLeft();
                } else if (isSeekable()) {
                    mPlaybackService.seek(0, true);
                } else {
                    mPlaybackService.restart();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }
        }
    };


    private final View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlaybackService == null) return;
            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
            try {
                if (mPlaybackService.getQueueLength() > mCurrentQueuePosition + 1) {
                        mChangeTrackFast = true;
                        mTrackWorkspace.scrollRight();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }


            mTrackWorkspace.scrollRight();
        }
    };

    private void doPauseResume() {
        try {
            if (mPlaybackService != null) {
                mPlaybackService.toggle();
                if (mWaveformLoaded) mPlaybackService.setClearToPlay(true);
                long next = refreshNow();
                queueNextRefresh(next);
                setPauseButtonImage();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    private void setPauseButtonImage() {
        if (mPauseButton == null) return;
        try {
            if (mPlaybackService != null && mPlaybackService.isSupposedToBePlaying()) {
                mPauseButton.setImageDrawable(mPauseState);
            } else {
                mPauseButton.setImageDrawable(mPlayState);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    private void queueNextRefresh(long delay) {
        if (!mActivityPaused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {
        try {
            if (mPlaybackService == null)
                return REFRESH_DELAY;

            if (mPlaybackService.loadPercent() > 0 && !mIsPlaying) {
                mIsPlaying = true;
            }

            long pos = mPlaybackService.getPosition();
            long remaining = REFRESH_DELAY - (pos % REFRESH_DELAY);
            int queuePos = mPlaybackService.getQueuePosition();

            final PlayerTrackView ptv = getTrackView(queuePos);
            if (ptv != null){
                ptv.setProgress(pos, mPlaybackService.loadPercent(), Build.VERSION.SDK_INT >= WaveformController.MINIMUM_SMOOTH_PROGRESS_SDK &&
                        (mPlaybackService.isPlaying() && !mPlaybackService.isBuffering()));
            }

            // return the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            return !mPlaybackService.isPlaying() ? REFRESH_DELAY : remaining;

        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        return REFRESH_DELAY;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    break;
                case SEND_CURRENT_QUEUE_POSITION:
                    if (mPlaybackService != null) try {
                        mPlaybackService.setQueuePosition(mCurrentQueuePosition);
                    } catch (RemoteException ignore) {}
                    break;
                default:
                    break;
            }
        }
    };

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int queuePos = intent.getIntExtra("queuePosition", -1);
            String action = intent.getAction();
            if (action.equals(CloudPlaybackService.META_CHANGED)) {
                if (mCurrentQueuePosition != queuePos) {
                    if (mCurrentQueuePosition != -1 && queuePos == mCurrentQueuePosition + 1 && !mTrackWorkspace.isScrolling()) { // auto advance
                        mTrackWorkspace.scrollRight();
                    } else {
                        setTrackDisplayFromService(intent.getLongExtra("id", -1));
                    }
                }
                for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++) {
                    if (((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).getPlayPosition() != queuePos) {
                        ((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).getWaveformController().reset(false);
                    }
                }
                mPlayingTrack = getTrackById(intent.getLongExtra("id",-1), mCurrentQueuePosition);
                setPauseButtonImage();
                setFavoriteStatus();
                long next = refreshNow();
                queueNextRefresh(next);

            } else if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)) {
                setPauseButtonImage();
                if (getTrackView(queuePos) != null) getTrackView(queuePos).setPlaybackStatus(false, intent.getLongExtra("position", 0));
            } else if (action.equals(CloudPlaybackService.FAVORITE_SET) ||
                        action.equals(CloudPlaybackService.COMMENTS_LOADED) ||
                        action.equals(Consts.IntentActions.COMMENT_ADDED)) {
                for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++){
                    ((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).handleIdBasedIntent(intent);
                }
                if (action.equals(CloudPlaybackService.FAVORITE_SET)) setFavoriteStatus();
            } else {
                if (action.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {
                    setPauseButtonImage();
                }
                if (getTrackView(queuePos) != null) getTrackView(queuePos).handleStatusIntent(intent);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mActivityPaused = false;

        IntentFilter f = new IntentFilter();
        f.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(CloudPlaybackService.META_CHANGED);
        f.addAction(CloudPlaybackService.PLAYBACK_ERROR);
        f.addAction(CloudPlaybackService.STREAM_DIED);
        f.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        f.addAction(CloudPlaybackService.BUFFERING);
        f.addAction(CloudPlaybackService.BUFFERING_COMPLETE);
        f.addAction(CloudPlaybackService.COMMENTS_LOADED);
        f.addAction(CloudPlaybackService.SEEK_COMPLETE);
        f.addAction(CloudPlaybackService.FAVORITE_SET);
        f.addAction(Consts.IntentActions.COMMENT_ADDED);
        registerReceiver(mStatusListener, new IntentFilter(f));
    }

    @Override
    protected void onResume() {
        trackPage(Consts.Tracking.PLAYER);
        super.onResume();

        FocusHelper.registerHeadphoneRemoteControl(this);
        setPauseButtonImage();

        long next = refreshNow();
        queueNextRefresh(next);

    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            if (mPlaybackService != null) mPlaybackService.setClearToPlay(true);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++){
            ((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).onStop(true);
        }
        mActivityPaused = true;
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);
        mPlaybackService = null;
    }

    private Track getTrackById(long trackId, int queuePos) {
        Track t = SoundCloudApplication.TRACK_CACHE.get(trackId);
        if (t == null) {
            t = SoundCloudDB.getTrackById(getContentResolver(), trackId);
            if (t == null && queuePos != -1){
                try {
                    t = mPlaybackService.getTrackAt(queuePos);
                } catch (RemoteException ignored) {
                }
            }
        }
        return t;
    }

    private void setTrackDisplaySingleTrack(long trackId, boolean showBuffering) {
        final boolean first = mTrackWorkspace.getChildCount() == 0; // first layout, initialize workspace at end
        if (mTrackWorkspace.getScreenCount() > 1) {
            while (mTrackWorkspace.getScreenCount() > 1) {
                mTrackWorkspace.removeViewFromFront();
            }
            mTrackWorkspace.requestLayout();
        }
        PlayerTrackView ptv;
        if (mTrackWorkspace.getScreenCount() == 0){
            ptv = new PlayerTrackView(this);
            mTrackWorkspace.addView(ptv);
        } else {
            ptv = (PlayerTrackView) mTrackWorkspace.getScreenAt(0);
        }
        ptv.setTrack(getTrackById(trackId, -1), 0, false);
        setTrackDisplayIndex(0,showBuffering);
    }

    private void setTrackDisplayFromService(final long trackId) {
        if (mPlaybackService == null || trackId == -1)
            return;

        try {
            mCurrentQueuePosition = mPlaybackService.getQueuePosition();
            final long queueLength = mPlaybackService.getQueueLength();

            mPlayingTrack = getTrackById(trackId, mCurrentQueuePosition);
            setFavoriteStatus();

            final boolean first = mTrackWorkspace.getChildCount() == 0; // first layout, initialize workspace at end
            if (queueLength < mTrackWorkspace.getScreenCount()){
                while (queueLength < mTrackWorkspace.getScreenCount()){
                    mTrackWorkspace.removeViewFromFront();
                }
                mTrackWorkspace.requestLayout();
            }

            int workspaceIndex = 0;
            for (int pos = mCurrentQueuePosition -1; pos < mCurrentQueuePosition + 2; pos++){
                if (pos >= 0 && pos < queueLength){
                    PlayerTrackView ptv;
                    if (mTrackWorkspace.getScreenCount() > workspaceIndex){
                        ptv = ((PlayerTrackView) mTrackWorkspace.getScreenAt(workspaceIndex));
                    } else {
                        ptv = new PlayerTrackView(this);
                        mTrackWorkspace.addViewAtScreenPosition(ptv, workspaceIndex);
                    }
                    ptv.setTrack(getTrackById(mPlaybackService.getTrackIdAt(pos), pos), pos, false);
                    workspaceIndex++;
                }
            }

            setTrackDisplayIndex(mCurrentQueuePosition > 0 ? 1 : 0, mPlaybackService.isBuffering());
        } catch (RemoteException ignored) {}
    }

    private void setTrackDisplayIndex(int position, boolean showBuffering){
         if (!mTrackWorkspace.isInitialized()){
                mTrackWorkspace.setVisibility(View.VISIBLE);
                mTrackWorkspace.initWorkspace(position);
                mTrackWorkspace.setSeparator(R.drawable.track_view_seperator);
            } else {
                mTrackWorkspace.setCurrentScreenNow(mCurrentQueuePosition > 0 ? 1 : 0, false);
            }

            if (showBuffering && getCurrentTrackView() != null){
                getCurrentTrackView().onBuffering();
            }
    }

    private void setFavoriteStatus() {
        if (mPlayingTrack == null || mFavoriteButton == null) return;

        if (mPlayingTrack.user_favorite) {
            if (mFavoritedDrawable == null) mFavoritedDrawable = getResources().getDrawable(R.drawable.ic_liked_states);
            mFavoriteButton.setImageDrawable(mFavoritedDrawable);
        } else {
            if (mFavoriteDrawable == null) mFavoriteDrawable = getResources().getDrawable(R.drawable.ic_like_states);
            mFavoriteButton.setImageDrawable(mFavoriteDrawable);
        }
    }

    private PlayerTrackView getCurrentTrackView(){
        return ((PlayerTrackView) mTrackWorkspace.getScreenAt(mTrackWorkspace.getCurrentScreen()));
    }

    private PlayerTrackView getTrackView(int playPos){
        for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++){
            if (((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).getPlayPosition() == playPos) {
                return ((PlayerTrackView) mTrackWorkspace.getScreenAt(i));
            }
        }
        return null;
    }

    private PlayerTrackView getTrackViewById(long track_id) {
        for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++){
            if (((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).getTrackId() == track_id) {
                return ((PlayerTrackView) mTrackWorkspace.getScreenAt(i));
            }
        }
        return null;
    }
}
