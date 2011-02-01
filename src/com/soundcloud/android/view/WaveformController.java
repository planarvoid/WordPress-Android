package com.soundcloud.android.view;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ImageView.ScaleType;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.adapter.LazyExpandableBaseAdapter;
import com.soundcloud.android.objects.Track;

public class WaveformController extends FrameLayout implements OnTouchListener, OnLongClickListener {
	private static final String TAG = "WaveformController" ;

	private Track mPlayingTrack;
	private Drawable mLoadingWaveform;
	private Drawable mPendingWaveform;
	private Boolean mPendingComments = false;
	
	private Boolean mTrackSeeking = false;
	
	private ImageView mOverlay;
	private ProgressBar mProgressBar;
	private RelativeLayout mCommentBar;
	private RelativeLayout mTrackTouchBar;
	private WaveformHolder mWaveformHolder;
	private RelativeLayout mWaveformFrame;
	private RelativeLayout mConnectingBar;
	
	private ScPlayer mPlayer;
	private Context mContext;
	
	private int mDuration;
	private LazyExpandableBaseAdapter mCommentsAdapter;
	
	private Float initialWaveScaleX;
	private Boolean mLandscape = false; 
	
	private ImageLoader.BindResult waveformResult;
	
	
    final Handler mHandler = new Handler();
	
	// These matrices will be used to move and zoom image
	   Matrix matrix = new Matrix();
	   Matrix savedMatrix = new Matrix();
	  
	   float oldDist;
	   PointF start = new PointF();
	   PointF fake = new PointF();
	   PointF mid = new PointF();
	   
	   static final int SEEK_TOLERANCE = 10;
	   static final double TOUCH_MOVE_TOLERANCE = 2.0;

	   // We can be in one of these 3 states
	   static final int SEEK_NONE = 0;
	   static final int SEEK_DRAG = 1;
	   int mode = SEEK_NONE;
	   
	   
	   
	   SharedPreferences mPrefernces;

	
	public WaveformController(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;
		
		mPrefernces = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		LayoutInflater inflater = (LayoutInflater) context
		.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.waveformcontroller, this);

		mConnectingBar = (RelativeLayout) findViewById(R.id.connecting_bar);
		mWaveformFrame = (RelativeLayout) findViewById(R.id.waveform_frame);
		mWaveformHolder = (WaveformHolder) findViewById(R.id.waveform_holder);
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
		//mCommentBar = (RelativeLayout) findViewById(R.id.comment_bar);
		mTrackTouchBar = (RelativeLayout) findViewById(R.id.track_touch_bar);
		mTrackTouchBar.setOnTouchListener(this);
		mOverlay = (ImageView) findViewById(R.id.progress_overlay);
		
		if (mCommentBar != null){
			mCommentBar.setOnTouchListener(this);
			mCommentBar.setOnLongClickListener(this);	
		}
		
		LightingColorFilter lcf = new LightingColorFilter(1,mContext.getResources().getColor(R.color.white));
		mOverlay.setBackgroundColor(Color.TRANSPARENT);
		mOverlay.setColorFilter(lcf);
		mOverlay.setScaleType(ScaleType.FIT_XY);
		File dirFile = new File(CloudUtils.getCacheDirPath(mContext)+"/waves/");
		dirFile.mkdirs();
		
		
		// mOverlay.setImageDrawable(context.getResources().getDrawable(R.drawable.wave));
	}
	
	public void showConnectingLayout(){
		mConnectingBar.setVisibility(View.VISIBLE);
	}
	
	public void hideConnectingLayout(){
		mConnectingBar.setVisibility(View.GONE);
	}
	
	public void setLandscape(boolean isLandscape){
		mLandscape = isLandscape;
		
		if (!mLandscape)
			this.setStaticTransformationsEnabled(true);
	}
	
	@Override
	protected boolean getChildStaticTransformation(View child, Transformation t){
		
		boolean ret =  super.getChildStaticTransformation(child, t);
		if (child == mWaveformFrame){
			t.setAlpha((float) 0.7);
			return true;
		}
		return ret;
	}
	
	
	
	public void setProgress(long pos){
		if (mDuration == 0)
			return;
		
		mProgressBar.setProgress((int) (1000 * pos / mDuration));
	}
	
	public void setSecondaryProgress(int percent){
		mProgressBar.setSecondaryProgress(percent);
	}
	
	public void setPlayer(ScPlayer ocPlayer){
		mPlayer = ocPlayer;
	}
	
	public void updateTrack(Track track) {
		if (mPlayingTrack != null){
			if (mPlayingTrack.getId().compareTo(track.getId()) == 0 && waveformResult != BindResult.ERROR){
				return;
			}
		}
		
		mPlayingTrack = track;
		mDuration = mPlayingTrack.getDuration();
		
		if (waveformResult != BindResult.ERROR){ //clear loader errors so we can try to reload
			 ImageLoader.get(mContext).clearErrors();
		}
		
		waveformResult = ImageLoader.get(mContext).bind(mOverlay, track.getWaveformUrl(), new ImageLoader.Callback(){
			@Override
			public void onImageError(ImageView view, String url, Throwable error) {
				waveformResult = BindResult.ERROR;
			}
			@Override
			public void onImageLoaded(ImageView view, String url) {
			}
		});
		
		
		if (waveformResult != BindResult.OK){ //otherwise, it succesfull pulled it out of memory, so no temp image necessary
			mOverlay.setImageDrawable(mContext.getResources().getDrawable(R.drawable.player_wave_bg));
		}
	}
	
	public BindResult currentWaveformResult(){
		return waveformResult;
	}
	
	   public boolean onTouch(View v, MotionEvent event) {
			   switch (event.getAction() & MotionEvent.ACTION_MASK) {
			      case MotionEvent.ACTION_DOWN:
			    	  if (mPlayer != null && mPlayer.isSeekable()){
			    		  mode = SEEK_DRAG;
				    	  if (mPlayer != null) setProgress(mPlayer.setSeekMarker(event.getX()/mWaveformHolder.getWidth()));  
				    	  mWaveformHolder.invalidate();  
			    	  }
			    	  break;
			    	  
			      case MotionEvent.ACTION_MOVE:
			    	  
			    	  if (mPlayer != null && mPlayer.isSeekable()){
				    	  if (mode == SEEK_DRAG){
				    		  if (mPlayer != null) setProgress(mPlayer.setSeekMarker(event.getX()/mWaveformHolder.getWidth()));  
				    	  }
				    	  mWaveformHolder.invalidate();
			    	  }
			    	  break;
			      case MotionEvent.ACTION_UP:
			    	  if (mode == SEEK_DRAG){
			    		  if (mPlayer != null) mPlayer.sendSeek();  
			    		  mode = SEEK_NONE;
			    	  }
			    	  break;
			   }
	      return true; // indicate event was handled
	   }
	   
	   private Handler handler = new Handler();


	public boolean onLongClick(View v) {
		return true;
	}
	
	
	
	
}
