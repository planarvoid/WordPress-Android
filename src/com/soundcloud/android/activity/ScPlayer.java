
package com.soundcloud.android.activity;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
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
import android.widget.Button;
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
import java.lang.ref.SoftReference;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScPlayer extends LazyActivity implements OnTouchListener {

    // Debugging tag.
    @SuppressWarnings("unused")
    private String TAG = "ScPlayer";

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    private boolean _isPlaying = false;

    private ImageButton mPrevButton;

    private ImageButton mPauseButton;

    private ImageButton mNextButton;

    protected Boolean mLandscape;

    private ImageView mArtwork;

    private ImageButton mProfileButton;

    private ImageButton mFavoriteButton;

    private ImageButton mCommentsButton;

    private ImageButton mShareButton;

    private ImageButton mInfoButton;

    private int mTouchSlop;

    private WaveformController mWaveformController;

    private Long mCurrentTrackId = null;

    private Track mPlayingTrack;

    private Track[] mEnqueueList;

    private int mEnqueuePosition;

    private LinearLayout mTrackInfoBar;

    private ViewFlipper mTrackFlipper;

    private RelativeLayout mTrackInfo;

    private RelativeLayout mPlayableLayout;

    private FrameLayout mUnplayableLayout;

    private Boolean mCurrentTrackError = false;

    private BindResult mCurrentArtBindResult;

    protected ArrayList<Parcelable> mThreadData;

    protected ArrayList<ArrayList<Parcelable>> mCommentData;

    protected String[] mFrom;

    protected int[] mTo;

    private String mDurationFormatLong;

    private String mDurationFormatShort;

    private String mCurrentDurationString;

    private TextView mCurrentTime;

    private TextView mUserName;

    private TextView mTrackName;

    private ProgressBar mProgress;

    private long mDuration;

    private boolean paused;

    private boolean mTrackInfoFilled = false;
    
    private boolean mTrackInfoCommentsFilled = false;

    private LoadCommentsTask mLoadCommentsTask;

    private ArrayList<Comment> mCurrentComments;

    private RelativeLayout mContainer;

    private static final int REFRESH = 1;

    private static final int QUIT = 2;

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

        super.onCreate(icicle, R.layout.main_player);

        mMainHolder = (LinearLayout) findViewById(R.id.main_holder);
        /*
         * Intent intent = getIntent(); Bundle extras = intent.getExtras(); if
         * (extras != null){ Log.i(TAG,"Setting track id to " +
         * extras.getString("trackId")); mPlayTrackId =
         * extras.getString("trackId"); }
         */

        initControls();

        mDurationFormatLong = getString(R.string.durationformatlong);
        mDurationFormatShort = getString(R.string.durationformatshort);

    }

    private void initControls() {

        mTrackFlipper = (ViewFlipper) findViewById(R.id.vfTrackInfo);

        mLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        mWaveformController = (WaveformController) findViewById(R.id.waveform_controller);
        mWaveformController.setLandscape(mLandscape);

        mProgress = (ProgressBar) findViewById(R.id.progress_bar);
        mProgress.setMax(1000);
        mProgress.setInterpolator(new AccelerateDecelerateInterpolator());

        mCurrentTime = (TextView) findViewById(R.id.currenttime);
        mUserName = (TextView) findViewById(R.id.user);
        mTrackName = (TextView) findViewById(R.id.track);

        View v = (View) mTrackName.getParent();
        v.setOnTouchListener(this);

        mTrackInfoBar = ((LinearLayout) findViewById(R.id.track_info_row));
        mTrackInfoBar.setBackgroundColor(getResources().getColor(R.color.playerControlBackground));

        mInfoButton = ((ImageButton) findViewById(R.id.btn_info));
        mInfoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onTrackInfoFlip();
            }
        });

        mPrevButton = (ImageButton) findViewById(R.id.prev);
        mPrevButton.setOnClickListener(mPrevListener);
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnClickListener(mPauseListener);
        mNextButton = (ImageButton) findViewById(R.id.next);
        mNextButton.setOnClickListener(mNextListener);

        mPlayableLayout = (RelativeLayout) findViewById(R.id.playable_layout);
        mUnplayableLayout = (FrameLayout) findViewById(R.id.unplayable_layout);

        // mCommentsAdapter = createCommentsAdapter();
        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        if (!mLandscape) {

            mProfileButton = (ImageButton) findViewById(R.id.btn_profile);
            if (mProfileButton == null)
                return;// failsafe for orientation check failure
            mProfileButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    if (mPlayingTrack == null) {
                        return;
                    }

                    Intent intent = new Intent(ScPlayer.this, ScProfile.class);
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

            mShareButton = (ImageButton) findViewById(R.id.btn_share);
            mShareButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                }
            });
            mShareButton.setVisibility(View.GONE);

            mArtwork = (ImageView) findViewById(R.id.artwork);
            mArtwork.setScaleType(ScaleType.CENTER_CROP);
            mArtwork.setImageDrawable(getResources().getDrawable(R.drawable.artwork_player));

            mCommentsButton = (ImageButton) findViewById(R.id.btn_comment);
            mCommentsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    addNewComment(mPlayingTrack,-1);
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
     * Right now, the only draggable textview is the track, but this function
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
            if (mService.getTrack() != null) {
                if (mService.isBuffering()) {
                    mWaveformController.showConnectingLayout();
                } else
                    mWaveformController.hideConnectingLayout();

                updateTrackInfo();
                setPauseButtonImage();
                long next = refreshNow();
                queueNextRefresh(next);
                return;
            } else {
                Intent intent = new Intent(this, Dashboard.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        } catch (RemoteException ex) {
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

                // Only turn ellipsizing off when it's not already off, because
                // it
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
                // scrolling
                // should even be allowed
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
            if (mService == null) {
                return;
            }
            try {
                if (mService.position() < 2000) {
                    mService.prev();
                } else if (isSeekable()) {
                    mService.seek(0);
                    // mService.play();
                } else {
                    mService.restart();
                }
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }
    };

    private View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mService == null) {
                return;
            }
            try {
                mService.next();
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }
    };

    private void doPauseResume() {
        try {
            if (mService != null) {
                if (mService.isPlaying()) {
                    mService.pause();
                } else {
                    mService.play();
                }
                long next = refreshNow();
                queueNextRefresh(next);
                setPauseButtonImage();
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
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
                    mLoadTrackDetailsTask.execute(getSoundCloudApplication().getPreparedRequest(
                            CloudAPI.Enddpoints.TRACK_DETAILS.replace("{track_id}",
                                    Long.toString(mPlayingTrack.id))));

                } catch (Exception e) {
                    e.printStackTrace();
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
            ((Button) commentsList.findViewById(R.id.btn_info_comment)).setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    addNewComment(mPlayingTrack,-1);                    
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

        /*
         * temporary commenting out created_at until I figure out how to do the
         * formatting properly try { str += "<b>Uploaded " +
         * mPlayingTrack.getData(Track.key_created_at)+"<br />";
         * SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
         * Date uploadDate = new
         * Date(sdf.parse(mPlayingTrack.getData(Track.key_created_at
         * ).substring(0
         * ,mPlayingTrack.getData(Track.key_created_at).indexOf("+")
         * -1)).getTime()); str += "<b>Uploaded " +
         * uploadDate.getDate()+"<br />"; } catch (ParseException e) { // TODO
         * Auto-generated catch block e.printStackTrace(); }
         */

        return str;
    }

    private void setPauseButtonImage() {
        try {
            if (mService != null && mService.isPlaying()) {
                mPauseButton.setImageResource(R.drawable.ic_pause_states);
            } else {
                mPauseButton.setImageResource(R.drawable.ic_play_states);
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
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

            if (mService == null)
                return 500;

            if (mService.loadPercent() > 0 && !_isPlaying) {
                _isPlaying = true;
            }

            long pos = mService.position();
            long remaining = 1000 - pos % 1000;

            if (pos >= 0 && mDuration > 0) {
                mCurrentTime.setText(CloudUtils.makeTimeString(pos < 3600000 ? mDurationFormatShort
                        : mDurationFormatLong, pos / 1000)
                        + " / " + mCurrentDurationString);
                mWaveformController.setProgress(pos);
                mWaveformController.setSecondaryProgress(mService.loadPercent() * 10);
            } else {
                mCurrentTime.setText("--:--/--:--");
                mWaveformController.setProgress(0);
                mWaveformController.setSecondaryProgress(0);
            }

            // return the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            return remaining;

        } catch (RemoteException ex) {
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

        if (mService != null) {
            try {
                if (mService.getTrack() == null){
                    mWaveformController.clearTrack();
                    return;
                }

                if (mPlayingTrack == null || mPlayingTrack.id != mService.getTrackId())
                    mPlayingTrack = mService.getTrack();

            } catch (RemoteException e) {
                e.printStackTrace();
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
        mLoadCommentsTask.execute(getSoundCloudApplication().getPreparedRequest(
                CloudAPI.Enddpoints.TRACK_COMMENTS.replace("{track_id}",
                        Long.toString(mPlayingTrack.id))));
    }

    private void updateArtwork() {
        if (!mLandscape)
            if (TextUtils.isEmpty(mPlayingTrack.artwork_url)) {
                // no artwork
                ImageLoader.get(this).unbind(mArtwork);
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
                                }
                            })) != BindResult.OK)
                        mArtwork.setImageDrawable(getResources().getDrawable(
                                R.drawable.artwork_player));
            }
    }

    public Boolean isSeekable() {
        try {
            return !(mService == null || !mService.isSeekable());
        } catch (RemoteException e) {
            return false;
        }
    }

    private long mSeekPos = -1;

    private long mLastSeekEventTime = -1;

    public long setSeekMarker(float seekPercent) {
        try {
            if (mService == null || !mService.isSeekable()) {
                mSeekPos = -1;
                return mService.position();
            }

            if (mPlayingTrack != null) {
                long now = SystemClock.elapsedRealtime();
                if ((now - mLastSeekEventTime) > 250) {
                    mLastSeekEventTime = now;
                    try {
                        mSeekPos = mService.seek((long) (mPlayingTrack.duration * seekPercent));
                    } catch (RemoteException ex) {
                    }
                } else {
                    // where would we be if we had seeked
                    mSeekPos = mService
                            .getSeekResult((long) (mPlayingTrack.duration * seekPercent));
                }

                return mSeekPos;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void sendSeek() {
        try {
            if (mService == null || !mService.isSeekable()) {
                return;
            }

            mService.seek(mSeekPos);
            mSeekPos = -1;
        } catch (RemoteException e) {
            e.printStackTrace();
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
     * @param outState A Bundle in which to place any state information you wish
     *            to save.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("paused", paused);
        outState.putBoolean("currentTrackError", mCurrentTrackError);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mCurrentTrackError = savedInstanceState.getBoolean("currentTrackError");
        paused = savedInstanceState.getBoolean("paused");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[] {
                mPlayingTrack, mLoadTrackDetailsTask, mLoadCommentsTask, mCurrentComments
        };
    }

    private LoadDetailsTask mLoadTrackDetailsTask;

    @SuppressWarnings({
        "unchecked"
    })
    @Override
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

    /**
     * Called as part of the activity lifecycle when an activity is going into
     * the background, but has not (yet) been killed. The counterpart to
     * onResume().
     */
    @Override
    protected void onPause() {
        super.onPause();

    }

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
        mService = null;

    }

    private Track mFavoriteTrack;

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
                    mService.setFavoriteStatus(mPlayingTrack.id, false);
                mFavoriteTrack.user_favorite = false;
            } else {
                mService.setFavoriteStatus(mPlayingTrack.id, true);
                mFavoriteTrack.user_favorite = true;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
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
        mAddComment.user = CloudUtils.resolveUserById(this.getSoundCloudApplication(), mAddComment.user_id, mAddComment.user_id);
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
                    e.printStackTrace();
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
                
            } else
                handleException();
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
