package com.soundcloud.android.view;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.adapter.LazyExpandableBaseAdapter;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;

public class WaveformController extends FrameLayout implements OnTouchListener, OnClickListener, OnLongClickListener {
	private static final String TAG = "Touch" ;

	private Track mPlayingTrack;
	private Drawable mLoadingWaveform;
	private Drawable mPendingWaveform;
	private Boolean mPendingComments = false;
	
	private RemoteImageView mOverlay;
	private ProgressBar mProgressBar;
	private RelativeLayout mCommentBar;
	private RelativeLayout mTrackTouchBar;
	private WaveformHolder mWaveformHolder;
	private RelativeLayout mWaveformFrame;
	
	private CommentMarker mAddCommentMarker;
	private Float mLastAddCommentTimerX;
	
	private CommentMarker[] mCommentMarkers;
	private int[] mCommentTimestamps;
	private String mLastCommentTimestamp = "";
	
	private ScPlayer mPlayer;
	private Context mContext;
	
	private int mDuration;
	private LazyExpandableBaseAdapter mCommentsAdapter;
	
	private Float initialWaveScaleX;
	private Boolean mLandscape = false; 
	
	static final int maxWavesStored = 5;
	
	
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
	   static final int NONE = 0;
	   static final int DRAG = 1;
	   static final int ZOOM = 2;
	   static final int DRAG_ZOOM = 3;
	   int mode = NONE;
	   
	   
	   
	   SharedPreferences mPrefernces;

	
	public WaveformController(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;
		
		mPrefernces = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		LayoutInflater inflater = (LayoutInflater) context
		.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.waveformcontroller, this);
		
		mWaveformFrame = (RelativeLayout) findViewById(R.id.waveform_frame);
		mWaveformHolder = (WaveformHolder) findViewById(R.id.waveform_holder);
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
		//mCommentBar = (RelativeLayout) findViewById(R.id.comment_bar);
		mTrackTouchBar = (RelativeLayout) findViewById(R.id.track_touch_bar);
		mTrackTouchBar.setOnTouchListener(this);
		mOverlay = (RemoteImageView) findViewById(R.id.progress_overlay);
		
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
		
