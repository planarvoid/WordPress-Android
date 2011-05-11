
package com.soundcloud.android.activity;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.google.android.imageloader.ImageLoader.ImageViewCallback;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.service.RemoteControlReceiver;
import com.soundcloud.android.task.AddCommentTask.AddCommentListener;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.task.LoadTrackInfoTask;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.ClickSpan;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.CloudUtils.GraphicsSizes;
import com.soundcloud.android.view.WaveformController;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScPlayer extends ScActivity implements OnTouchListener {
    @SuppressWarnings("unused")
    private static final String TAG = "ScPlayer";
    private static final int REFRESH = 1;

    private boolean mIsPlaying = false;
    private ImageButton mPauseButton;
    private boolean mLandscape;

    private ImageView mArtwork;

    private ImageButton mFavoriteButton;
    private ImageButton mInfoButton;

    private int mTouchSlop;

    private WaveformController mWaveformController;

    private long mCurrentTrackId;

    private Track mPlayingTrack;

    private ViewFlipper mTrackFlipper;

    private RelativeLayout mTrackInfo;
    private RelativeLayout mPlayableLayout;

    private FrameLayout mUnplayableLayout;

    private boolean mCurrentTrackError;
    private BindResult mCurrentArtBindResult;

    private String mDurationFormatLong;
    private String mDurationFormatShort;
    private String mCurrentDurationString;

    private TextView mCurrentTime;
    private TextView mUserName;
    private TextView mTrackName;

    private long mDuration;

    private boolean mWaveformLoaded;

    private boolean mPaused;
    private boolean mTrackInfoFilled;
    private boolean mTrackInfoCommentsFilled;

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

    static {
        initializeRemoteControlRegistrationMethods();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main_player);

        initControls();

        mDurationFormatLong = getString(R.string.durationformatlong);
        mDurationFormatShort = getString(R.string.durationformatshort);

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mRemoteControlResponder = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());

        restoreState();
    }

    private void initControls() {
        mTrackFlipper = (ViewFlipper) findViewById(R.id.vfTrackInfo);

        mLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        mWaveformController = (WaveformController) findViewById(R.id.waveform_controller);
        mWaveformController.setLandscape(mLandscape);

        ProgressBar mProgress = (ProgressBar) findViewById(R.id.progress_bar);
        mProgress.setMax(1000);
        mProgress.setInterpolator(new AccelerateDecelerateInterpolator());

        mCurrentTime = (TextView) findViewById(R.id.currenttime);
        mUserName = (TextView) findViewById(R.id.user);
        mTrackName = (TextView) findViewById(R.id.track);

        View v = (View) mTrackName.getParent();
        v.setOnTouchListener(this);

        LinearLayout mTrackInfoBar = ((LinearLayout) findViewById(R.id.track_info_row));
        mTrackInfoBar.setBackgroundColor(getResources().getColor(R.color.playerControlBackground));

        mInfoButton = ((ImageButton) findViewById(R.id.btn_info));
        mInfoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onTrackInfoFlip();
            }
        });

        ImageButton mPrevButton = (ImageButton) findViewById(R.id.prev);
        mPrevButton.setOnClickListener(mPrevListener);
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnClickListener(mPauseListener);
        ImageButton mNextButton = (ImageButton) findViewById(R.id.next);
        mNextButton.setOnClickListener(mNextListener);

        mPlayableLayout = (RelativeLayout) findViewById(R.id.playable_layout);
        mUnplayableLayout = (FrameLayout) findViewById(R.id.unplayable_layout);

        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        if (!mLandscape) {
            ImageButton mProfileButton = (ImageButton) findViewById(R.id.btn_profile);
            if (mProfileButton == null) return;// failsafe for orientation check failure
            mProfileButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    if (mPlayingTrack == null) {
                        return;
                    }

                    Intent intent = new Intent(ScPlayer.this, UserBrowser.class);
                    intent.putExtra("userId", mPlayingTrack.user_id);
                    startActivity(intent);
                }
            });

            mFavoriteButton = (ImageButton) findViewById(R.id.btn_favorite);
            mFavoriteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleFavorite();
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
            mArtwork.setImageDrawable(getResources().getDrawable(R.drawable.artwork_player));

            ImageButton mCommentsButton = (ImageButton) findViewById(R.id.btn_comment);
            mCommentsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    addNewComment(
                            CloudUtils.buildComment(ScPlayer.this, getUserId(), mPlayingTrack.id, -1, "", 0),
                            addCommentListener);
                }
            });
        } else {
            mContainer = (RelativeLayout) findViewById(R.id.container);
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
        }
    }

    /**
     * Handle text dragging for viewing of long track names
     */
    public boolean onTouch(View v, MotionEvent event) {
        CloudUtils.dumpMotionEvent(event);
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
                        // scrolled the text completely off the view to the
                        // right
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

    Handler mLabelScroller = new Handler() {
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

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };

    private View.OnClickListener mPrevListener = new View.OnClickListener() {
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

    private View.OnClickListener mNextListener = new View.OnClickListener() {
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
                if (mPlaybackService.isPlaying()) {
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
        return (mTrackFlipper.getDisplayedChild() == 0);
    }

    @Override
    public void onRefresh(){
        mPlayingTrack.info_loaded = false;
        mPlayingTrack.comments_loaded = false;
        mPlayingTrack.comments = null;

        mTrackInfoFilled = false;
        mTrackInfoCommentsFilled = false;

        if (mTrackInfo != null){
            fillTrackInfoComments();
            fillTrackDetails();
        }

        refreshComments();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
             mTrackFlipper.getDisplayedChild() != 0) {
            onTrackInfoFlip();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void onTrackInfoFlip() {
        if (mTrackFlipper.getDisplayedChild() == 0) {
            mWaveformController.closeComment();

            if (mTrackInfo == null) {
                mTrackInfo = (RelativeLayout) ((ViewStub) findViewById(R.id.stub_info)).inflate();
            }

            if (!mTrackInfoFilled) {
                fillTrackDetails();
            } else if (!mTrackInfoCommentsFilled){
                fillTrackInfoComments();
            }

            mTrackFlipper.setInAnimation(AnimUtils.inFromRightAnimation());
            mTrackFlipper.setOutAnimation(AnimUtils.outToLeftAnimation());
            mTrackFlipper.showNext();
            if (mPlayingTrack.artwork_url != null){
                if ((mCurrentArtBindResult = ImageLoader.get(this).bind(
                        mInfoButton,
                        (getResources().getDisplayMetrics().density > 1) ?
                            CloudUtils.formatGraphicsUrl(mPlayingTrack.artwork_url, GraphicsSizes.BADGE)
                        :
                            CloudUtils.formatGraphicsUrl(mPlayingTrack.artwork_url, GraphicsSizes.SMALL), null)) != BindResult.OK){
                    mInfoButton.setImageDrawable(getResources().getDrawable(
                            R.drawable.artwork_player_sm));
                }
            }
            else {
                mInfoButton.setImageDrawable(getResources().getDrawable(R.drawable.artwork_player_sm));
            }

            mInfoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_info_artwork_states));
        } else {
            ImageLoader.get(this).unbind(mInfoButton);
            mTrackFlipper.setInAnimation(AnimUtils.inFromLeftAnimation());
            mTrackFlipper.setOutAnimation(AnimUtils.outToRightAnimation());
            mTrackFlipper.showPrevious();
            mInfoButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_info_states));
            mInfoButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.transparent_rect));
        }
    }

    public void onTrackInfoResult(long trackId, Track track) {
        if (trackId != mPlayingTrack.id)
            return;

        if (mTrackInfo.findViewById(R.id.loading_layout) != null) {
            mTrackInfo.findViewById(R.id.loading_layout).setVisibility(View.GONE);
        }

        if (track == null) {
            if (mTrackInfo.findViewById(android.R.id.empty) != null) {
                mTrackInfo.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            } else {
                mTrackInfo.addView(
                        CloudUtils.buildEmptyView(this,
                                getResources().getString(R.string.info_error)),
                        mTrackInfo.getChildCount() - 2);
            }
            mTrackInfo.findViewById(R.id.info_view).setVisibility(View.GONE);
        } else {
            mPlayingTrack = track;
            fillTrackDetails();

            if (mTrackInfo.findViewById(android.R.id.empty) != null) {
                mTrackInfo.findViewById(android.R.id.empty).setVisibility(View.GONE);
            }
            mTrackInfo.findViewById(R.id.info_view).setVisibility(View.VISIBLE);
        }
    }

    private void fillTrackDetails() {
        if (mPlayingTrack == null)
            return;

        if (!mPlayingTrack.info_loaded) {
            if (mTrackInfo.findViewById(R.id.loading_layout) != null) {
                mTrackInfo.findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
            } else {
                mTrackInfo.findViewById(R.id.stub_loading).setVisibility(View.VISIBLE);
            }

            mTrackInfo.findViewById(R.id.info_view).setVisibility(View.GONE);

            if (mTrackInfo.findViewById(android.R.id.empty) != null) {
                mTrackInfo.findViewById(android.R.id.empty).setVisibility(View.GONE);
            }

            if (mPlayingTrack.load_info_task == null || CloudUtils.isTaskFinished(mPlayingTrack.load_info_task))
                mPlayingTrack.load_info_task = new LoadTrackInfoTask(getSoundCloudApplication(), mPlayingTrack.id);

            mPlayingTrack.load_info_task.setActivity(this);
            if (CloudUtils.isTaskPending(mPlayingTrack.load_info_task)) {
                mPlayingTrack.load_info_task.execute(Request.to(Endpoints.TRACK_DETAILS, mPlayingTrack.id));
            }
        } else {
            ((TextView) mTrackInfo.findViewById(R.id.txtPlays)).setText(Integer.toString(mPlayingTrack.playback_count));
            ((TextView) mTrackInfo.findViewById(R.id.txtFavorites)).setText(Integer.toString(mPlayingTrack.favoritings_count));
            ((TextView) mTrackInfo.findViewById(R.id.txtDownloads)).setText(Integer.toString(mPlayingTrack.download_count));
            ((TextView) mTrackInfo.findViewById(R.id.txtComments)).setText(Integer.toString(mPlayingTrack.comment_count));

            TextView txtInfo = (TextView) mTrackInfo.findViewById(R.id.txtInfo);
            txtInfo.setText(Html.fromHtml(mPlayingTrack.trackInfo()));

            // for some reason this needs to be set to support links
            // http://www.mail-archive.com/android-beginners@googlegroups.com/msg04465.html
            MovementMethod mm = txtInfo.getMovementMethod();
            if (!(mm instanceof LinkMovementMethod)) {
                txtInfo.setMovementMethod(LinkMovementMethod.getInstance());
            }
            fillTrackInfoComments();
            mTrackInfoFilled = true;
        }
    }

    private void fillTrackInfoComments(){
        if (mTrackInfo == null)
            return;

        LinearLayout commentsList;
        if (mTrackInfo.findViewById(R.id.comments_list) == null) {
            commentsList = (LinearLayout) ((ViewStub) mTrackInfo
                    .findViewById(R.id.stub_comments_list)).inflate();
            commentsList.findViewById(R.id.btn_info_comment).setOnClickListener(
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addNewComment(CloudUtils.buildComment(ScPlayer.this, getUserId(), mPlayingTrack.id,
                                    -1, "", 0), addCommentListener);
                        }
                    });
        } else {
            commentsList = (LinearLayout) mTrackInfo.findViewById(R.id.comments_list);
            while (commentsList.getChildCount() > 1) {
                commentsList.removeViewAt(1);
            }
        }

        if (mPlayingTrack.comments == null)
            return;

        //sort by created date descending for this list
        Collections.sort(mPlayingTrack.comments, new Comment.CompareCreatedAt());

        final SpannableStringBuilder commentText = new SpannableStringBuilder();
        final ForegroundColorSpan fcs = new ForegroundColorSpan(getResources().getColor(R.color.commentGray));
        final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);

        int spanStartIndex;
        int spanEndIndex;

        for (final Comment comment : mPlayingTrack.comments){
            commentText.clear();

            View v = new View(this);
            v.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,1));
            v.setBackgroundColor(R.color.background_dark);
            commentsList.addView(v);

            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
            tv.setPadding(10, 5, 10, 5);
            tv.setTextSize(14);
            tv.setLineSpacing(5, 1);

            if (comment.user != null && comment.user.username != null) {
                commentText.append(comment.user.username).append(' ');
            }

            spanEndIndex = commentText.length();
            commentText.setSpan(bss, 0, spanEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (comment.timestamp > 0)
                commentText.append(" ").append(CloudUtils.formatTimestamp(comment.timestamp)).append(" ");

            spanStartIndex = commentText.length();
            commentText.append(" said ").append(CloudUtils.getTimeElapsed(getResources(), comment.created_at.getTime()));

            spanEndIndex = commentText.length();
            commentText.setSpan(fcs, spanStartIndex, spanEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            commentText.append("\n").append(comment.body);

            tv.setText(commentText);
            commentsList.addView(tv);

            if (comment.user != null && comment.user.username != null) {
                tv.setLinkTextColor(0xFF000000);
                CloudUtils.clickify(tv, comment.user.username, new ClickSpan.OnClickListener(){
                    @Override
                    public void onClick() {
                        Intent intent = new Intent(ScPlayer.this, UserBrowser.class);
                        intent.putExtra("userId", comment.user.id);
                        startActivity(intent);
                    }
                });
            }
        }
        //restore default sort
        Collections.sort(mPlayingTrack.comments, new Comment.CompareTimestamp());
    }

    private void setPauseButtonImage() {
        try {
            if (mPlaybackService != null && mPlaybackService.isPlaying()) {
                mPauseButton.setImageResource(R.drawable.ic_pause_states);
            } else {
                mPauseButton.setImageResource(R.drawable.ic_play_states);
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
                return 500;

            if (mPlaybackService.loadPercent() > 0 && !mIsPlaying) {
                mIsPlaying = true;
            }

            long pos = mPlaybackService.position();
            long remaining = 1000 - pos % 1000;

            if (pos >= 0 && mDuration > 0) {
                mCurrentTime.setText(CloudUtils.makeTimeString(pos < 3600000 ? mDurationFormatShort
                        : mDurationFormatLong, pos / 1000)
                        + " / " + mCurrentDurationString);
                mWaveformController.setProgress(pos);
                mWaveformController.setSecondaryProgress(mPlaybackService.loadPercent() * 10);
            } else {
                mCurrentTime.setText("--:--/--:--");
                mWaveformController.setProgress(0);
                mWaveformController.setSecondaryProgress(0);
            }

            // return the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            return remaining;

        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        return 500;
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

    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(CloudPlaybackService.META_CHANGED)) {
                mCurrentTrackError = false;

                updateTrackInfo();
                setPauseButtonImage();
                mWaveformController.showConnectingLayout();
                queueNextRefresh(1);
            } else if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)) {
                setPauseButtonImage();
            } else if (action.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
                if (intent.getBooleanExtra("isPlaying", false)) {
                    hideUnplayable();
                    updateTrackInfo();
                    mCurrentTrackError = false;
                }
            } else if (action.equals(CloudPlaybackService.FAVORITE_SET)) {
                if (mPlayingTrack != null && mPlayingTrack.id == intent.getLongExtra("id", -1)){
                    mPlayingTrack.user_favorite = intent.getBooleanExtra("isFavorite", false);
                    if (mFavoriteButton != null) mFavoriteButton.setEnabled(true);
                    setFavoriteStatus();
                }
            } else if (action.equals(CloudPlaybackService.INITIAL_BUFFERING)) {
                mCurrentTrackError = false;
                hideUnplayable();
                mWaveformController.showConnectingLayout();
            } else if (action.equals(CloudPlaybackService.BUFFERING)) {
                hideUnplayable();
                mWaveformController.showConnectingLayout();
            } else if (action.equals(CloudPlaybackService.BUFFERING_COMPLETE)) {
                // clearSeekVars();
                mWaveformController.hideConnectingLayout();
            } else if (action.equals(CloudPlaybackService.TRACK_ERROR)) {
                mCurrentTrackError = true;
                mWaveformController.hideConnectingLayout();
                showUnplayable();
            } else if (action.equals(CloudPlaybackService.STREAM_DIED)) {
                mWaveformController.hideConnectingLayout();
                setPauseButtonImage();
            } else if (action.equals(CloudPlaybackService.COMMENTS_LOADED)) {
                updateTrackInfo();
            } else if (action.equals(CloudPlaybackService.SEEK_COMPLETE)) {
                // setPauseButtonImage();
            }
        }
    };

    private void showUnplayable() {
        if (mPlayingTrack == null || mPlayingTrack.streamable) { // playback
            // error
            ((TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt))
                    .setText(R.string.player_error);
        } else {
            ((TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt))
                    .setText(R.string.player_not_streamable);
        }

        mPlayableLayout.setVisibility(View.GONE);
        mUnplayableLayout.setVisibility(View.VISIBLE);

    }

    private void hideUnplayable() {
        mPlayableLayout.setVisibility(View.VISIBLE);
        mUnplayableLayout.setVisibility(View.GONE);
    }

    private void updateTrackInfo() {
        if (mPlaybackService == null)
            return;

        try {
            long trackId = mPlaybackService.getTrackId();
            if (trackId == -1) {
                mPlayingTrack = null;
            } else if (mPlayingTrack == null || mPlayingTrack.id != trackId){

                Log.i(TAG,"Get Playing Track " + trackId);
                if (getSoundCloudApplication().getTrackFromCache(trackId) == null){

                    Track t = SoundCloudDB.getInstance().getTrackById(getContentResolver(), trackId, getUserId());
                    Log.i(TAG,"Get Playing Track from db " + t);
                    getSoundCloudApplication().cacheTrack(t != null ? t : mPlaybackService.getTrack());
                }

                mPlayingTrack = getSoundCloudApplication().getTrackFromCache(trackId);
            }

        } catch (RemoteException ignored) {}

        if (mPlayingTrack == null) {
            mWaveformController.clearTrack();
            return;
        }

        Log.i(TAG,"Got Playing Track " + mPlayingTrack + " " + mPlayingTrack.waveform_url);

        mWaveformController.updateTrack(mPlayingTrack);
        updateArtwork();

        if (mPlayingTrack.id != mCurrentTrackId) {
            mWaveformController.clearTrack();
            mTrackInfoFilled = false;
            mTrackInfoCommentsFilled = false;
            mWaveformLoaded = false;

            mCurrentTrackId = mPlayingTrack.id;

            if (mPlayingTrack.comments != null){
                setCurrentComments(true);
            } else {
                refreshComments();
            }

            mTrackName.setText(mPlayingTrack.title);
            mUserName.setText(mPlayingTrack.user.username);

            if (mTrackFlipper != null && mTrackFlipper.getDisplayedChild() == 1) {
                onTrackInfoFlip();
            }

            setFavoriteStatus();
            mDuration = mPlayingTrack.duration;
            mCurrentDurationString = CloudUtils.makeTimeString(
                    mDuration < 3600000 ? mDurationFormatShort : mDurationFormatLong,
                    mDuration / 1000);

            if (mCurrentTrackError)
                return;

            if (mPlayingTrack.streamable) {
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
        if (!mLandscape)
            if (TextUtils.isEmpty(mPlayingTrack.artwork_url)) {
                // no artwork
                ImageLoader.get(this).unbind(mArtwork);
                mArtwork.setScaleType(ScaleType.CENTER_CROP);
                mArtwork.setImageDrawable(getResources().getDrawable(R.drawable.artwork_player));
            } else {
                // load artwork as necessary
                if (mPlayingTrack.id != mCurrentTrackId || mCurrentArtBindResult == BindResult.ERROR) {
                    if ((mCurrentArtBindResult = ImageLoader.get(this).bind(
                            mArtwork,
                            CloudUtils.formatGraphicsUrl(mPlayingTrack.artwork_url,
                                    GraphicsSizes.T500), new ImageViewCallback() {
                                @Override
                                public void onImageError(ImageView view, String url, Throwable error) {
                                    mCurrentArtBindResult = BindResult.ERROR;
                                }

                                @Override
                                public void onImageLoaded(ImageView view, String url) {
                                    onArtworkSet();
                                }
                            })) != BindResult.OK) {
                        mArtwork.setImageDrawable(getResources().getDrawable(R.drawable.artwork_player));
                        mArtwork.setScaleType(ScaleType.CENTER_CROP);
                    }else
                        onArtworkSet();
                }
            }
    }

    private void onArtworkSet(){
        if (mArtwork.getWidth() == 0)
            return;

        Matrix m = new Matrix();
        float scale;
        if ( ((float)mArtwork.getWidth())/mArtwork.getHeight() > ((float) mArtwork.getDrawable().getMinimumWidth())/mArtwork.getDrawable().getMinimumHeight()){
            scale = ((float)mArtwork.getWidth())/((float) mArtwork.getDrawable().getMinimumWidth());
        } else {
            scale = ((float)mArtwork.getHeight())/((float) mArtwork.getDrawable().getMinimumHeight());
        }
        m.setScale(scale,scale);
        m.setTranslate((mArtwork.getWidth() - mArtwork.getDrawable().getMinimumHeight()*scale)/2, mArtwork.getDrawable().getMinimumHeight()*scale - mArtwork.getHeight());
        mArtwork.setScaleType(ScaleType.MATRIX);
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

    public void sendSeek() {
        try {
            if (mPlaybackService == null || !mPlaybackService.isSeekable()) {
                return;
            }

            mPlaybackService.seek(mSeekPos);
            mSeekPos = -1;
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        mPaused = false;
        getSoundCloudApplication().playerWaitForArtwork = true;

        IntentFilter f = new IntentFilter();
        f.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(CloudPlaybackService.META_CHANGED);
        f.addAction(CloudPlaybackService.TRACK_ERROR);
        f.addAction(CloudPlaybackService.STREAM_DIED);
        f.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        f.addAction(CloudPlaybackService.BUFFERING);
        f.addAction(CloudPlaybackService.BUFFERING_COMPLETE);
        f.addAction(CloudPlaybackService.COMMENTS_LOADED);
        f.addAction(CloudPlaybackService.SEEK_COMPLETE);
        f.addAction(CloudPlaybackService.FAVORITE_SET);
        registerReceiver(mStatusListener, new IntentFilter(f));
    }

    @Override
    protected void onResume() {
        pageTrack("/player");

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
        getSoundCloudApplication().playerWaitForArtwork = false;

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
        state.putBoolean("currentTrackError", mCurrentTrackError);

        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        mCurrentTrackError = state.getBoolean("currentTrackError");
        mPaused = state.getBoolean("paused");
        super.onRestoreInstanceState(state);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[] {mPlayingTrack};
    }

    protected void restoreState() {
        // restore state
        Object[] saved = (Object[]) getLastNonConfigurationInstance();
        if (saved != null && saved[0] != null) mPlayingTrack = (Track) saved[0];
    }

    private void setFavoriteStatus() {
        if (mPlayingTrack == null || mFavoriteButton == null) {
            return;
        }

        if (mPlayingTrack.user_favorite) {
            mFavoriteButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_favorited_states));
        } else {
            mFavoriteButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_favorite_states));
        }
    }

    private void toggleFavorite() {
        if (mPlayingTrack == null)
            return;

        Track track = mPlayingTrack;
        mFavoriteButton.setEnabled(false);
        try {
            if (mPlayingTrack.user_favorite) {
                    mPlaybackService.setFavoriteStatus(mPlayingTrack.id, false);
                track.user_favorite = false;
            } else {
                mPlaybackService.setFavoriteStatus(mPlayingTrack.id, true);
                track.user_favorite = true;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
            mFavoriteButton.setEnabled(true);
        }
        setFavoriteStatus();
    }

    public void refreshComments(){
        if (mPlayingTrack == null) return;

        if (mPlayingTrack.load_comments_task == null
                || CloudUtils.isTaskFinished(mPlayingTrack.load_comments_task))
            mPlayingTrack.load_comments_task = new LoadCommentsTask(
                    this.getSoundCloudApplication(), mPlayingTrack.id);

        mPlayingTrack.load_comments_task.setPlayer(this);

        if (CloudUtils.isTaskPending(mPlayingTrack.load_comments_task))
            mPlayingTrack.load_comments_task.execute();
    }

    public void onCommentsLoaded(long track_id, List<Comment> comments){
        if (track_id == mPlayingTrack.id) setCurrentComments(true);
    }

    private void setCurrentComments(boolean animateIn){
        mTrackInfoCommentsFilled = false;
        if (mTrackFlipper.getDisplayedChild() == 1) fillTrackInfoComments();
        if (mLandscape) mWaveformController.setComments(mPlayingTrack.comments, animateIn);
    }

    public AddCommentListener addCommentListener = new AddCommentListener(){
        @Override
        public void onCommentAdd(boolean success, Comment c) {
            if (c.track_id != mPlayingTrack.id || !success)
            return;

            if (mPlayingTrack.comments == null) mPlayingTrack.comments = new ArrayList<Comment>();

            mPlayingTrack.comments.add(c);
            getSoundCloudApplication().cacheTrack(mPlayingTrack);
            setCurrentComments(true);

        }

        @Override
        public void onException(Comment c, Exception e) {
            setException(e);
            handleException();
        }
    };

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
}
