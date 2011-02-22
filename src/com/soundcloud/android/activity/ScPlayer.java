
package com.soundcloud.android.activity;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.task.LoadCollectionTask;
import com.soundcloud.android.task.LoadDetailsTask;
import com.soundcloud.android.view.WaveformController;
import com.soundcloud.utils.AnimUtils;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Matrix;
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
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScPlayer extends ScActivity implements OnTouchListener {

    // Debugging tag.
    @SuppressWarnings("unused")
    private String TAG = "ScPlayer";

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    private boolean _isPlaying = false;

    private ImageButton mPauseButton;

    protected Boolean mLandscape;

    private ImageView mArtwork;

    private ImageButton mFavoriteButton;

    private ImageButton mInfoButton;

    private int mTouchSlop;

    private WaveformController mWaveformController;

    private Long mCurrentTrackId = null;

    private Track mPlayingTrack;
    private Track mFavoriteTrack;

    private ViewFlipper mTrackFlipper;

    private RelativeLayout mTrackInfo;

    private RelativeLayout mPlayableLayout;

    private FrameLayout mUnplayableLayout;

    private Boolean mCurrentTrackError = false;

    private BindResult mCurrentArtBindResult;

    private String mDurationFormatLong;

    private String mDurationFormatShort;

    private String mCurrentDurationString;

    private TextView mCurrentTime;

    private TextView mUserName;

    private TextView mTrackName;

    private long mDuration;

    private boolean paused;

    private boolean mTrackInfoFilled = false;
    
    private boolean mTrackInfoCommentsFilled = false;

    private LoadCommentsTask mLoadCommentsTask;

    private ArrayList<Comment> mCurrentComments;

    private RelativeLayout mContainer;

    private static final int REFRESH = 1;

    // ******************************************************************** //
    // Activity Lifecycle.
    // ******************************************************************** //

    /**
     * Called when the activity is starting. This is where most initialisation
     * should go: calling setContentView(int) to inflate the activity's UI, etc.
     * You can call finish() from within this function, in which case
     * onDestroy() will be immediately called without any of the rest of the
     * activity lifecycle executing. Derived classes must call through to the
     * super class's implementation of this method. If they do not, an exception
     * will be thrown.
     * 
     * @param icicle If the activity is being re-initialised after previously
     *            being shut down then this Bundle contains the data it most
     *            recently supplied@Override protected void onLayout(boolean
     *            changed, int l, int t, int r, int b) { Log.d("test",
     *            "In MainLayout.onLayout"); int childCount = getChildCount();
     *            for (int childIndex = 0; childIndex < childCount;
     *            childIndex++) { getChildAt(childIndex).setLayoutParams(new
     *            LayoutParams(100, 100, 100, 100)); } super.onLayout(changed,
     *            l, t, r, b); } in onSaveInstanceState(Bundle). Note: Otherwise
     *            it is null.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main_player);

        initControls();

        mDurationFormatLong = getString(R.string.durationformatlong);
        mDurationFormatShort = getString(R.string.durationformatshort);

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
                }
            });
            mShareButton.setVisibility(View.GONE);

            mArtwork = (ImageView) findViewById(R.id.artwork);
            mArtwork.setScaleType(ScaleType.CENTER_CROP);
            mArtwork.setImageDrawable(getResources().getDrawable(R.drawable.artwork_player));

            ImageButton mCommentsButton = (ImageButton) findViewById(R.id.btn_comment);
            mCommentsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    addNewComment(mPlayingTrack, -1);
                }
            });
            // setCommentButtonImage();

        } else {
            mContainer = (RelativeLayout) findViewById(R.id.container);
        }
    }

    public ViewGroup getCommentHolder() {
        return mContainer;
    }

    int mInitialX = -1;

    int mLastX = -1;

    int mTextWidth = 0;

    int mViewWidth = 0;

    boolean mDraggingLabel = false;

    /**
     * Right now, the only draggable ashtextview is the track, but this function
     * can be changed to easily add more later.
     * 
     * @param v
     * @return
     */
    TextView textViewForContainer(View v) {
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
                } else
                    mWaveformController.hideConnectingLayout();

                updateTrackInfo();
                setPauseButtonImage();
                long next = refreshNow();
                queueNextRefresh(next);
            } else {
                Intent intent = new Intent(this, Dashboard.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        } catch (RemoteException ignored) {
            Log.e(TAG, "error", ignored);
        }

    }

    @Override
    protected void onServiceUnbound() {
        super.onServiceUnbound();
    }

    @Override
    protected void onDataConnectionChanged(Boolean isConnected) {
        super.onDataConnectionChanged(isConnected);
        if (mPlayingTrack == null)
            return;

        if (mWaveformController.currentWaveformResult() == BindResult.ERROR) {
            mWaveformController.updateTrack(mPlayingTrack);
        }

        if (mCurrentArtBindResult == BindResult.ERROR) {
            updateArtwork();
        }
    }

    @Override
    public void onRefresh(boolean b) {
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
                // layout might be null if the text just changed, or ellipsizing
                // was just turned off
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
            if (mPlaybackService == null) {
                return;
            }
            try {
                mPlaybackService.next();
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
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
            mInfoButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_info_states));
        } else {
            mTrackFlipper.setInAnimation(AnimUtils.inFromLeftAnimation());
            mTrackFlipper.setOutAnimation(AnimUtils.outToRightAnimation());
            mTrackFlipper.showPrevious();
            mInfoButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_info_states));
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

    private void onDetailsResult(Boolean success) {
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

        if (mPlayingTrack.playback_count == null) {

            if (mTrackInfo.findViewById(R.id.loading_layout) != null)
                mTrackInfo.findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
            else
                mTrackInfo.findViewById(R.id.stub_loading).setVisibility(View.VISIBLE);

            mTrackInfo.findViewById(R.id.info_view).setVisibility(View.GONE);

            if (mTrackInfo.findViewById(android.R.id.empty) != null)
                mTrackInfo.findViewById(android.R.id.empty).setVisibility(View.GONE);

            if (mLoadTrackDetailsTask == null || CloudUtils.isTaskFinished(mLoadTrackDetailsTask)) {
                try {
                    mLoadTrackDetailsTask = newLoadTrackDetailsTask();
                    mLoadTrackDetailsTask.execute(getSoundCloudApplication().getRequest(
                            CloudAPI.Enddpoints.TRACK_DETAILS.replace("{track_id}",
                                    Long.toString(mPlayingTrack.id)), null));

                } catch (Exception e) {
                    Log.e(TAG, "error", e);
                }
            } else {
                if (mTrackInfo.findViewById(R.id.loading_layout) != null)
                    mTrackInfo.findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
            }
            return;
        }

        ((TextView) mTrackInfo.findViewById(R.id.txtPlays)).setText(mPlayingTrack.playback_count);
        ((TextView) mTrackInfo.findViewById(R.id.txtFavorites)).setText(Integer
                .toString(mPlayingTrack.favoritings_count));
        ((TextView) mTrackInfo.findViewById(R.id.txtDownloads)).setText(Integer
                .toString(mPlayingTrack.download_count));
        ((TextView) mTrackInfo.findViewById(R.id.txtComments)).setText(Integer
                .toString(mPlayingTrack.comment_count));

        ((TextView) mTrackInfo.findViewById(R.id.txtInfo)).setText(Html
                .fromHtml(generateTrackInfoString()));

        fillTrackInfoComments();
        
        mTrackInfoFilled = true;
    }
    
    private void fillTrackInfoComments(){
        if (mTrackInfo == null)
            return;
        
        
        LinearLayout commentsList;
        if (mTrackInfo.findViewById(R.id.comments_list) == null){
            commentsList = (LinearLayout) ((ViewStub) mTrackInfo.findViewById(R.id.stub_comments_list)).inflate();
            commentsList.findViewById(R.id.btn_info_comment).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    addNewComment(mPlayingTrack, -1);
                }
            });
        } else {
            commentsList = (LinearLayout) mTrackInfo.findViewById(R.id.comments_list);
            while (commentsList.getChildCount() > 1){
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
            
            commentText.append(comment.user.username).append(" ");
            spanEndIndex = commentText.length();
            commentText.setSpan(bss, 0, spanEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            if (comment.timestamp != 0)
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

    private String generateTrackInfoString() {
        String str = "";
        
        if (!TextUtils.isEmpty(mPlayingTrack.description)) 
                str+= mPlayingTrack.description + "<br /><br />";
        if (!TextUtils.isEmpty(mPlayingTrack.tag_list))
            str += mPlayingTrack.tag_list + "<br />";
        if (!TextUtils.isEmpty(mPlayingTrack.key_signature))
            str += mPlayingTrack.key_signature + "<br />";
        if (!TextUtils.isEmpty(mPlayingTrack.genre))
            str += mPlayingTrack.genre + "<br />";
        if (!(mPlayingTrack.bpm == null))
            str += mPlayingTrack.bpm + "<br />";
        str += "<br />";
        if (!TextUtils.isEmpty(mPlayingTrack.license)
                && !mPlayingTrack.license.toLowerCase().contentEquals("all rights reserved")
                && !mPlayingTrack.license.toLowerCase().contentEquals("all-rights-reserved"))
            str += mPlayingTrack.license + "<br /><br />";

        if (!TextUtils.isEmpty(mPlayingTrack.label_name)) {
            str += "<b>Released By</b><br />";
            str += mPlayingTrack.label_name + "<br />";
            if (!TextUtils.isEmpty(mPlayingTrack.release_year))
                str += mPlayingTrack.release_year + "<br />";
            str += "<br />";
        }

        return str;
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
        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {

        try {

            if (mPlaybackService == null)
                return 500;

            if (mPlaybackService.loadPercent() > 0 && !_isPlaying) {
                _isPlaying = true;
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
                // showToast(getString(R.string.toast_error_stream_died));
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

        if (mPlayingTrack == null){
            mWaveformController.clearTrack();
            return;
        }

        mWaveformController.updateTrack(mPlayingTrack);
        updateArtwork();
        if (mCurrentTrackId == null || mPlayingTrack.id.compareTo(mCurrentTrackId) != 0) {
            mWaveformController.clearTrack();

            mCurrentTrackId = mPlayingTrack.id;

            mCurrentComments = getSoundCloudApplication().getCommentsFromCache(mPlayingTrack.id);
            if (mCurrentComments != null){
              refreshComments(true);
            } else if (mLoadCommentsTask == null)
                startCommentLoading();
            else if (mLoadCommentsTask != null && mLoadCommentsTask.track_id != mCurrentTrackId) {
                mLoadCommentsTask.cancel(true);
                startCommentLoading();
            }

            mTrackName.setText(mPlayingTrack.title);
            mUserName.setText(mPlayingTrack.user.username);

            if (mTrackFlipper != null && mTrackFlipper.getDisplayedChild() == 1) {
                onTrackInfoFlip();
            }

            if (mCurrentTrackError)
                return;

            if (CloudUtils.isTrackPlayable(mPlayingTrack)) {
                hideUnplayable();
            } else {
                showUnplayable();
                mWaveformController.hideConnectingLayout();
            }

            mTrackInfoFilled = false;
            mTrackInfoCommentsFilled = false;
            
            setFavoriteStatus();
            mDuration = Long.parseLong(Integer.toString(mPlayingTrack.duration));
            mCurrentDurationString = CloudUtils.makeTimeString(
                    mDuration < 3600000 ? mDurationFormatShort : mDurationFormatLong,
                    mDuration / 1000);
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
                if (mCurrentTrackId == null || mPlayingTrack.id.compareTo(mCurrentTrackId) != 0
                        || mCurrentArtBindResult == BindResult.ERROR)
                    if ((mCurrentArtBindResult = ImageLoader.get(this).bind(
                            mArtwork,
                            CloudUtils.formatGraphicsUrl(mPlayingTrack.artwork_url,
                                    GraphicsSizes.t500), new ImageLoader.Callback() {
                                @Override
                                public void onImageError(ImageView view, String url, Throwable error) {
                                    mCurrentArtBindResult = BindResult.ERROR;
                                }

                                @Override
                                public void onImageLoaded(ImageView view, String url) {
                                    setArtworkMatrix();
                                }
                            })) != BindResult.OK){
                        mArtwork.setImageDrawable(getResources().getDrawable(
                                R.drawable.artwork_player));
                        mArtwork.setScaleType(ScaleType.CENTER_CROP);
                    }else
                        setArtworkMatrix();
            }
    }
    
    private void setArtworkMatrix(){
        if (mArtwork.getWidth() == 0)
            return;
        
        Matrix m = new Matrix();
        float scale = 1;
        if ( ((float)mArtwork.getWidth())/mArtwork.getHeight() > ((float) mArtwork.getDrawable().getMinimumWidth())/mArtwork.getDrawable().getMinimumHeight()){
            scale = ((float)mArtwork.getWidth())/((float) mArtwork.getDrawable().getMinimumWidth());
        } else {
            scale = ((float)mArtwork.getHeight())/((float) mArtwork.getDrawable().getMinimumHeight());
        }
        m.setScale(scale,scale);
        m.setTranslate((mArtwork.getWidth() - mArtwork.getDrawable().getMinimumHeight()*scale)/2, mArtwork.getDrawable().getMinimumHeight()*scale - mArtwork.getHeight());
        mArtwork.setScaleType(ScaleType.MATRIX);
    }

    public Boolean isSeekable() {
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

        paused = false;

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

        updateTrackInfo();
        setPauseButtonImage();

        long next = refreshNow();
        queueNextRefresh(next);
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

        state.putBoolean("paused", paused);
        state.putBoolean("currentTrackError", mCurrentTrackError);

        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        mCurrentTrackError = state.getBoolean("currentTrackError");
        paused = state.getBoolean("paused");
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
                    && ((ArrayList<Comment>) saved[3]).size() > 0
                    && !(mPlayingTrack != null && mPlayingTrack.id != ((ArrayList<Comment>) saved[3])
                    .get(0).id)) {
                mCurrentComments = (ArrayList<Comment>) saved[3];
                mWaveformController.setComments(mCurrentComments, false);
            }

        }
    }

    private LoadDetailsTask mLoadTrackDetailsTask;


    /**
     * Called when you are no longer visible to the user. You will next receive
     * either {@link #onStart}, {@link #onDestroy}, or nothing, depending on
     * later user activity.
     */
    @Override
    protected void onStop() {
        super.onStop();

        mWaveformController.onStop();
        paused = true;
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);
        mPlaybackService = null;

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

        mFavoriteTrack = mPlayingTrack;
        mFavoriteButton.setEnabled(false);
        try {
            if (mPlayingTrack.user_favorite) {
                    mPlaybackService.setFavoriteStatus(mPlayingTrack.id, false);
                mFavoriteTrack.user_favorite = false;
            } else {
                mPlaybackService.setFavoriteStatus(mPlayingTrack.id, true);
                mFavoriteTrack.user_favorite = true;
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
                mCurrentComments = newItems;
                getSoundCloudApplication().cacheComments(track_id, mCurrentComments);
                refreshComments(true);
            }

        }
    }
    
    private void refreshComments(boolean animateIn){
        mTrackInfoCommentsFilled = false;
        
        if (mTrackFlipper.getDisplayedChild() == 1)
            fillTrackInfoComments();
        
        mWaveformController.setComments(mCurrentComments, animateIn);
        
    }

    public void addNewComment(final Track track, final long timestamp) {
        final EditText input = new EditText(this);
        final AlertDialog commentDialog = new AlertDialog.Builder(ScPlayer.this)
                .setMessage(timestamp == -1 ? "Add an untimed comment" : "Add comment at " + CloudUtils.formatTimestamp(timestamp))
                .setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        sendComment(track.id,timestamp,input.getText().toString(),0);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).create();
        
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    commentDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        commentDialog.show();
    }
    
    private HttpResponse mAddCommentResult;
    private Comment mAddComment;
    private void sendComment(final long track_id, long timestamp, final String commentBody, long replyTo) {
        
        mAddComment = new Comment();
        mAddComment.track_id = track_id;
        mAddComment.created_at = new Date(System.currentTimeMillis());
        mAddComment.user_id = CloudUtils.getCurrentUserId(this);
        
        mAddComment.user = SoundCloudDB.getInstance().resolveUserById(this.getContentResolver(), mAddComment.user_id);
        mAddComment.timestamp = timestamp;
        mAddComment.body = commentBody;
        
        final List<NameValuePair> apiParams = new ArrayList<NameValuePair>();
        apiParams.add(new BasicNameValuePair("comment[body]", commentBody));
        if (timestamp > -1) apiParams.add(new BasicNameValuePair("comment[timestamp]", Long.toString(timestamp)));
        if (replyTo > 0) apiParams.add(new BasicNameValuePair("comment[reply_to]", Long.toString(replyTo)));

        
        // Fire off a thread to do some work that we shouldn't do directly in the UI thread
        Thread t = new Thread() {
            public void run() {
                try {
                    mAddCommentResult = getSoundCloudApplication().postContent(
                            CloudAPI.Enddpoints.TRACK_COMMENTS.replace("{track_id}", Long.toString(mAddComment.track_id)), apiParams);
                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                    ScPlayer.this.setException(e);
                }
                mHandler.post(mOnCommentAdd);
            }
        };
        t.start();
    }
    
 // Create runnable for posting
    final Runnable mOnCommentAdd = new Runnable() {
        public void run() {
            
            if (mAddCommentResult != null && mAddCommentResult.getStatusLine().getStatusCode() == 201){
                if (mAddComment.track_id != mPlayingTrack.id)
                    return;
                
                if (mCurrentComments == null)
                    mCurrentComments = new ArrayList<Comment>();
                
                mCurrentComments.add(mAddComment);
                getSoundCloudApplication().cacheComments(mPlayingTrack.id, mCurrentComments);
                refreshComments(true);
                
            } else {
                handleException();
            }
        }
    };
    
    

    public void replyToComment(final Comment comment) {
        final EditText input = new EditText(this);
        final AlertDialog commentDialog = new AlertDialog.Builder(ScPlayer.this)
                .setMessage("Reply to " + comment.user.username + " at "
                        + CloudUtils.formatTimestamp(comment.timestamp)).setView(input)
                .setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        sendComment(comment.track_id,comment.timestamp,input.getText().toString(),comment.id);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create();
        
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    commentDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        commentDialog.show();
    }

}
