
package com.soundcloud.android.activity;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.google.android.imageloader.ImageLoader.ImageViewCallback;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.service.RemoteControlReceiver;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.task.LoadTrackInfoTask;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.FlowLayout;
import com.soundcloud.android.view.PlayerTrackInfo;
import com.soundcloud.android.view.TrackInfoBar;
import com.soundcloud.android.view.WaveformController;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ScPlayer extends ScActivity implements OnTouchListener, LoadTrackInfoTask.LoadTrackInfoListener {
    private static final String TAG = "ScPlayer";
    private static final int REFRESH = 1;
    public static final int REFRESH_DELAY = 1000;

    private boolean mIsPlaying = false;
    private boolean mIsCommenting = false;
    private ImageButton mPauseButton;
    private boolean mLandscape;

    private ImageView mArtwork;
    private ImageButton mFavoriteButton;

    private int mTouchSlop;

    private WaveformController mWaveformController;
    private FrameLayout mUnplayableLayout;

    private long mCurrentTrackId;

    private Track mPlayingTrack;

    private ViewFlipper mTrackFlipper;

    private PlayerTrackInfo mTrackInfo;
    private FlowLayout mTrackTags;

    private int mCurrentTrackError;
    private BindResult mCurrentArtBindResult;
    private BindResult mCurrentAvatarBindResult;

    private String mCurrentDurationString;

    private TextView mFavoritersTxt;

    private long mDuration;

    private boolean mWaveformLoaded;

    private boolean mPaused;

    private RelativeLayout mContainer;

    private int mInitialX = -1;
    private int mLastX = -1;
    private int mTextWidth = 0;
    private int mViewWidth = 0;
    private boolean mDraggingLabel = false;

    private ComponentName mRemoteControlResponder;
    private AudioManager mAudioManager;

    private static Method mRegisterMediaButtonEventReceiver;
    private static Method mUnregisterMediaButtonEventReceiver;
    private Drawable mPlayState, mPauseState, mFavoriteDrawable, mFavoritedDrawable;

    private TrackInfoBar mTrackInfoBar;
    private ImageView mAvatar;
    private ImageButton mCommentButton;

    static {
        initializeRemoteControlRegistrationMethods();
    }

    private interface PlayerError {
        int PLAYBACK_ERROR = 0;
        int STREAM_ERROR = 1;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.sc_player);
        initControls();

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mRemoteControlResponder = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());

        mPauseState = getResources().getDrawable(R.drawable.ic_pause_states);
        mPlayState = getResources().getDrawable(R.drawable.ic_play_states);
        restoreState();
    }

    private void initControls() {
        mTrackInfoBar = (TrackInfoBar) findViewById(R.id.track_info_bar);
        mTrackFlipper = (ViewFlipper) findViewById(R.id.vfTrackInfo);

        mLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        mWaveformController = (WaveformController) findViewById(R.id.waveform_controller);

        ProgressBar mProgress = (ProgressBar) findViewById(R.id.progress_bar);
        mProgress.setMax(1000);
        mProgress.setInterpolator(new AccelerateDecelerateInterpolator());


        ((View) findViewById(R.id.track).getParent()).setOnTouchListener(this);

        mAvatar = (ImageView) findViewById(R.id.icon);
        mAvatar.setBackgroundDrawable(getResources().getDrawable(R.drawable.avatar_badge));
        mAvatar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mPlayingTrack != null) {
                    Intent intent = new Intent(ScPlayer.this, UserBrowser.class);
                    intent.putExtra("userId", mPlayingTrack.user_id);
                    startActivity(intent);
                }
            }
        });

        findViewById(R.id.private_indicator).setVisibility(View.GONE);

        ImageButton mPrevButton = (ImageButton) findViewById(R.id.prev);
        mPrevButton.setOnClickListener(mPrevListener);
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnClickListener(mPauseListener);
        ImageButton mNextButton = (ImageButton) findViewById(R.id.next);
        mNextButton.setOnClickListener(mNextListener);

        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        mFavoriteButton = (ImageButton) findViewById(R.id.btn_favorite);
        mFavoriteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleFavorite();
            }
        });

        mCommentButton = (ImageButton) findViewById(R.id.btn_comment);
        mCommentButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleCommentMode();
            }
        });

        if (!mLandscape) {
            findViewById(R.id.btn_info).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onTrackInfoFlip();
                }
            });

            ImageButton mShareButton = (ImageButton) findViewById(R.id.btn_share);
            mShareButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mPlayingTrack == null || !mPlayingTrack.sharing.contentEquals("public")) return;
                    Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mPlayingTrack.title + " by " + mPlayingTrack.user.username + " on SoundCloud");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, mPlayingTrack.permalink_url);
                    startActivity(Intent.createChooser(shareIntent, "Share: " + mPlayingTrack.title));
                }
            });

            mArtwork = (ImageView) findViewById(R.id.artwork);
            mArtwork.setScaleType(ScaleType.CENTER_CROP);
            mArtwork.setVisibility(View.INVISIBLE);
        }
        mContainer = (RelativeLayout) findViewById(R.id.container);
    }

    public void toggleCommentMode() {
        mIsCommenting = !mIsCommenting;
        mWaveformController.setCommentMode(mIsCommenting);
        if (mIsCommenting){
            mCommentButton.setImageResource(mLandscape ? R.drawable.ic_commenting_states : R.drawable.ic_commenting_states_v1);
        } else {
            mCommentButton.setImageResource(mLandscape ? R.drawable.ic_comment_states : R.drawable.ic_comment_states_v1);
        }
    }

    public ViewGroup getCommentHolder() {
        return mContainer;
    }

    private TextView textViewForContainer(View v) {
        View vv = v.findViewById(R.id.track);
        if (vv != null) {
            return (TextView) vv;
        }
        return null;
    }

    @Override
    protected void onServiceBound() {
        super.onServiceBound();

        try {
            if (mPlaybackService.getTrackId() != -1) {
                if (mPlaybackService.isBuffering()) {
                    mWaveformController.showConnectingLayout();
                } else {
                    mWaveformController.hideConnectingLayout();
                }

                updateTrackInfo();
                setPauseButtonImage();
                long next = refreshNow();
                queueNextRefresh(next);
            } else {
                Intent intent = new Intent(this, Main.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        } catch (RemoteException ignored) {
            Log.e(TAG, "error", ignored);
        }
    }

    @Override
    protected void onDataConnectionChanged(boolean isConnected) {
        super.onDataConnectionChanged(isConnected);
        if (mPlayingTrack != null) {
            if (mWaveformController.waveformResult == BindResult.ERROR) {
                mWaveformController.updateTrack(mPlayingTrack);
            }

            if (mCurrentArtBindResult == BindResult.ERROR) {
                updateArtwork();
            }
            if (mCurrentAvatarBindResult == BindResult.ERROR) {
                updateAvatar();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWaveformController != null) {
            mWaveformController.onDestroy();
        }
    }

    /**
     * Handle text dragging for viewing of long track names
     */
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        TextView tv = textViewForContainer(v);
        if (tv == null) {
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            mInitialX = mLastX = (int) event.getX();
            mDraggingLabel = false;
            return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mDraggingLabel) {
                Message msg = mLabelScroller.obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(msg, 1000);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mDraggingLabel) {
                int scrollx = tv.getScrollX();
                int x = (int) event.getX();
                int delta = mLastX - x;
                if (delta != 0) {
                    mLastX = x;
                    scrollx += delta;
                    if (scrollx > mTextWidth) {
                        // scrolled the text completely off the view to the left
                        scrollx -= mTextWidth;
                        scrollx -= mViewWidth;
                    }
                    if (scrollx < -mViewWidth) {
                        // scrolled the text completely off the view to the right
                        scrollx += mViewWidth;
                        scrollx += mTextWidth;
                    }
                    tv.scrollTo(scrollx, 0);
                }
                return true;
            }
            int delta = mInitialX - (int) event.getX();
            if (Math.abs(delta) > mTouchSlop) {
                // start moving
                mLabelScroller.removeMessages(0, tv);

                // Only turn ellipsizing off when it's not already off, because it
                // causes the scroll position to be reset to 0.
                if (tv.getEllipsize() != null) {
                    tv.setEllipsize(null);
                }
                Layout ll = tv.getLayout();
                // layout might be null if the text just changed, or ellipsizing was just turned off
                if (ll == null) {
                    return false;
                }
                // get the non-ellipsized line width, to determine whether
                // scrolling should even be allowed
                mTextWidth = (int) tv.getLayout().getLineWidth(0);
                mViewWidth = tv.getWidth();
                if (mViewWidth > mTextWidth) {
                    tv.setEllipsize(TruncateAt.END);
                    v.cancelLongPress();
                    return false;
                }
                mDraggingLabel = true;
                tv.setHorizontalFadingEdgeEnabled(true);
                v.cancelLongPress();
                return true;
            }
        }
        return false;
    }

    private final Handler mLabelScroller = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView tv = (TextView) msg.obj;
            int x = tv.getScrollX();
            x = x * 3 / 4;
            tv.scrollTo(x, 0);
            if (x == 0) {
                tv.setEllipsize(TruncateAt.END);
            } else {
                Message newmsg = obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(newmsg, 15);
            }
        }
    };

    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };

    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlaybackService == null) {
                return;
            }
            try {
                if (mPlaybackService.position() < 2000) {
                    mPlaybackService.prev();
                } else if (isSeekable()) {
                    mPlaybackService.seek(0);
                    // mService.play();
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
            if (mPlaybackService != null) {
                try {
                    mPlaybackService.next();
                } catch (RemoteException e) {
                    Log.e(TAG, "error", e);
                }
            }
        }
    };

    private void doPauseResume() {
        try {
            if (mPlaybackService != null) {
                if (mPlaybackService.isSupposedToBePlaying()) {
                    mPlaybackService.pause();
                } else {
                    mPlaybackService.play();
                    if (mWaveformLoaded) mPlaybackService.setClearToPlay(true);
                }
                long next = refreshNow();
                queueNextRefresh(next);
                setPauseButtonImage();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    public boolean waveformVisible(){
        return (mTrackFlipper == null || mTrackFlipper.getDisplayedChild() == 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mTrackFlipper != null && keyCode == KeyEvent.KEYCODE_BACK &&
             mTrackFlipper.getDisplayedChild() != 0) {
            onTrackInfoFlip();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void onTrackInfoFlip() {
        if (mTrackFlipper.getDisplayedChild() == 0) {
            mWaveformController.closeComment(false);

            if (mTrackInfo == null) {
                mTrackInfo = new PlayerTrackInfo(this);
                mTrackInfo.setPlayingTrack(mPlayingTrack);
                mTrackFlipper.addView(mTrackInfo);
            }

            if (!mTrackInfo.getIsTrackInfoFilled()) mTrackInfo.fillTrackDetails();
            if (!mTrackInfo.getIsTrackInfoCommentsFilled()) mTrackInfo.fillTrackInfoComments();


            mTrackFlipper.setInAnimation(AnimUtils.inFromRightAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.setOutAnimation(AnimUtils.outToLeftAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.showNext();
        } else {
            mTrackFlipper.setInAnimation(AnimUtils.inFromLeftAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.setOutAnimation(AnimUtils.outToRightAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.showPrevious();
        }
    }



    private void setPauseButtonImage() {
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
        if (!mPaused) {
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

            long pos = mPlaybackService.position();
            long remaining = REFRESH_DELAY - (pos % REFRESH_DELAY);

            if (pos >= 0 && mDuration > 0) {
                mWaveformController.setProgress(pos);
                mWaveformController.setSecondaryProgress(mPlaybackService.loadPercent() * 10);
            } else {
                mWaveformController.setProgress(0);
                mWaveformController.setSecondaryProgress(0);
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
                default:
                    break;
            }
        }
    };

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action.equals(CloudPlaybackService.META_CHANGED)) {
                mCurrentTrackError = -1;
                updateTrackInfo();
                setPauseButtonImage();
                mWaveformController.showConnectingLayout();
                queueNextRefresh(1);
            } else if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)) {
                setPauseButtonImage();
                mWaveformController.setPlaybackStatus(false, intent.getLongExtra("position", 0));
            } else if (action.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
                if (intent.getBooleanExtra("isSupposedToBePlaying", false)) {
                    hideUnplayable();
                    updateTrackInfo();
                    mCurrentTrackError = -1;
                } else {
                    mWaveformController.setPlaybackStatus(false, intent.getLongExtra("position", 0));
                }
            } else if (action.equals(CloudPlaybackService.FAVORITE_SET)) {
                if (mPlayingTrack != null && mPlayingTrack.id == intent.getLongExtra("id", -1)){
                    mPlayingTrack.user_favorite = intent.getBooleanExtra("isFavorite", false);
                    if (mFavoriteButton != null) mFavoriteButton.setEnabled(true);
                    updateTrackInfo();
                }
            } else if (action.equals(CloudPlaybackService.INITIAL_BUFFERING)) {
                mCurrentTrackError = -1;
                hideUnplayable();
                mWaveformController.showConnectingLayout();
            } else if (action.equals(CloudPlaybackService.BUFFERING)) {
                hideUnplayable();
                mWaveformController.showConnectingLayout();
            } else if (action.equals(CloudPlaybackService.BUFFERING_COMPLETE)) {
                mWaveformController.hideConnectingLayout();
                mWaveformController.setPlaybackStatus(intent.getBooleanExtra("isPlaying", false), intent.getLongExtra("position", 0));
            } else if (action.equals(CloudPlaybackService.PLAYBACK_ERROR)) {
                mCurrentTrackError = PlayerError.PLAYBACK_ERROR;
                mWaveformController.hideConnectingLayout();
                mWaveformController.setPlaybackStatus(intent.getBooleanExtra("isPlaying", false), intent.getLongExtra("position", 0));
                showUnplayable();
            } else if (action.equals(CloudPlaybackService.STREAM_DIED)) {
                mCurrentTrackError = PlayerError.STREAM_ERROR;
                mWaveformController.hideConnectingLayout();
                mWaveformController.setPlaybackStatus(intent.getBooleanExtra("isPlaying", false), intent.getLongExtra("position", 0));
                showUnplayable();
            } else if (action.equals(CloudPlaybackService.COMMENTS_LOADED)) {
                updateTrackInfo();
            } else if (action.equals(CloudPlaybackService.SEEK_COMPLETE)) {
                // setPauseButtonImage();
            }  else if (action.equals(Consts.IntentActions.COMMENT_ADDED)) {
                final Comment c = intent.getParcelableExtra("comment");
                if (c.track_id == mPlayingTrack.id) {
                    setCurrentComments(true);
                    mWaveformController.showNewComment(c);
                }
            }
        }
    };



    private void showUnplayable() {
        if (mUnplayableLayout == null) {
            mUnplayableLayout = (FrameLayout) ((ViewStub) findViewById(R.id.stub_unplayable_layout)).inflate();
        }

        if (mPlayingTrack == null || mPlayingTrack.isStreamable()) {
            ((TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt))
                    .setText(mCurrentTrackError == 0 ? R.string.player_error : R.string.player_stream_error);
        } else {
            ((TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt))
                    .setText(R.string.player_not_streamable);
        }

        mWaveformController.setVisibility(View.GONE);
        mUnplayableLayout.setVisibility(View.VISIBLE);

    }

    private void hideUnplayable() {
        mWaveformController.setVisibility(View.VISIBLE);
        if (mUnplayableLayout != null) mUnplayableLayout.setVisibility(View.GONE);
    }

    private void updateTrackInfo() {
        if (mPlaybackService == null)
            return;

        try {
            long trackId = mPlaybackService.getTrackId();
            if (trackId == -1) {
                mPlayingTrack = null;
            } else {
                if (getApp().getTrackFromCache(trackId) == null) {
                    Track t = SoundCloudDB.getTrackById(getContentResolver(), trackId, getCurrentUserId());
                    getApp().cacheTrack(t != null ? t : mPlaybackService.getTrack());
                }

                mPlayingTrack = getApp().getTrackFromCache(trackId);
            }
        } catch (RemoteException ignored) {}

        if (mPlayingTrack == null || mPlayingTrack.id != mCurrentTrackId) {
            mWaveformController.clearTrack();
            if (mPlayingTrack == null) return;
        }

        mWaveformController.updateTrack(mPlayingTrack);
        try {
            mWaveformController.setPlaybackStatus(mPlaybackService.isPlaying(),mPlaybackService.position());
        } catch (RemoteException ignored) {}


        mTrackInfoBar.display(mPlayingTrack,false,-1, true);
        if (mTrackInfo != null) mTrackInfo.setPlayingTrack(mPlayingTrack);
        updateArtwork();
        updateAvatar();

        if (mDuration != mPlayingTrack.duration) {
            mDuration = mPlayingTrack.duration;
            if (mDuration != 0) {
                mCurrentDurationString = CloudUtils.formatTimestamp(mDuration);
            }
        }

        setFavoriteStatus();

        if (!mPlayingTrack.info_loaded) {
              if (CloudUtils.isTaskFinished(mPlayingTrack.load_info_task)){
                mPlayingTrack.load_info_task = new LoadTrackInfoTask(getApp(), mPlayingTrack.id, true, true);
              }

            mPlayingTrack.load_info_task.setListener(this);
            if (CloudUtils.isTaskPending(mPlayingTrack.load_info_task)) {
                mPlayingTrack.load_info_task.execute(Request.to(Endpoints.TRACK_DETAILS, mPlayingTrack.id));
            }
        }

        if (mPlayingTrack.id != mCurrentTrackId) {
            mWaveformController.clearTrack();
            mCurrentTrackId = mPlayingTrack.id;

            if (mPlayingTrack.user != null && TextUtils.isEmpty(mPlayingTrack.user.username)){
                trackPage(mPlayingTrack.pageTrack());
            }


            if (mPlayingTrack.comments != null) {
                setCurrentComments(true);
            } else {
                refreshComments();
            }

            if (mTrackFlipper != null && mTrackFlipper.getDisplayedChild() == 1) {
                onTrackInfoFlip();
            }

            if (mCurrentTrackError >= 0)
                return;

            if (mPlayingTrack.isStreamable()) {
                hideUnplayable();
            } else {
                showUnplayable();
                mWaveformController.hideConnectingLayout();
            }
        }
    }



    public void onWaveformLoaded(){
        mWaveformLoaded = true;

        try {
            if (mPlaybackService != null) mPlaybackService.setClearToPlay(true);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    private void updateArtwork() {
        if (!mLandscape) {
            if (TextUtils.isEmpty(mPlayingTrack.artwork_url)) {
                // no artwork
                ImageLoader.get(this).unbind(mArtwork);
                mArtwork.setVisibility(View.INVISIBLE);
            } else {
                // load artwork as necessary
                if (mPlayingTrack.id != mCurrentTrackId || mCurrentArtBindResult == BindResult.ERROR) {
                    if ((mCurrentArtBindResult = ImageUtils.loadImageSubstitute(
                            this,
                            mArtwork,
                            mPlayingTrack.artwork_url,
                            Consts.GraphicSize.T500, new ImageViewCallback() {
                        @Override
                        public void onImageError(ImageView view, String url, Throwable error) {
                            mCurrentArtBindResult = BindResult.ERROR;
                            Log.e(TAG,"Error loading artwork " + error);
                        }

                        @Override
                        public void onImageLoaded(ImageView view, String url) {
                            onArtworkSet();
                        }
                    }, null)) != BindResult.OK) {
                        mArtwork.setVisibility(View.INVISIBLE);
                    } else {
                        onArtworkSet();
                    }
                }
            }
        }
    }

    private void updateAvatar() {
        if (mPlayingTrack.hasAvatar()) {
            // load artwork as necessary
            if (mPlayingTrack.id != mCurrentTrackId || mCurrentAvatarBindResult == BindResult.ERROR) {
                if ((mCurrentAvatarBindResult = ImageLoader.get(this).bind(
                        mAvatar,
                        ImageUtils.formatGraphicsUriForList(this, mPlayingTrack.user.avatar_url),
                        new ImageViewCallback() {
                            @Override
                            public void onImageError(ImageView view, String url, Throwable error) {
                                mCurrentAvatarBindResult = BindResult.ERROR;
                            }

                            @Override
                            public void onImageLoaded(ImageView view, String url) {
                            }
                        })) != BindResult.OK) {
                }
            }
        } else {
            // no artwork
            ImageLoader.get(this).unbind(mAvatar);
        }
    }

    private void onArtworkSet(){
        if (mArtwork.getVisibility() == View.INVISIBLE || mArtwork.getVisibility() == View.GONE) {
            AnimUtils.runFadeInAnimationOn(this, mArtwork);
            mArtwork.setVisibility(View.VISIBLE);
        }

    }

    public boolean isSeekable() {
        try {
            return !(mPlaybackService == null || !mPlaybackService.isSeekable());
        } catch (RemoteException e) {
            return false;
        }
    }

    private long mSeekPos = -1;

    private long mLastSeekEventTime = -1;

    public long setSeekMarker(float seekPercent) {
        try {
            if (mPlaybackService != null) {

                if (!mPlaybackService.isSeekable()) {
                    mSeekPos = -1;
                    return mPlaybackService.position();
                } else {
                    if (mPlayingTrack != null) {
                        long now = SystemClock.elapsedRealtime();
                        if ((now - mLastSeekEventTime) > 250) {
                            mLastSeekEventTime = now;
                            try {
                                mSeekPos = mPlaybackService.seek((long) (mPlayingTrack.duration * seekPercent));
                            } catch (RemoteException e) {
                                Log.e(TAG, "error", e);
                            }
                        } else {
                            // where would we be if we had seeked
                            mSeekPos = mPlaybackService.getSeekResult((long) (mPlayingTrack.duration * seekPercent));
                        }
                        return mSeekPos;
                    }
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        return 0;
    }

    public void sendSeek(float seekPercent) {
        try {
            if (mPlaybackService == null || !mPlaybackService.isSeekable()) {
                return;
            }

            mPlaybackService.seek((long) (mPlayingTrack.duration * seekPercent));
            mSeekPos = -1;
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        mPaused = false;
        getApp().playerWaitForArtwork = true;

        IntentFilter f = new IntentFilter();
        f.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(CloudPlaybackService.META_CHANGED);
        f.addAction(CloudPlaybackService.PLAYBACK_ERROR);
        f.addAction(CloudPlaybackService.STREAM_DIED);
        f.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        f.addAction(CloudPlaybackService.BUFFERING);
        f.addAction(CloudPlaybackService.INITIAL_BUFFERING);
        f.addAction(CloudPlaybackService.BUFFERING_COMPLETE);
        f.addAction(CloudPlaybackService.COMMENTS_LOADED);
        f.addAction(CloudPlaybackService.SEEK_COMPLETE);
        f.addAction(CloudPlaybackService.FAVORITE_SET);
        f.addAction(Consts.IntentActions.COMMENT_ADDED);
        registerReceiver(mStatusListener, new IntentFilter(f));
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerHeadphoneRemoteControl();

        updateTrackInfo();
        setPauseButtonImage();

        long next = refreshNow();
        queueNextRefresh(next);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // no longer have to wait for artwork to load
        try {
            if (mPlaybackService != null) mPlaybackService.setClearToPlay(true);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        getApp().playerWaitForArtwork = false;

        mWaveformController.onStop();
        mPaused = true;
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);
        mPlaybackService = null;
    }


    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        state.putBoolean("paused", mPaused);
        state.putInt("currentTrackError", mCurrentTrackError);

        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        mCurrentTrackError = state.getInt("currentTrackError");
        mPaused = state.getBoolean("paused");
        super.onRestoreInstanceState(state);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[] {mPlayingTrack};
    }

    void restoreState() {
        // restore state
        Object[] saved = (Object[]) getLastNonConfigurationInstance();
        if (saved != null && saved[0] != null) mPlayingTrack = (Track) saved[0];
    }

    private void setFavoriteStatus() {
        if (mPlayingTrack == null || mFavoriteButton == null) {
            return;
        }

        if (mPlayingTrack.user_favorite) {
            if (mFavoritedDrawable == null) mFavoritedDrawable = getResources().getDrawable(
                    mLandscape ? R.drawable.ic_liked_states : R.drawable.ic_liked_states_v1);
            mFavoriteButton.setImageDrawable(mFavoritedDrawable);
        } else {
            if (mFavoriteDrawable == null) mFavoriteDrawable = getResources().getDrawable(
                    mLandscape ? R.drawable.ic_like_states : R.drawable.ic_like_states_v1);
            mFavoriteButton.setImageDrawable(mFavoriteDrawable);
        }
    }

    private void toggleFavorite() {
        if (mPlayingTrack == null)
            return;

        mFavoriteButton.setEnabled(false);
        try {
            if (mPlayingTrack.user_favorite) {
                    mPlaybackService.setFavoriteStatus(mPlayingTrack.id, false);
            } else {
                mPlaybackService.setFavoriteStatus(mPlayingTrack.id, true);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
            mFavoriteButton.setEnabled(true);
        }
    }

    void refreshComments() {
        if (mPlayingTrack == null) return;
        if (CloudUtils.isTaskFinished(mPlayingTrack.load_comments_task)) {
            mPlayingTrack.load_comments_task =
                    new LoadCommentsTask(getApp(), mPlayingTrack.id);
        }
        mPlayingTrack.load_comments_task.setPlayer(this);

        if (CloudUtils.isTaskPending(mPlayingTrack.load_comments_task)) {
            mPlayingTrack.load_comments_task.execute((Request) null);
        }
    }

    /** @noinspection UnusedParameters*/
    public void onCommentsLoaded(long track_id, List<Comment> comments){
        if (track_id == mPlayingTrack.id) setCurrentComments(true);
    }

    private void setCurrentComments(boolean animateIn){
        mWaveformController.setComments(mPlayingTrack.comments, animateIn);
        if (mTrackInfo != null) {
            mTrackInfo.clearIsTrackInfoCommentsFilled();
            if (!mLandscape && mTrackFlipper != null && mTrackFlipper.getDisplayedChild() == 1) {
                mTrackInfo.fillTrackInfoComments();
            }
        }

    }

    // http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
    private static void initializeRemoteControlRegistrationMethods() {
        try {
            if (mRegisterMediaButtonEventReceiver == null) {
                mRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "registerMediaButtonEventReceiver",
                        new Class[] { ComponentName.class });
            }
            if (mUnregisterMediaButtonEventReceiver == null) {
                mUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "unregisterMediaButtonEventReceiver",
                        new Class[] { ComponentName.class });
            }
        } catch (NoSuchMethodException ignored) {
            // Android < 2.2
        }
    }

    private void registerHeadphoneRemoteControl() {
        if (mRegisterMediaButtonEventReceiver == null) return;

        try {
            mRegisterMediaButtonEventReceiver.invoke(mAudioManager, mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(TAG, "unexpected", ie);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void unregisterRemoteControl() {
        if (mUnregisterMediaButtonEventReceiver == null) return;

        try {
            mUnregisterMediaButtonEventReceiver.invoke(mAudioManager, mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(TAG, "unexpected", ie);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.REFRESH:
                mPlayingTrack.info_loaded = false;
                mPlayingTrack.comments_loaded = false;
                mPlayingTrack.comments = null;

                if (mTrackInfo != null) {
                    mTrackInfo.clearIsTrackInfoFilled();
                    mTrackInfo.clearIsTrackInfoCommentsFilled();
                    mTrackInfo.fillTrackDetails();
                    mTrackInfo.fillTrackInfoComments();
                }
                refreshComments();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onTrackInfoLoaded(Track track, String action) {
        if (track.id != mPlayingTrack.id) return;
        updateTrackInfo();
        if (mTrackInfo != null) {
            mTrackInfo.onInfoLoadSuccess();
        }
    }

    @Override
    public void onTrackInfoError(long trackId) {
        if (trackId != mPlayingTrack.id) return;
        if (mTrackInfo != null){
            mTrackInfo.onInfoLoadError();
        }
    }
}
