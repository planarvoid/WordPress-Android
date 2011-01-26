package com.soundcloud.android.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Layout;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.ImageView.ScaleType;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.adapter.CommentsAdapter;
import com.soundcloud.android.adapter.LazyExpandableBaseAdapter;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.view.CommentMarker;
import com.soundcloud.android.view.UserBrowser;
import com.soundcloud.android.view.WaveformController;
import com.soundcloud.utils.AnimUtils;

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

	private ExpandableListView comments_lv;
	private WaveformController mWaveformController;
	

	private int mCurrentTrackId = -1;
	private String mPendingArtwork = "";

	private Track mPlayingTrack;
	private Track[] mEnqueueList;
	private int mEnqueuePosition;

	private LazyExpandableBaseAdapter mCommentsAdapter;
	private LinearLayout mCommentListHolder;
	private LinearLayout mLoadingLayout;
	
	private LinearLayout mTrackInfoBar;
	private ViewGroup mTransportBar;
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
	private boolean mFromTouch = false;
	private long mDuration;
	private boolean paused;

	private static final int REFRESH = 1;
	private static final int QUIT = 2;
	private static final int ALBUM_ART_DECODED = 4;
	

	// ******************************************************************** //
	// Activity Lifecycle.
	// ******************************************************************** //

	/**
	 * Called when the activity is starting. This is where most initialisation
	 * should go: calling setContentView(int) to inflate the activity's UI, etc.
	 * 
	 * You can call finish() from within this function, in which case
	 * onDestroy() will be immediately called without any of the rest of the
	 * activity lifecycle executing.
	 * 
	 * Derived classes must call through to the super class's implementation of
	 * this method. If they do not, an exception will be thrown.
	 * 
	 * @param icicle
	 *            If the activity is being re-initialised after previously being
	 *            shut down then this Bundle contains the data it most recently
	 *            supplied@Override protected void onLayout(boolean changed, int
	 *            l, int t, int r, int b) { Log.d("test",
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
		 * Intent intent = getIntent(); Bundle extras = intent.getExtras();
		 * 
		 * if (extras != null){ Log.i(TAG,"Setting track id to " +
		 * extras.getString("trackId")); mPlayTrackId =
		 * extras.getString("trackId"); }
		 */

		initControls();
		
		mDurationFormatLong = getString(R.string.durationformatlong);
		mDurationFormatShort = getString(R.string.durationformatshort);
		
     	Intent intent = getIntent();
 		Bundle extras = intent.getExtras();
 		
 		if (extras != null){
 			if (extras.containsKey("enqueueList")){
 				
 				//cast parcelables to tracks
 				Parcelable[] tmpParcelables = extras.getParcelableArray("enqueueList");
 				mEnqueueList = new Track[tmpParcelables.length];
 				int i = 0;
 				for (Parcelable tmp : tmpParcelables){
 					mEnqueueList[i] = (Track) tmp;
 					i++;
 				}
 				
 				//mEnqueueList = (Track[]) tmpParcelables;
 				mEnqueuePosition = extras.getInt("enqueuePosition");
 				mPlayingTrack = mEnqueueList[mEnqueuePosition];
 				updateTrackInfo();
 				
 				getIntent().removeExtra("enqueueList");
 			}
 		}

	}

	private void initControls() {

		mTransportBar = (ViewGroup) findViewById(R.id.transport_bar);
		mTrackFlipper = (ViewFlipper) findViewById(R.id.vfTrackInfo);
		
		//mLandscape = (this.getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		int width = getWindowManager().getDefaultDisplay().getWidth();
		int height = getWindowManager().getDefaultDisplay().getHeight(); 

		mLandscape =  ( getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		showingComments = preferences.getBoolean("showPlayerComments", false);
		// showingComments = false;


		mWaveformController = (WaveformController) findViewById(R.id.waveform_controller);
		mWaveformController.setPlayer(this);
		mWaveformController.setLandscape(mLandscape);
		
		mProgress = (ProgressBar) findViewById(R.id.progress_bar);
		mProgress.setMax(1000);
		mProgress.setInterpolator(new AccelerateDecelerateInterpolator());

		mCurrentTime = (TextView) findViewById(R.id.currenttime);
		mUserName = (TextView) findViewById(R.id.user);
		mTrackName = (TextView) findViewById(R.id.track);

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

		//mCommentsAdapter = createCommentsAdapter();
		mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

		if (!mLandscape) {
			
			mProfileButton = (ImageButton) findViewById(R.id.btn_profile);
			if (mProfileButton == null) return;//failsafe for orientation check failure
			mProfileButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {

					if (mPlayingTrack == null) {
						return;
					}

					Intent intent = new Intent(ScPlayer.this, ScProfile.class);
					intent.putExtra("userId", mPlayingTrack.getUserId());
					startActivityForResult(intent, CloudUtils.RequestCodes.REUATHORIZE);
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
					Log.i(TAG, "CLICK SHARE");
				}
			});
			mShareButton.setVisibility(View.GONE);
			
			
			mArtwork = (ImageView) findViewById(R.id.artwork);
			mArtwork.setScaleType(ScaleType.CENTER_CROP);
			mArtwork.setImageDrawable(getResources().getDrawable(
					R.drawable.artwork_player));

			mCommentsButton = (ImageButton) findViewById(R.id.btn_comment);
			//mCommentsButton.setOnClickListener(mToggleCommentsListener);
			//setCommentButtonImage();
			
			//temp
			mCommentsButton.setVisibility(View.GONE);
			
		}

	}
	
	

	
	int mInitialX = -1;
	int mLastX = -1;
	int mTextWidth = 0;
	int mViewWidth = 0;
	boolean mDraggingLabel = false;

	TextView textViewForContainer(View v) {
		View vv = v.findViewById(R.id.username);
		if (vv != null) {
			return (TextView) vv;
		}
		vv = v.findViewById(R.id.trackname);
		if (vv != null) {
			return (TextView) vv;
		}
		return null;
	}

	@Override
	protected void onServiceBound() {
		super.onServiceBound();
		try {
			Log.i(TAG,"On Service Bound 1 " + mService.getTrack());
			if (mService.getTrack() != null) {
				Log.i(TAG,"On Service Bound 2 " + mService.getTrack() + " "  + mService.isBuffering());
				if (mService.isBuffering()){
					setBufferingStart();
				}else
					setBufferingDone();
				
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

	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		TextView tv = textViewForContainer(v);
		if (tv == null) {
			return false;
		}
		if (action == MotionEvent.ACTION_DOWN) {
			v.setBackgroundColor(0xff606060);
			mInitialX = mLastX = (int) event.getX();
			mDraggingLabel = false;
		} else if (action == MotionEvent.ACTION_UP
				|| action == MotionEvent.ACTION_CANCEL) {
			v.setBackgroundColor(0);
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
				} else if (isSeekable()){
					mService.seek(0);
					//mService.play();
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

	/*private View.OnClickListener mToggleCommentsListener = new View.OnClickListener() {
		public void onClick(View v) {
			toggleComments();
		}
	};

	private void toggleComments() {

		if (showingComments) {
			applyRotation(0, -90);
		} else {
			applyRotation(0, 90);
		}

		showingComments = !showingComments;

		// showingComments = !showingComments;
		setCommentButtonImage();

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		preferences.edit().putBoolean("showPlayerComments", showingComments)
				.commit();
	}

	private void applyRotation(float start, float end) {
		// Find the center of image

		final float centerX;
		final float centerY;

		if (showingComments) {
			centerX = mCommentListHolder.getWidth() / 2.0f;
			centerY = mCommentListHolder.getHeight() / 2.0f;
		} else {
			centerX = mArtwork.getWidth() / 2.0f;
			centerY = mArtwork.getHeight() / 2.0f;
		}

		Log.i("DEBUG", "Apply rotation " + centerX);

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Flip3dAnimation rotation = new Flip3dAnimation(start, end,
				centerX, centerY);
		rotation.setDuration(500);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new DisplayNextView(!showingComments,
				mArtwork, mCommentListHolder));

		if (showingComments) {
			mCommentListHolder.startAnimation(rotation);
		} else {
			mArtwork.startAnimation(rotation);
		}
	}*/

	private void doPauseResume() {
		try {
			Log.i(TAG,"DO PAUSE RESUME");
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
	
	private void onTrackInfoFlip(){
		if (mTrackFlipper.getDisplayedChild() == 0){
			if (mTrackInfo == null){
				mTrackInfo = (RelativeLayout) ((ViewStub) findViewById(R.id.stub_info)).inflate();
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
	
	private void fillTrackDetails(){
		if (mPlayingTrack == null)
			return;
		
		((TextView) mTrackInfo.findViewById(R.id.txtPlays)).setText(mPlayingTrack.getPlaybackCount());
		((TextView) mTrackInfo.findViewById(R.id.txtFavorites)).setText(Integer.toString(mPlayingTrack.getFavoritingsCount()));
		((TextView) mTrackInfo.findViewById(R.id.txtDownloads)).setText(Integer.toString(mPlayingTrack.getDownloadCount()));
		((TextView) mTrackInfo.findViewById(R.id.txtComments)).setText(Integer.toString(mPlayingTrack.getCommentCount()));
		
		((TextView) mTrackInfo.findViewById(R.id.txtInfo)).setText(Html.fromHtml(generateTrackInfoString()));
	}
	
	private String generateTrackInfoString(){
		String str = "";
		str += "<b>Description</b><br />";
		str += mPlayingTrack.getDescription()+"<br /><br />";
		if (!CloudUtils.stringNullEmptyCheck(mPlayingTrack.getTagList())) str += mPlayingTrack.getTagList()+ "<br />";
		if (!CloudUtils.stringNullEmptyCheck(mPlayingTrack.getKeySignature())) str += mPlayingTrack.getKeySignature() + "<br />";
		if (!CloudUtils.stringNullEmptyCheck(mPlayingTrack.getGenre())) str += mPlayingTrack.getGenre() + "<br />";
		if (!(mPlayingTrack.getBpm() == null)) str += mPlayingTrack.getBpm() + "<br />";
		str += "<br />";
		if (!CloudUtils.stringNullEmptyCheck(mPlayingTrack.getLicense()) && !mPlayingTrack.getLicense().toLowerCase().contentEquals("all rights reserved")) str += mPlayingTrack.getLicense() + "<br /><br />";
		
		if (!CloudUtils.stringNullEmptyCheck(mPlayingTrack.getLabelName())){
			str += "<b>Released By</b><br />";
			str += mPlayingTrack.getLabelName() + "<br />";
			if (!CloudUtils.stringNullEmptyCheck(mPlayingTrack.getReleaseYear())) str += mPlayingTrack.getReleaseYear() + "<br />";
			str += "<br />";
		}
		
		
		/*try {
			str += "<b>Uploaded " + mPlayingTrack.getData(Track.key_created_at)+"<br />";
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
			Date uploadDate = new Date(sdf.parse(mPlayingTrack.getData(Track.key_created_at).substring(0,mPlayingTrack.getData(Track.key_created_at).indexOf("+")-1)).getTime());
			str += "<b>Uploaded " + uploadDate.getDate()+"<br />";
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
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

	private void setCommentButtonImage() {
		/*
		 * if (!mLandscape) if (showingComments) {
		 * mCommentsButton.setImageResource(R.drawable.ic_comm); } else {
		 * mCommentsButton.setImageResource(R.drawable.ic_comment); }
		 */
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
				mCurrentTime.setText(CloudUtils.makeTimeString(pos < 3600000 ? mDurationFormatShort : mDurationFormatLong, pos / 1000) + " / " + mCurrentDurationString);
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
			Log.i(TAG,"ScPlayer received action " + action);
			if (action.equals(CloudPlaybackService.META_CHANGED)) {
				mCurrentTrackError = false;
				resetComments();
				updateTrackInfo();
				setPauseButtonImage();
				setBufferingStart();
				queueNextRefresh(1);
			} else if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)) {
				setPauseButtonImage();
			} else if (action.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {
				setPauseButtonImage();
			} else if (action.equals(CloudPlaybackService.INITIAL_BUFFERING)) {
				mCurrentTrackError = false;
				hideUnplayable();
				setBufferingStart();
			} else if (action.equals(CloudPlaybackService.BUFFERING)) {
				hideUnplayable();
				setBufferingStart();
			} else if (action.equals(CloudPlaybackService.BUFFERING_COMPLETE)) {
				//clearSeekVars();
				setBufferingDone();
			} else if (action.equals(CloudPlaybackService.TRACK_ERROR)) {
				mCurrentTrackError = true;
				//showDialog(CloudUtils.Dialogs.DIALOG_ERROR_TRACK_ERROR);
				setBufferingDone();
				showUnplayable();
			} else if (action.equals(CloudPlaybackService.STREAM_DIED)) {
				//showToast(getString(R.string.toast_error_stream_died));
			} else if (action.equals(CloudPlaybackService.COMMENTS_LOADED)) {
				updateTrackInfo();
			} else if (action.equals(CloudPlaybackService.SEEK_COMPLETE)) {
				//setPauseButtonImage();
				
			}
		}
	};
	
	private void showUnplayable(){
		if (mPlayingTrack == null || CloudUtils.isTrackPlayable(mPlayingTrack)){ //playback error
			((TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt)).setText(R.string.player_error);
		}else {
			((TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt)).setText(R.string.player_not_streamable);
		}
		
		mPlayableLayout.setVisibility(View.GONE);
		mUnplayableLayout.setVisibility(View.VISIBLE);
		
	}
	
	private void hideUnplayable(){
		mPlayableLayout.setVisibility(View.VISIBLE);
		mUnplayableLayout.setVisibility(View.GONE);
	}
	
	private void setBufferingStart() {
		Log.i(TAG, "Set Buffering Start");
		//mPauseButton.setEnabled(false);
		mWaveformController.showConnectingLayout();
	}

	private void setBufferingDone() {
		Log.i(TAG, "Set Buffering Done");
		//mPauseButton.setEnabled(true);
		mWaveformController.hideConnectingLayout();
		// play();

	}

	public void startPlayback(Track track) {
		
		if (mService == null) {
			return;
		}

		try {
			mService.enqueueTrack(track, CloudPlaybackService.NOW);
			mPlayingTrack = track;
		} catch (RemoteException ex) {
			Log.d("MediaPlaybackActivity", "couldn't start playback: " + ex);
		}

	}

	private void updateTrackInfo() {

		if (mService != null) {
			try {
				if (mService.getTrack() == null) return;
				mPlayingTrack = mService.getTrack();
			} catch (RemoteException e) {e.printStackTrace();}
		}
		
		Log.i(TAG,"Playing Track " + mPlayingTrack);
		if (mPlayingTrack == null) 
			return;
		
		mWaveformController.updateTrack(mPlayingTrack);
		if (!mLandscape)
		if (CloudUtils.stringNullEmptyCheck(mPlayingTrack.getArtworkUrl())) {
			//no artwork
			ImageLoader.get(this).unbind(mArtwork);
			mArtwork.setImageDrawable(getResources().getDrawable(R.drawable.artwork_player));
		} else {
			//load artwork as necessary
			if (!(mPlayingTrack.getId() == mCurrentTrackId) || mCurrentArtBindResult == BindResult.ERROR)
				if (ImageLoader.get(this).bind(mArtwork, CloudUtils.formatGraphicsUrl(mPlayingTrack.getArtworkUrl(),GraphicsSizes.crop), null) != BindResult.OK)
					mArtwork.setImageDrawable(getResources().getDrawable(R.drawable.artwork_player));
		}
		
		Log.i(TAG,"New track? " + mPlayingTrack.getId() + " " + mCurrentTrackId);
		if (mPlayingTrack.getId() != mCurrentTrackId) {
			mTrackName.setText(mPlayingTrack.getTitle());
			mUserName.setText(mPlayingTrack.getUser().getUsername());
			
			if (mTrackFlipper != null && mTrackFlipper.getDisplayedChild() == 1){
				onTrackInfoFlip();
			}

			Log.i(TAG,"Current track error? " + mCurrentTrackError);
			if (mCurrentTrackError)
				return;
			
			if (CloudUtils.isTrackPlayable(mPlayingTrack)){
					hideUnplayable();
			} else {
				showUnplayable();
				setBufferingDone();
			}

			if (mTrackInfo != null)
				fillTrackDetails();
			
			setFavoriteStatus();
			mDuration = Long.parseLong(Integer.toString(mPlayingTrack.getDuration()));
			mCurrentDurationString = CloudUtils.makeTimeString(mDuration < 3600000 ? mDurationFormatShort : mDurationFormatLong, mDuration / 1000);
			
			mapCurrentComments();
		}

	}
	

	public Boolean isSeekable() {
		try {
			return !(mService == null ||  !mService.isSeekable());
		} catch (RemoteException e) {
			return false;
		}
	}

	private long mSeekPos = -1;
	
	public long setSeekMarker(float seekPercent) {
		
		try {
			if (mService == null ||  !mService.isSeekable()) {
				mSeekPos = -1;
				return mService.position();
			}

			if (mPlayingTrack != null) {
				mSeekPos = (long) (mPlayingTrack.getDuration() * seekPercent);
				return mSeekPos;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		return 0;
		
	}
	
	public void sendSeek(){
		try {
			if (mService == null  ||  !mService.isSeekable()){
				return;
			}
			
			mService.seek(mSeekPos);
			mSeekPos = -1;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	
	public void playFromPosition(int trackId, long timestamp) {
		if (mService == null) {
			return;
		}

		try {
			if (mPlayingTrack != null) {
				if (mPlayingTrack.getId() == trackId && mService.isSeekable()) {
					mService.seek(timestamp);
				}
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void commentAdded(Comment comment) {

		if (mService != null) {
			try {
				mService.addComment(comment);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (mPlayingTrack != null) {
			//if (mPlayingTrack.getId == comment.get.getData(Comment.key_track_id))) {
				//updateTrackInfo();
			//}
		}
	}

	private void mapCurrentComments() {
		if (mPlayingTrack != null) {
			//mCommentsAdapter.clear();
			//Comment[] comments = mPlayingTrack.comments;
			//if (comments != null) {
				//CloudUtils.mapCommentsToAdapter(comments, mCommentsAdapter, true);
				//mCommentsAdapter.notifyDataSetChanged();
				/*if (mLandscape) {
					mWaveformController.mapComments(mCommentsAdapter,
							Integer.parseInt(mPlayingTrack
									.getData(Track.key_duration)));
					mWaveformController.invalidate();
				} else {
				mWaveformController.setDuration(Integer.parseInt(mPlayingTrack.getData(Track.key_duration)));
				//}*/

			//}

		}
	}

	protected LazyExpandableBaseAdapter createCommentsAdapter() {
		mThreadData = new ArrayList<Parcelable>();
		mCommentData = new ArrayList<ArrayList<Parcelable>>();
		return new CommentsAdapter(this, mThreadData, mCommentData);
	}

	public ExpandableListView getCommentsList() {
		return comments_lv;
	}

	public LazyExpandableBaseAdapter getCommentsAdapter() {
		return mCommentsAdapter;
	}

	protected void resetComments() {
		//showCommentsLoading();

		//mCommentsAdapter.clear();
		//mCommentsAdapter.notifyDataSetChanged();

		//mWaveformController.mapComments(mCommentsAdapter, 1);
		//mWaveformController.invalidate();
	}

	protected void showCommentsLoading() {
		if (mLandscape) {
			return;
		}

		mLoadingLayout.findViewById(android.R.id.progress).setVisibility(
				View.VISIBLE);

		if (mLoadingLayout.getParent() != null) {
			((ViewGroup) mLoadingLayout.getParent()).removeView(mLoadingLayout);
		}

		mCommentListHolder.addView(mLoadingLayout, 0);
		comments_lv.setVisibility(View.INVISIBLE);
	}

	

	public void addCommentPrompt(Comment comment) {
		addComment = comment;
		showDialog(CloudUtils.Dialogs.DIALOG_ADD_COMMENT);
	}

	public void addComment(String commentBody) {

		/*addComment.putData(Comment.key_body, commentBody);

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder documentBuilder;

		Comment addedComment = null;

		try {

			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(getSoundCloudApplication().putComment(addComment).getContent());
			NodeList childNodeList = document.getElementsByTagName("comment");

			if (childNodeList.getLength() == 0) {
				showDialog(CloudUtils.Dialogs.DIALOG_ADD_COMMENT_ERROR);
			} else {
				addedComment = new Comment(childNodeList.item(0));
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			showDialog(CloudUtils.Dialogs.DIALOG_ADD_COMMENT_ERROR);
		}

		// add this comment somewhere
		if (addedComment != null) {
			commentAdded(addedComment);
		}*/

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
	 * camera), etc.
	 * 
	 * Derived classes must call through to the super class's implementation of
	 * this method. If they do not, an exception will be thrown.
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

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (preferences.getBoolean("showPlayerComments", true) != showingComments) {
			showingComments = preferences.getBoolean("showPlayerComments",
					false);
			// showingComments = true;
		}

		setCommentButtonImage();

	}

	/**
	 * Called to retrieve per-instance state from an activity before being
	 * killed so that the state can be restored in onCreate(Bundle) or
	 * onRestoreInstanceState(Bundle) (the Bundle populated by this method will
	 * be passed to both).
	 * 
	 * @param outState
	 *            A Bundle in which to place any state information you wish to
	 *            save.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putBoolean("paused", paused);
		outState.putBoolean("currentTrackError", mCurrentTrackError);
		
		super.onSaveInstanceState(outState); 
	}
	
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) 
    {
    	mCurrentTrackError = savedInstanceState.getBoolean("currentTrackError");
    	paused = savedInstanceState.getBoolean("paused");
        super.onRestoreInstanceState(savedInstanceState);
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

	@Override
	protected Dialog onCreateDialog(int which) {
		switch (which) {

		case CloudUtils.Dialogs.DIALOG_ADD_COMMENT:
			final EditText input = new EditText(this);
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			/*alert.setTitle(String.format(
					getString(R.string.add_comment_dialog_title), addComment
							.getData(Comment.key_timestamp_formatted)));
			alert.setView(input);
			alert.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							addComment(input.getText().toString());
							removeDialog(CloudUtils.Dialogs.DIALOG_ADD_COMMENT);
						}
					});
			alert.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							removeDialog(CloudUtils.Dialogs.DIALOG_ADD_COMMENT);
						}
					});*/

			return alert.show();

		case CloudUtils.Dialogs.DIALOG_ADD_COMMENT_ERROR:
			return new AlertDialog.Builder(this).setTitle(
					R.string.error_add_comment_error_title).setMessage(
					R.string.error_add_comment_error_message)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									removeDialog(CloudUtils.Dialogs.DIALOG_ADD_COMMENT_ERROR);
								}
							}).create();

		}
		return super.onCreateDialog(which);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		if (v.getClass() == CommentMarker.class) {

			menuParcelable = ((CommentMarker.CommentContextMenuInfo) menuInfo).comment;
			menu.add(0, CloudUtils.ContextMenu.REPLY_TO_COMMENT, 0,
					getString(R.string.context_menu_reply_to_comment));
			menu.add(0, CloudUtils.ContextMenu.VIEW_UPLOADER, 0,
					getString(R.string.context_menu_view_user));
			menu.add(0, CloudUtils.ContextMenu.CLOSE, 0,
					getString(R.string.context_menu_close));

		} else if (v == getCommentsList()) {

			ExpandableListView.ExpandableListContextMenuInfo info;
			try {
				info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
			} catch (ClassCastException e) {
				Log.e(getClass().getSimpleName(), "bad menuInfo", e);
				return;
			}

			int groupPosition = ExpandableListView
					.getPackedPositionGroup(info.packedPosition);
			int childPosition = ExpandableListView
					.getPackedPositionChild(info.packedPosition);

			if (childPosition == -1) {
				menuParcelable = (Comment) getCommentsAdapter().getGroup(
						groupPosition);
			} else {
				menuParcelable = (Comment) getCommentsAdapter().getChild(
						groupPosition, childPosition);
			}

			menu
					.add(
							0,
							CloudUtils.ContextMenu.PLAY_FROM_COMMENT_POSITION,
							0,
							getString(R.string.context_menu_play_from_comment_position));
			menu.add(0, CloudUtils.ContextMenu.REPLY_TO_COMMENT, 0,
					getString(R.string.context_menu_reply_to_comment));
			menu.add(0, CloudUtils.ContextMenu.VIEW_UPLOADER, 0,
					getString(R.string.context_menu_view_user));
			menu.add(0, CloudUtils.ContextMenu.CLOSE, 0,
					getString(R.string.context_menu_close));

		}

		//
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		/*switch (item.getItemId()) {
		case CloudUtils.ContextMenu.PLAY_FROM_COMMENT_POSITION:
			long tsLong = Long.parseLong(((Comment) menuParcelable)
					.getData(Comment.key_timestamp));
			if (tsLong < 0) {
				tsLong = 0;
			}
			String trackId = ((Comment) menuParcelable)
					.getData(Comment.key_track_id);
			playFromPosition(trackId, tsLong);
			break;

		case CloudUtils.ContextMenu.VIEW_COMMENTER:
			Intent i = new Intent(this, UserBrowser.class);
			i.putExtra("userLoadPermalink", ((Comment) menuParcelable)
					.getData(Comment.key_user_permalink));
			startActivity(i);
			break;

		case CloudUtils.ContextMenu.REPLY_TO_COMMENT:
			String ts = ((Comment) menuParcelable)
					.getData(Comment.key_timestamp);

			Comment mAddComment = new Comment();
			mAddComment.putData(Comment.key_timestamp, ts);
			mAddComment.putData(Comment.key_timestamp_formatted, CloudUtils
					.formatTimestamp(Integer.parseInt(ts)));
			mAddComment.putData(Comment.key_track_id,
					((Comment) menuParcelable).getData(Comment.key_track_id));
			addCommentPrompt(mAddComment);
			break;

		default:
			return super.onContextItemSelected(item);
		}*/
		return true;
	}

	private Track mFavoriteTrack;
	private String mFavoriteResult;

	private void setFavoriteStatus() {

		if (mPlayingTrack == null || mFavoriteButton == null) {
			return;
		}

		if (mPlayingTrack.getUserFavorite()
				.contentEquals("true")) {
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

		if (mFavoriteTrack.getUserFavorite().contentEquals("true")) {
			mFavoriteTrack.setUserFavorite("");
			removeFavorite();
		} else {
			mFavoriteTrack.setUserFavorite("true");
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
					mFavoriteResult = IOUtils.toString(getSoundCloudApplication()
									.putContent(SoundCloudApplication.PATH_MY_FAVORITES + "/" + mFavoriteTrack.getId()));
				} catch (IOException e) {
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
					mFavoriteResult = IOUtils.toString(getSoundCloudApplication()
							.deleteContent(SoundCloudApplication.PATH_MY_FAVORITES + "/" + mFavoriteTrack.getId()));
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
			if (mFavoriteResult != null) {
				if (mFavoriteResult.indexOf("200 - OK") != -1 || mFavoriteResult.indexOf("201 - Created") != -1) {
					try {
						mService.setFavoriteStatus(mFavoriteTrack.getId(), "true");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} 
			}
			mFavoriteButton.setEnabled(true);
		}
	};
	
	// Create runnable for posting since we update the following asynchronously
	final Runnable mUpdateRemoveFavorite = new Runnable() {
		public void run() {
			handleException();
			if (mFavoriteResult != null) {
				if (mFavoriteResult.indexOf("200 - OK") != -1 || mFavoriteResult.indexOf("201 - Created") != -1) {
					try {
						mService.setFavoriteStatus(mFavoriteTrack.getId(), "");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} 
			}
			if (mFavoriteButton != null) mFavoriteButton.setEnabled(true);
			
		}
	};

}
