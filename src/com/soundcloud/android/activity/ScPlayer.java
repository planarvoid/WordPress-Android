
package com.soundcloud.android.activity;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.google.android.imageloader.ImageLoader.ImageViewCallback;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.R;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.service.RemoteControlReceiver;
import com.soundcloud.android.task.AddCommentTask.AddCommentListener;
import com.soundcloud.android.task.LoadCollectionTask;
import com.soundcloud.android.task.LoadDetailsTask;
import com.soundcloud.android.view.WaveformController;
import com.soundcloud.utils.AnimUtils;

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
import android.os.Parcelable;
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

    private LoadCommentsTask mLoadCommentsTask;

    private ArrayList<Comment> mCurrentComments;

    private RelativeLayout mContainer;

    private int mInitialX = -1;
    private int mLastX = -1;
    private int mTextWidth = 0;
    private int mViewWidth = 0;
    private boolean mDraggingLabel = false;

    private LoadDetailsTask mLoadTrackDetailsTask;

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

        // mCommentsAdapter = createCommentsAdapter();
        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        if (!mLandscape) {

            ImageButton mProfileButton = (ImageButton) findViewById(R.id.btn_profile);
            if (mProfileButton == null)
                return;// failsafe for orientation check failure
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
                            CloudUtils.buildComment(ScPlayer.this, mPlayingTrack.id, -1, "", 0),
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
            if (mPlaybackService.getTrack() != null) {
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
                            CloudUtils.formatGraphicsUrl(mPlayingTrack.artwork_url, GraphicsSizes.badge)
                        :
                            CloudUtils.formatGraphicsUrl(mPlayingTrack.artwork_url, GraphicsSizes.small), null)) != BindResult.OK){
                    mInfoButton.setImageDrawable(getResources().getDrawable(
                            R.drawable.artwork_player_sm));
                }
            }
            else
                mInfoButton.setImageDrawable(getResources().getDrawable(R.drawable.artwork_player_sm));

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

    protected LoadDetailsTask newLoadTrackDetailsTask() {
        LoadDetailsTask lt = new LoadTrackDetailsTask();
        lt.loadModel = CloudUtils.Model.track;
        lt.setActivity(this);
        return lt;
    }

    protected class LoadTrackDetailsTask extends LoadDetailsTask {
        @Override
        protected void mapDetails(Parcelable update) {
            mPlayingTrack = (Track) update;
            fillTrackDetails();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            onDetailsResult(result);
        }
    }

    private void onDetailsResult(boolean success) {
        if (mTrackInfo.findViewById(R.id.loading_layout) != null) {
            mTrackInfo.findViewById(R.id.loading_layout).setVisibility(View.GONE);
        }

        if (!success) {
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
            if (mTrackInfo.findViewById(android.R.id.empty) != null) {
                mTrackInfo.findViewById(android.R.id.empty).setVisibility(View.GONE);
            }

            mTrackInfo.findViewById(R.id.info_view).setVisibility(View.VISIBLE);

        }
    }

    private void fillTrackDetails() {
        if (mPlayingTrack == null)
            return;

        if (mPlayingTrack.uri == null) {
            if (mTrackInfo.findViewById(R.id.loading_layout) != null) {
                mTrackInfo.findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
            } else {
                mTrackInfo.findViewById(R.id.stub_loading).setVisibility(View.VISIBLE);
            }

            mTrackInfo.findViewById(R.id.info_view).setVisibility(View.GONE);

            if (mTrackInfo.findViewById(android.R.id.empty) != null) {
                mTrackInfo.findViewById(android.R.id.empty).setVisibility(View.GONE);
            }

            if (mLoadTrackDetailsTask == null || CloudUtils.isTaskFinished(mLoadTrackDetailsTask)) {
                mLoadTrackDetailsTask = newLoadTrackDetailsTask();
                mLoadTrackDetailsTask.execute(getSoundCloudApplication().getRequest(
                        CloudAPI.Enddpoints.TRACK_DETAILS.replace("{track_id}",
                                Long.toString(mPlayingTrack.id)), null));
            } else {
                if (mTrackInfo.findViewById(R.id.loading_layout) != null)
                    mTrackInfo.findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
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
                            addNewComment(CloudUtils.buildComment(ScPlayer.this, mPlayingTrack.id,
                                    -1, "", 0), addCommentListener);
                        }
                    });
        } else {
            commentsList = (LinearLayout) mTrackInfo.findViewById(R.id.comments_list);
            while (commentsList.getChildCount() > 1) {
                commentsList.removeViewAt(1);
            }
        }


        if (mCurrentComments == null)
            return;

        //sort by created date descending for this list
        Collections.sort(mCurrentComments, new Comment.CompareCreatedAt());



        final SpannableStringBuilder commentText = new SpannableStringBuilder();
        final ForegroundColorSpan fcs = new ForegroundColorSpan(getResources().getColor(R.color.commentGray));
        final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);

        int spanStartIndex;
        int spanEndIndex;

        for (Comment comment : mCurrentComments){
            commentText.clear();

            View v = new View(this);
            v.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,1));
            v.setBackgroundColor(R.color.background_dark);
            commentsList.addView(v);

            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
            tv.setPadding(10, 5, 10, 5);
            //((LinearLayout.LayoutParams) tv.getLayoutParams()).
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
            commentText.append(" said ");

            long elapsed = (System.currentTimeMillis() - comment.created_at.getTime())/1000;

            if (elapsed < 60)
                commentText.append(getResources().getQuantityString(R.plurals.elapsed_seconds, (int) elapsed,(int) elapsed));
            else if (elapsed < 3600)
                commentText.append(getResources().getQuantityString(R.plurals.elapsed_minutes, (int) (elapsed/60),(int) (elapsed/60)));
            else if (elapsed < 86400)
                commentText.append(getResources().getQuantityString(R.plurals.elapsed_hours, (int) (elapsed/3600),(int) (elapsed/3600)));
            else if (elapsed < 2592000)
                commentText.append(getResources().getQuantityString(R.plurals.elapsed_days, (int) (elapsed/86400),(int) (elapsed/86400)));
            else if (elapsed < 31536000)
                commentText.append(getResources().getQuantityString(R.plurals.elapsed_months, (int) (elapsed/2592000),(int) (elapsed/2592000)));
            else
                commentText.append(getResources().getQuantityString(R.plurals.elapsed_years, (int) (elapsed/31536000),(int) (elapsed/31536000)));

            spanEndIndex = commentText.length();
            commentText.setSpan(fcs, spanStartIndex, spanEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            commentText.append("\n").append(comment.body);

            tv.setText(commentText);
            commentsList.addView(tv);
        }

        //restore default sort
        Collections.sort(mCurrentComments, new Comment.CompareTimestamp());

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
        if (mPlayingTrack == null || CloudUtils.isTrackPlayable(mPlayingTrack)) { // playback
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


        if (mPlaybackService != null) {
            try {
                if (mPlaybackService.getTrack() == null){
                    mWaveformController.clearTrack();
                    return;
                }

                if (mPlayingTrack == null || mPlayingTrack.id != mPlaybackService.getTrackId())
                    mPlayingTrack = mPlaybackService.getTrack();

            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }
        }

        if (mPlayingTrack == null) {
            mWaveformController.clearTrack();
            return;
        }

        mWaveformController.updateTrack(mPlayingTrack);
        updateArtwork();
        if (mPlayingTrack.id != mCurrentTrackId) {
            mWaveformController.clearTrack();
            mTrackInfoFilled = false;
            mTrackInfoCommentsFilled = false;
            mWaveformLoaded = false;

            mCurrentTrackId = mPlayingTrack.id;

            mCurrentComments = getSoundCloudApplication().getCommentsFromCache(mPlayingTrack.id);
            if (mCurrentComments != null){
              refreshComments(true);
            } else if (mLoadCommentsTask == null) {
                startCommentLoading();
            } else if (mLoadCommentsTask.track_id != mCurrentTrackId) {
                mLoadCommentsTask.cancel(true);
                startCommentLoading();
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

            if (CloudUtils.isTrackPlayable(mPlayingTrack)) {
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

    private void startCommentLoading() {
        mLoadCommentsTask = new LoadCommentsTask();
        mLoadCommentsTask.track_id = mPlayingTrack.id;
        mLoadCommentsTask.loadModel = CloudUtils.Model.comment;
        mLoadCommentsTask.pageSize = 50;
        mLoadCommentsTask.setContext(this);
        mLoadCommentsTask.execute(getSoundCloudApplication().getRequest(
                CloudAPI.Enddpoints.TRACK_COMMENTS.replace("{track_id}",
                        Long.toString(mPlayingTrack.id)), null));
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
                                    GraphicsSizes.t500), new ImageViewCallback() {
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

    /**
     * Called after {@link #onCreate} or {@link #onStop} when the current
     * activity is now being displayed to the user. It will be followed by
     * {@link #onRestart}.
     */
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
        this.registerReceiver(mStatusListener, new IntentFilter(f));
    }

    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user. This is a good
     * place to begin animations, open exclusive-access devices (such as the
     * camera), etc. Derived classes must call through to the super class's
     * implementation of this method. If they do not, an exception will be
     * thrown.
     */
    @Override
    protected void onResume() {
        tracker.trackPageView("/player");
        tracker.dispatch();

        super.onResume();

        registerRemoteControl(); // headphone remote events

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

    /**
     * Called to retrieve per-instance state from an activity before being
     * killed so that the state can be restored in onCreate(Bundle) or
     * onRestoreInstanceState(Bundle) (the Bundle populated by this method will
     * be passed to both).
     *
     * @param state A Bundle in which to place any state information you wish
     *            to save.
     */
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
        return new Object[] {
                mPlayingTrack, mLoadTrackDetailsTask, mLoadCommentsTask, mCurrentComments
        };
    }

     protected void restoreState() {

        // restore state
        Object[] saved = (Object[]) getLastNonConfigurationInstance();

        if (saved != null) {
            if (saved[0] != null)
                mPlayingTrack = (Track) saved[0];

            if (saved[1] != null) {
                mLoadTrackDetailsTask = (LoadDetailsTask) saved[1];
                mLoadTrackDetailsTask.setActivity(this);
                if (CloudUtils.isTaskPending(mLoadTrackDetailsTask))
                    mLoadTrackDetailsTask.execute();
            }

            if (saved[2] != null
                    && !(mPlayingTrack != null && mPlayingTrack.id != ((LoadCommentsTask) saved[2]).track_id)) {
                mLoadCommentsTask = (LoadCommentsTask) saved[2];
                mLoadCommentsTask.setContext(this);
                if (CloudUtils.isTaskPending(mLoadCommentsTask))
                    mLoadCommentsTask.execute();
            }

            if (saved[3] != null
                    && ((List) saved[3]).size() > 0
                    && !(mPlayingTrack != null
                    && mPlayingTrack.id != ((List<Comment>) saved[3]).get(0).id)) {
                mCurrentComments = (ArrayList<Comment>) saved[3];
            }
        }
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

    private class LoadCommentsTask extends LoadCollectionTask<Comment> {
        public long track_id;

        @Override
        protected void onPostExecute(Boolean keepGoing) {
            super.onPostExecute(keepGoing);

            if (newItems != null) {
                getSoundCloudApplication().cacheComments(track_id, newItems);

                if (track_id != mPlayingTrack.id)
                    return;

                mCurrentComments = newItems;
                refreshComments(true);
            }

        }
    }

    private void refreshComments(boolean animateIn){
        mTrackInfoCommentsFilled = false;
        if (mTrackFlipper.getDisplayedChild() == 1) fillTrackInfoComments();
        if (mLandscape) mWaveformController.setComments(mCurrentComments, animateIn);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
            doPauseResume();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    public AddCommentListener addCommentListener = new AddCommentListener(){
        @Override
        public void onCommentAdd(boolean success, Comment c) {
            if (c.track_id != mPlayingTrack.id || !success)
            return;

            if (mCurrentComments == null)
                mCurrentComments = new ArrayList<Comment>();

            mCurrentComments.add(c);
            getSoundCloudApplication().cacheComments(mPlayingTrack.id, mCurrentComments);
            refreshComments(true);

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

    private void registerRemoteControl() {
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
