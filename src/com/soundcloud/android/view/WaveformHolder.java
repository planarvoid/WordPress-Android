package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.soundcloud.android.view.CommentMarker.OnCommentClicked;

public class WaveformHolder extends RelativeLayout {
	
	private static final String TAG = "WaveformHolder" ;

	private Float initialXScale;
	private Matrix matrix;
	private ProgressBar mProgressBar;
	private OnCommentClicked commentClickedListener;
	
	private CommentMarker[] mCommentMarkers;
	private Boolean mCommentHit = false;
	
	private Float mTransformX;
	
	private int mRight;
	private int mWidth;
	
	private float[] checkMatrixValues = new float[9];
	
	public WaveformHolder(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		this.setWillNotDraw(false);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
	
		super.onLayout(changed, l, t, r, b);

		mRight = r;
		mWidth = r-l;
	}
	
	@Override
	protected void onDraw(Canvas canvas)
	{
		if (matrix != null){
			canvas.save();	
			canvas.concat(matrix);
		} else {
			canvas.restore();
		}
		
	    super.onDraw(canvas);
	    
	} 
	
	public void setInitialXScale(Float xScale){
		mTransformX = initialXScale = xScale;
		matrix = new Matrix();
		matrix.setScale(xScale, 1);
	}
	
	public Matrix setMatrix(Matrix m){
		
		float[] values = new float[9];
		m.getValues(values);
		mTransformX = values[0] = Math.min(1,Math.max(initialXScale, values[0]));
		Log.i(TAG,"Set Matrix  transform " + mTransformX + "|" + initialXScale + "|" + values[2] + "|" + ((RelativeLayout)this.getParent()).getWidth());
		values[2] = Math.max(-(this.getWidth()*values[0]-((WaveformController)((RelativeLayout)this.getParent()).getParent()).getWidth()),Math.min(0, values[2]));
		Log.i(TAG,"Set Matrix  other " + values[2]);
		m.setValues(values);
		return matrix = m;
	}
	
	public Matrix checkMatrix(Matrix m){
		
		
		m.getValues(checkMatrixValues);
		
		mTransformX = checkMatrixValues[0] = Math.min(1,Math.max(initialXScale, checkMatrixValues[0]));
		checkMatrixValues[2] = Math.max(-(this.getWidth()*checkMatrixValues[0]-((WaveformController)((RelativeLayout)this.getParent()).getParent()).getWidth()),Math.min(0, checkMatrixValues[2]));
		
		Matrix checkmatrix = new Matrix();
		checkmatrix.setValues(checkMatrixValues);
		return  checkmatrix;
	}
	
	public Float getTransformX(){
		return mTransformX;
	}
	
	
	public void setCommentMarkers(CommentMarker[] commentMarkers){
		mCommentMarkers = commentMarkers;
	}
	
	public Boolean commentHit(){
		return mCommentHit;
	}
	

//	@Override
//	public boolean dispatchTouchEvent(MotionEvent ev) {
//		super.dispatchTouchEvent(ev);
//		
//		if (ev.getAction() == MotionEvent.ACTION_DOWN || ev.getAction() == MotionEvent.ACTION_UP) {
//			
//			CommentMarker touchedMarker = null;
//			
//			float[] values = new float[9];
//			matrix.getValues(values);
//			
//			float tx = values[0];
//			float dx = values[2];
//			
//			Log.i("MARKER", "Setting x from " + ev.getRawX() + " " + tx);
//			ev.setLocation(ev.getRawX()/tx, ev.getY());
//			Log.i("MARKER", "Setting x to " + ev.getX());
//			
//			int x = (int) ev.getX();
//			int y = (int) ev.getY();
//
//			Rect hitRect = new Rect();
//			
//			
//			int count = mCommentMarkers.length;
//			for(int i = 0; i < count; i++) {
//				CommentMarker cm = mCommentMarkers[i];
//				cm.getHitRect(hitRect);
//				Log.i("MARKER", "Get Hit Rec " + hitRect.left + " " + hitRect.width() + " " + x);
//				if (hitRect.contains(x, y)) {
//					Log.i("MARKER","Comes from a marker");
//					
//					if (ev.getAction() == MotionEvent.ACTION_DOWN) mCommentHit = true;
//					mCommentMarkers[i].dispatchTransformedTouchEvent(ev);
//					return true;
//					
//				}
//			}
//			
//			mCommentHit = false;
//			
//		}
//		
//		return true;
//	}
	
	
}