		/*if (!mPrefernces.getBoolean("showLiveComments", true))
			return;
		
		if (mCommentTimestamps == null || mCommentTimestamps.length == 0)
			return;
		
		for (int i = 0; i < mCommentTimestamps.length; i++){
			if (mCommentTimestamps[i] > pos){
				if (i > 0){
//					Log.i("COMMENTS", " CHecking " + mCommentTimestamps[i-1] + " " + pos);
					if (pos - mCommentTimestamps[i-1] < 1100 && mCommentTimestamps[i-1] > 0){
						showComment(i-1);
					}
				}
				break;
			}
		}*/
		
	}
	
	public void setSecondaryProgress(int percent){
		mProgressBar.setSecondaryProgress(percent);
	}
	
	public void showComment(int i){
		
		CharSequence toastText = "";
		
		Comment comment = (Comment) mCommentsAdapter.getGroupData().get(i);
		
		if (mLastCommentTimestamp.contentEquals(comment.getData(Comment.key_timestamp)))
			return;
		
		mLastCommentTimestamp = comment.getData(Comment.key_timestamp);
		
		toastText = comment.getData(Comment.key_username) + ": " + comment.getData(Comment.key_body);
		ArrayList<Parcelable> childComments = mCommentsAdapter.getChildData().get(i);
		for (Parcelable childComment : childComments){
			toastText = toastText + "\n\n   " + ((Comment) childComment).getData(Comment.key_username) + ": " + ((Comment) childComment).getData(Comment.key_body);
		}
		
		Toast toast = Toast.makeText(mContext, toastText, Toast.LENGTH_LONG);
		toast.show(); 
		
//		LayoutInflater inflater = mContext.getLayoutInflater();
//		View layout = inflater.inflate(R.layout.comments, (ViewGroup) findViewById(R.id.comment_layout_root));
//
//		TextView text = (TextView) layout.findViewById(R.id.text);
//		text.setText(toastText);
//
//		Toast toast = new Toast(mContext);
//		toast.setDuration(Toast.LENGTH_LONG);
//		toast.setView(layout);
//		
//		toast.show();
		
	}
	
	public void flashComment(String timestamp){
		
		for (CommentMarker commentMarker : mCommentMarkers){
			if (commentMarker.getTimestamp() == Integer.parseInt(timestamp))
				commentMarker.flashOn();
			 else
				commentMarker.flashOff();
		}
		
		removeCallbacks(flashTimer);
		handler.postDelayed(flashTimer, 1000);
		
		mWaveformHolder.invalidate();
	}
	
	 
	 private Runnable flashTimer = new Runnable() {
	   	public void run() {
	   		for (CommentMarker commentMarker : mCommentMarkers){
				commentMarker.flashOff();
			}
	   		mWaveformHolder.invalidate();
	   	}

	   };
	
	public void setPlayer(ScPlayer ocPlayer){
		mPlayer = ocPlayer;
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		
		
		if (mPendingComments){
			doCommentMap();
			mPendingComments = false;
		}
	}
	
	public void mapComments(LazyExpandableBaseAdapter commentsAdapter, int duration){
		mCommentsAdapter = commentsAdapter;
		mDuration = duration;
		
		
		
		//if (mOverlay != null)
			doCommentMap();
//		else
	//		mPendingComments = true;
	}
	
	private void doCommentMap(){
		if (mCommentMarkers != null){
			for (CommentMarker marker : mCommentMarkers){
				mWaveformHolder.removeView(marker);
				marker = null;
			}
		}
		
		if (mCommentsAdapter == null){
			return;
		}
		
		if (mCommentsAdapter.getGroupCount() == 0){
			mCommentMarkers = null;
			return;
		}
		
		mCommentMarkers = new CommentMarker[mCommentsAdapter.getGroupCount()];
		mCommentTimestamps = new int[mCommentsAdapter.getGroupCount()];
			
			//Collections.sort(mCommentsAdapter.getGroupData(), commentComparator);
			//Collections.sort(mCommentsAdapter.getChildData(), commentComparator);
		
		final float scale = getContext().getResources().getDisplayMetrics().density;
			
		int i = 0;
		for (Parcelable commentParcelable : mCommentsAdapter.getGroupData()){
			Comment comment = (Comment) commentParcelable; 
			
			int leftMargin = (int) (scale*1800*(Float.parseFloat((comment).getData(Comment.key_timestamp))/mDuration));
			
			CommentMarker cm = new CommentMarker(mContext);
			cm.setCommentData((comment));
			cm.setLeftMargin(leftMargin);
			cm.setOnClickListener(this);
			
			((LazyActivity) mContext).registerForContextMenu(cm);
			mWaveformHolder.addView(cm);
			mCommentMarkers[i] = cm;
			mCommentTimestamps[i] = Integer.parseInt((comment).getData(Comment.key_timestamp));
			i++;
		}
		
		
		
		this.requestLayout();
	}
	
	 public void onClick(View target) {
		 if (mPlayer == null)
			 return;
		 
	    if (target.getClass().getName().contentEquals(CommentMarker.class.getName())){
	    	Integer ts = ((CommentMarker) target).getTimestamp();
	    	mPlayer.seekTo(ts.floatValue() / mDuration);
	    	mLastCommentTimestamp = "";
	    }
	 }
	
	
	
	
	public void updateTrack(Track track) {
		
		if (mPlayingTrack != null)
			if (mPlayingTrack.getData(Track.key_id).contentEquals(track.getData(Track.key_id)))
				return;
		
		mPlayingTrack = track;
		mDuration = Integer.parseInt(mPlayingTrack.getData(Track.key_duration));
		loadWaveDrawable(CloudUtils.getTrackWaveformPath(track));
	}
	
	public void loadWaveDrawable(final String wavePath){
		
		mOverlay.setLocalURI(CloudUtils.getCacheDirPath(mContext)+"/waves/"+CloudUtils.getCacheFileName(wavePath));
		File local = new File(CloudUtils.getCacheDirPath(mContext)+"/waves/"+CloudUtils.getCacheFileName(wavePath));
		if (local.exists()){
			mOverlay.loadImage();
			return;
		}
		
		File oldestWave = null;
		File waveDir = new File(CloudUtils.getCacheDirPath(mContext)+"/waves"); 
		Boolean exists = false;
		if (waveDir.listFiles().length > maxWavesStored){
			
			for (File wave : waveDir.listFiles()){
				if (CloudUtils.getCacheFileName(wavePath).contentEquals(wave.getName())){
					exists = true;
					break;
				} if (oldestWave == null || wave.lastModified() < oldestWave.lastModified()){
					oldestWave = wave;
				}
			}
			oldestWave.delete();
		}
	
		mOverlay.setTemporaryDrawable(mContext.getResources().getDrawable(R.drawable.player_wave_bg));
		mOverlay.setRemoteURI(wavePath);
		mOverlay.loadImage();
	}
	
	private void resetTransform(){
		Integer w = getWidth();
		initialWaveScaleX = w.floatValue()/mOverlay.getWidth();
		
		matrix = new Matrix();
		matrix.setScale(initialWaveScaleX, 1);
		mWaveformHolder.setInitialXScale(initialWaveScaleX);
		mWaveformFrame.recomputeViewAttributes(mWaveformHolder);
		mWaveformHolder.invalidate();
	}
	

	
	private float calculateSeek (Float touchX){
		float[] values = new float[9];
		matrix.getValues(values);
		
		//float visibleWidth = initialWaveScaleX/values[0] * mWaveformHolder.getWidth();
		//float visibleWidth = mWaveformHolder.getWidth();
		//float startWidth = -(values[2]/values[0]);
		//float seekPercent = (startWidth + visibleWidth*(touchX/getWidth()))/mWaveformHolder.getWidth();
		float seekPercent = touchX/mWaveformHolder.getWidth();
		return seekPercent;
	}
	
	
	
	private int calculateLeftMargin (Float touchX){
		float[] values = new float[9];
		matrix.getValues(values);
		
		float visibleWidth = initialWaveScaleX/values[0] * mWaveformHolder.getWidth();
		float startWidth = -(values[2]/values[0]);
		return (int) (startWidth + visibleWidth*(touchX/getWidth()));
	}
	
	   public boolean onTouch(View v, MotionEvent event) {
	      
		   // Dump touch event to log
		      dumpEvent(event);
		   
		   if (v == mCommentBar){
			   
			   // Handle touch events here...
			      switch (event.getAction() & MotionEvent.ACTION_MASK) {
			      case MotionEvent.ACTION_DOWN:
			    	  if (mAddCommentMarker != null && mAddCommentMarker.getParent() == mWaveformHolder){
			    		  mWaveformHolder.removeView(mAddCommentMarker);
			    		  mAddCommentMarker = null;
			    	  }
			    	  
			    	  
			    	  mAddCommentMarker = new CommentMarker(mContext);
			    	  mAddCommentMarker.setOnLongClickListener(this);
			    	  mAddCommentMarker.setLeftMargin(calculateLeftMargin(event.getX()));
			    	  mAddCommentMarker.setTimestamp((int) (calculateSeek(event.getX())*mDuration));
			    	  mWaveformHolder.addView(mAddCommentMarker);
			    	  mAddCommentMarker.requestLayout();
			    	  
			    	  mLastAddCommentTimerX = event.getX();
			    	  
			    	  handler.removeCallbacks(addCommentTimer);
			    	  handler.postDelayed(addCommentTimer, 1000);
			    	  
			         break;
			      case MotionEvent.ACTION_UP:
			    	  if (mAddCommentMarker != null && mAddCommentMarker.getParent() == mWaveformHolder){
			    		  mWaveformHolder.removeView(mAddCommentMarker);
			    	  }
			    	  handler.removeCallbacks(addCommentTimer);
			    	  mAddCommentMarker = null;
			         break;
			      case MotionEvent.ACTION_MOVE:
			    	  if (mLastAddCommentTimerX != null && Math.abs(mLastAddCommentTimerX - event.getX()) > TOUCH_MOVE_TOLERANCE){
			    		  mLastAddCommentTimerX = event.getX();
			    		  
			    		  if (mAddCommentMarker != null){
				    		  mAddCommentMarker.setLeftMargin(calculateLeftMargin(event.getX()));
				    		  mAddCommentMarker.setTimestamp((int) (calculateSeek(event.getX())*mDuration));
				    	  }
			    		  
			    		  handler.removeCallbacks(addCommentTimer);
				    	  handler.postDelayed(addCommentTimer, 1000);
			    	  }
			    	  
			         break;
			      }
			   
		   } else {
			  
			   
			  
			   switch (event.getAction() & MotionEvent.ACTION_MASK) {
			      case MotionEvent.ACTION_DOWN:
			    	  if (mPlayer != null) mPlayer.seekTo(calculateSeek(event.getX()));
			    	  break;
			   }
			   
			   /*
			   
			   
			   Log.i(TAG,"Handle it event.getX " + event.getX() + "|" + event.getAction());
			   
			      // Handle touch events here...
			      switch (event.getAction() & MotionEvent.ACTION_MASK) {
			      case MotionEvent.ACTION_DOWN:
			         savedMatrix.set(matrix);
			         start.set(event.getX(), event.getY());
			         mode = DRAG;
			         break;
			      case MotionEvent.ACTION_POINTER_DOWN:
			    	  Log.d(TAG, "ACTIONPOINTERDOWN");
			         oldDist = spacing(event);

			         if (oldDist > 10f) {
			           savedMatrix.set(matrix);
			           midPoint(mid, event);
			           mode = ZOOM;
			            Log.d(TAG, "mode=ZOOM");
			         }
			         break;
			      case MotionEvent.ACTION_UP:
			    	  if (mode == DRAG){
			    		  if ( Math.abs(event.getX() - start.x) < SEEK_TOLERANCE){
			    			  if (mPlayer != null)
			    				  mPlayer.seekTo(calculateSeek(event.getX()));
			    			  mLastCommentTimestamp = "";
			    			  //mPlayer.seekTo(calculateSeek(event.getX())); 
			    		  }
			    	  }
			      case MotionEvent.ACTION_POINTER_UP:
			         mode = NONE;
			         oldDist = 0;
			         break;
			      case MotionEvent.ACTION_MOVE:
			         if (mode == DRAG) {
			            matrix.set(savedMatrix);
			            Log.i("checking","Translate " + event.getX() + " " + start.x);
			            matrix.postTranslate(event.getX() - start.x,0);
			            if (!mWaveformHolder.checkMatrix(matrix).equals(matrix) && event.getX() - start.x != 0){
			            	Log.i("checking","fake zooming");
			            	//matrix.set(savedMatrix);
			            	if (oldDist == 0){
			            		if (event.getX() - start.x > 0)
			            			fake.set(0,event.getY());
			            		else
			            			fake.set(getWidth(),event.getY());
			            		
			            		oldDist = spacing(event.getX(),event.getY(),fake.x, fake.y);
			            	} else {
			            		float newDist = spacing(event.getX(),event.getY(),fake.x, fake.y);
			            		if (newDist > 10f){
			            			float scale =  newDist / oldDist;
							        matrix.postScale(scale, 1, fake.x, 0);	
			            		}
			            			
			            	}
			            }
			         } else if (mode == ZOOM) {
			            float newDist = spacing(event);
			            Log.d(TAG, "newDist=" + newDist + "|" + oldDist);
			            if (newDist > 10f) {
			               matrix.set(savedMatrix);
			               float scale = newDist / oldDist;
			               matrix.postScale(scale, 1, mid.x, 0);
			            }
			         }
			
			         
			         break;
		   }
			      
			     // matrix = mWaveformHolder.setMatrix(matrix);
			              */
		   }
		   
		   //mWaveformHolder.invalidate();

	      return true; // indicate event was handled
	   }
	   
	   private Handler handler = new Handler();
	   private Runnable addCommentTimer = new Runnable() {

	   	public void run() {
	   		Comment mAddComment = new Comment();
	   		mAddComment.putData(Comment.key_timestamp, Integer.toString(mAddCommentMarker.getTimestamp()));
	   		mAddComment.putData(Comment.key_timestamp_formatted, CloudUtils.formatTimestamp(mAddCommentMarker.getTimestamp()));
	   		mAddComment.putData(Comment.key_track_id, mPlayingTrack.getData(Track.key_id));
			
			((ScPlayer) mContext).addCommentPrompt(mAddComment);
	   	}

	   };
	  
	
	  
	  private float spacing(MotionEvent event) {
		   float x = event.getX(0) - event.getX(1);
		   float y = event.getY(0) - event.getY(1);
		   return FloatMath.sqrt(x * x + y * y);
		}
	  
	  private float spacing(float x1,float y1, float x2, float y2) {
		   float x = x1 - x2;
		   float y = y1 - y2;
		   return FloatMath.sqrt(x * x + y * y);
		}
	  
	  private void midPoint(PointF point, MotionEvent event) {
		   float x = event.getX(0) + event.getX(1);
		   float y = event.getY(0) + event.getY(1);
		   point.set(x / 2, y / 2);
		}
	  
	  private void midPoint(PointF point, float x1,float y1, float x2, float y2) {
		   float x = x1 + x2;
		   float y = y1 + y2;
		   point.set(x / 2, y / 2);
		}

	/** Show an event in the LogCat view, for debugging */
	private void dumpEvent(MotionEvent event) {
	   String names[] = { "DOWN" , "UP" , "MOVE" , "CANCEL" , "OUTSIDE" ,
	      "POINTER_DOWN" , "POINTER_UP" , "7?" , "8?" , "9?" };
	   StringBuilder sb = new StringBuilder();
	   int action = event.getAction();
	   int actionCode = action & MotionEvent.ACTION_MASK;
	   sb.append("event ACTION_" ).append(names[actionCode]);
	   if (actionCode == MotionEvent.ACTION_POINTER_DOWN
	         || actionCode == MotionEvent.ACTION_POINTER_UP) {
	      sb.append("(pid " ).append(
	      action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
	      sb.append(")" );
	   }
	   sb.append("[" );
	   for (int i = 0; i < event.getPointerCount(); i++) {
	      sb.append("#" ).append(i);
	      sb.append("(pid " ).append(event.getPointerId(i));
	      sb.append(")=" ).append((int) event.getX(i));
	      sb.append("," ).append((int) event.getY(i));
	      if (i + 1 < event.getPointerCount())
	         sb.append(";" );
	   }
	   sb.append("]" );
	   Log.d(TAG, sb.toString());
	}

	public boolean onLongClick(View v) {
		return true;
	}
	
	
	
	
}
