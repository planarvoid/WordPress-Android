
package com.soundcloud.android.activity;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.task.LoadCollectionTask;
import com.soundcloud.android.task.LoadDetailsTask;
import com.soundcloud.android.view.WaveformController;
import com.soundcloud.utils.AnimUtils;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.client.ClientProtocolException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.IOException;
import java.util.ArrayList;

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

    private Boolean showingComments;

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
    
    private boolean mTrackDetailsFilled = false;
    
    private LoadCollectionTask mLoadCommentsTask;

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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        showingComments = preferences.getBoolean("showPlayerComments", false);

        mWaveformController = (WaveformController) findViewById(R.id.waveform_controller);
        mWaveformController.setPlayer(this);
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
                    intent.putExtra("userId", mPlayingTrack.getUserId());
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
            // mCommentsButton.setOnClickListener(mToggleCommentsListener);
            // setCommentButtonImage();

            // temp
            mCommentsButton.setVisibility(View.GONE);
        }
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
        Log.i(TAG,"Text view for container " + tv);
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
        Log.i(TAG,"Returning false");
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

    private void onTrackInfoFlip() {
        if (mTrackFlipper.getDisplayedChild() == 0) {
            if (mTrackInfo == null) {
                mTrackInfo = (RelativeLayout) ((ViewStub) findViewById(R.id.stub_info)).inflate();
            }
            
            if (!mTrackDetailsFilled){
                fillTrackDetails();                
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
        Log.i(TAG,"New Load Track Details Task");
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
    
    private void onDetailsResult(Boolean success){
        if (mTrackInfo.findViewById(R.id.loading_layout) != null){
            mTrackInfo.findViewById(R.id.loading_layout).setVisibility(View.GONE);
        } 
        
        if (!success){
            if (mTrackInfo.findViewById(android.R.id.empty) != null){
                mTrackInfo.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            } else {
                mTrackInfo.addView(CloudUtils.buildEmptyView(this, getResources().getString(R.string.info_error)),mTrackInfo.getChildCount()-2);  
            }
            
            mTrackInfo.findViewById(R.id.info_view).setVisibility(View.GONE);
            
        } else {
            if (mTrackInfo.findViewById(android.R.id.empty) != null){
                mTrackInfo.findViewById(android.R.id.empty).setVisibility(View.GONE);
            }
            
            mTrackInfo.findViewById(R.id.info_view).setVisibility(View.VISIBLE);
            
        }
    }

    private void fillTrackDetails() {
        if (mPlayingTrack == null)
            return;
        
        if (mPlayingTrack.getPlaybackCount() == null){
            
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
                            SoundCloudApplication.PATH_TRACK_DETAILS.replace("{track_id}", Long
                                    .toString(mPlayingTrack.getId()))));
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (mTrackInfo.findViewById(R.id.loading_layout) != null)
                    mTrackInfo.findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
            }
            return;
        }
        
        ((TextView) mTrackInfo.findViewById(R.id.txtPlays)).setText(mPlayingTrack
                .getPlaybackCount());
        ((TextView) mTrackInfo.findViewById(R.id.txtFavorites)).setText(Integer
                .toString(mPlayingTrack.getFavoritingsCount()));
        ((TextView) mTrackInfo.findViewById(R.id.txtDownloads)).setText(Integer
                .toString(mPlayingTrack.getDownloadCount()));
        ((TextView) mTrackInfo.findViewById(R.id.txtComments)).setText(Integer
                .toString(mPlayingTrack.getCommentCount()));

        ((TextView) mTrackInfo.findViewById(R.id.txtInfo)).setText(Html
                .fromHtml(generateTrackInfoString()));
        
        mTrackDetailsFilled = true;
    }

    private String generateTrackInfoString() {
        String str = "";
        str += "<b>Description</b><br />";
        str += mPlayingTrack.getDescription() + "<br /><br />";
        if (!TextUtils.isEmpty(mPlayingTrack.getTagList()))
            str += mPlayingTrack.getTagList() + "<br />";
        if (!TextUtils.isEmpty(mPlayingTrack.getKeySignature()))
            str += mPlayingTrack.getKeySignature() + "<br />";
        if (!TextUtils.isEmpty(mPlayingTrack.getGenre()))
            str += mPlayingTrack.getGenre() + "<br />";
        if (!(mPlayingTrack.getBpm() == null))
            str += mPlayingTrack.getBpm() + "<br />";
        str += "<br />";
        if (!TextUtils.isEmpty(mPlayingTrack.getLicense())
                && !mPlayingTrack.getLicense().toLowerCase().contentEquals("all rights reserved"))
            str += mPlayingTrack.getLicense() + "<br /><br />";

        if (!TextUtils.isEmpty(mPlayingTrack.getLabelName())) {
            str += "<b>Released By</b><br />";
            str += mPlayingTrack.getLabelName() + "<br />";
            if (!TextUtils.isEmpty(mPlayingTrack.getReleaseYear()))
                str += mPlayingTrack.getReleaseYear() + "<br />";
            str += "<br />";
        }

        /*
         * temporary commenting out timestamp until I figure out how to do the
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
            Log.i(TAG, "ScPlayer received action " + action);
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
                if (mService.getTrack() == null)
                    return;
                
                if (mPlayingTrack == null || mPlayingTrack.getId() != mService.getTrackId())
                    mPlayingTrack = mService.getTrack();
                
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        if (mPlayingTrack == null)
            return;

        mWaveformController.updateTrack(mPlayingTrack);
        updateArtwork();

        if (mCurrentTrackId == null || mPlayingTrack.getId().compareTo(mCurrentTrackId) != 0) {
            
            //mLoadCommentsTask = new LoadCollectionTask();
            //mLoadCommentsTask.loadModel = CloudUtils.Model.comment;
            //mLoadCommentsTask.pageSize = 50;
            //mLoadCommentsTask.setContext(this);
            //try {
              //  mLoadCommentsTask.execute(getSoundCloudApplication().getPreparedRequest(SoundCloudApplication.PATH_TRACK_COMMENTS.replace("{track_id}", Long.toString(mPlayingTrack.getId()))));
            //} catch (OAuthException e) {
              //  e.printStackTrace();
            //}
           
            
            mTrackName.setText(mPlayingTrack.getTitle());
            mUserName.setText(mPlayingTrack.getUser().getUsername());

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

            mTrackDetailsFilled = false;
            setFavoriteStatus();
            mDuration = Long.parseLong(Integer.toString(mPlayingTrack.getDuration()));
            mCurrentDurationString = CloudUtils.makeTimeString(
                    mDuration < 3600000 ? mDurationFormatShort : mDurationFormatLong,
                    mDuration / 1000);
        }
    }

    private void updateArtwork() {
        if (!mLandscape)
            if (TextUtils.isEmpty(mPlayingTrack.getArtworkUrl())) {
                // no artwork
                ImageLoader.get(this).unbind(mArtwork);
                mArtwork.setImageDrawable(getResources().getDrawable(R.drawable.artwork_player));
            } else {
                // load artwork as necessary
                if (mCurrentTrackId == null
                        || mPlayingTrack.getId().compareTo(mCurrentTrackId) != 0
                        || mCurrentArtBindResult == BindResult.ERROR)
                    if ((mCurrentArtBindResult = ImageLoader.get(this).bind(
                            mArtwork,
                            CloudUtils.formatGraphicsUrl(mPlayingTrack.getArtworkUrl(),
                                    GraphicsSizes.crop), new ImageLoader.Callback() {
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
                        mSeekPos = mService
                                .seek((long) (mPlayingTrack.getDuration() * seekPercent));
                    } catch (RemoteException ex) {
                    }
                } else {
                    // where would we be if we had seeked
                    mSeekPos = mService
                            .getSeekResult((long) (mPlayingTrack.getDuration() * seekPercent));
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean("showPlayerComments", true) != showingComments) {
            showingComments = preferences.getBoolean("showPlayerComments", false);
            // showingComments = true;
        }

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
        return new Object[] {mPlayingTrack, mLoadTrackDetailsTask};
    }
    
    private LoadDetailsTask mLoadTrackDetailsTask;
    

    @Override
    protected void restoreState() {

        // restore state
        Object[] saved = (Object[]) getLastNonConfigurationInstance();

        if (saved != null) {
            if (saved[0] != null)
                mPlayingTrack = (Track) saved[0];
            
            if (saved[1] != null){
                mLoadTrackDetailsTask = (LoadDetailsTask) saved[1];
                mLoadTrackDetailsTask.setActivity(this);
                if (CloudUtils.isTaskPending(mLoadTrackDetailsTask))
                    mLoadTrackDetailsTask.execute();
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

        paused = true;
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);
        mService = null;

    }

    private Track mFavoriteTrack;

    private String mFavoriteResult;

    private void setFavoriteStatus() {

        if (mPlayingTrack == null || mFavoriteButton == null) {
            return;
        }

        if (mPlayingTrack.getUserFavorite()) {
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

        mFavoriteResult = null;
        
        if (mPlayingTrack.getUserFavorite()) {
            mFavoriteTrack.setUserFavorite(false);
            removeFavorite();
        } else {
            mFavoriteTrack.setUserFavorite(true);
            addFavorite();
        }
        setFavoriteStatus();

    }

    public void addFavorite() {

        // Fire off a thread to do some work that we shouldn't do directly in
        // the UI thread
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    mFavoriteResult = CloudUtils.streamToString(getSoundCloudApplication()
                            .putContent(
                                    SoundCloudApplication.PATH_MY_FAVORITES + "/"
                                            + mFavoriteTrack.getId()));
                } catch (Exception e) {
                    e.printStackTrace();
                    setException(e);
                }
                mHandler.post(mUpdateAddFavorite);
            }
        };
        t.start();
    }

    private void removeFavorite() {

        // Fire off a thread to do some work that we shouldn't do directly in
        // the UI thread
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    mFavoriteResult = CloudUtils.streamToString(getSoundCloudApplication()
                            .deleteContent(
                                    SoundCloudApplication.PATH_MY_FAVORITES + "/"
                                            + mFavoriteTrack.getId()));
                } catch (Exception e) {
                    e.printStackTrace();
                    setException(e);
                }
                mHandler.post(mUpdateRemoveFavorite);
            }
        };
        t.start();
    }

    // Create runnable for posting since we update the following asynchronously
    final Runnable mUpdateAddFavorite = new Runnable() {
        public void run() {
            handleException();
            boolean favorite = false;
            if (mFavoriteResult != null) {
                if (mFavoriteResult.contains("200 - OK")
                        || mFavoriteResult.contains("201 - Created")) {
                    favorite = true;
                } else {
                    favorite = false;
                }
            }
            setFavoriteResult(favorite);
        }
    };

    // Create runnable for posting since we update the following asynchronously
    final Runnable mUpdateRemoveFavorite = new Runnable() {
        public void run() {
            handleException();
            boolean favorite = true;
            if (mFavoriteResult != null) {
                if (mFavoriteResult.contains("200 - OK")
                        || mFavoriteResult.contains("201 - Created")
                        || mFavoriteResult.contains("404 - Not Found")) {
                    favorite = false;
                } else {
                    favorite = true;
                }
            }
            setFavoriteResult(favorite);
        }
    };

    private void setFavoriteResult(boolean favorite) {
       
        if (mFavoriteTrack.getUserFavorite() != favorite) {
            mFavoriteTrack.setUserFavorite(favorite);
            setFavoriteStatus();
            try {
                mService.setFavoriteStatus(mFavoriteTrack.getId(), favorite);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        if (mFavoriteButton != null)
            mFavoriteButton.setEnabled(true);
    }

}
