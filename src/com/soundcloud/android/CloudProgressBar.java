package com.soundcloud.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class CloudProgressBar extends ProgressBar {

private static final String TAG_LOG = "CloudProgressBar";
private static final Paint mPaint = new Paint();

private ImageView _overlay;
private Context _context;
private ScPlayer _cloudplayer;



	public CloudProgressBar(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CloudProgressBar(ScPlayer cloudplayer, Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		_cloudplayer = cloudplayer;
	}
	
	public CloudProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		_context = context;
		
		//mPaint.setStrokeWidth(1.0f);
		//mPaint.setColor(Color.GRAY);
		//mPaint.setStyle(Paint.Style.STROKE);
		
		//setPadding(2,2,2,2);

	}
	
	public void setPlayer(ScPlayer ocPlayer){
		_cloudplayer = ocPlayer;
	}
	
	

	@Override
	protected synchronized void onDraw(Canvas canvas) {
	/*
	* I'm not sure the onDraw(Canvas) method from ProgressBar is
	correctly
	* implemented. Indeed. With padding values, The progressBar
	may be
	"clipped"
	*/
		
		//Log.d(TAG_LOG, "WIDTH " + this.getWidth());
       // canvas.rotate((float) (90*Math.PI/180));
		//canvas.save();
        //canvas.rotate(90);
		//canvas.translate(0,-50);
		//canvas.clipRect(0, 0, 200, 200);
		
		super.onDraw(canvas);
		//canvas.drawRect(0, 0, getWidth()-1, getHeight()-1,mPaint);
		//Log.d(TAG_LOG, "WIDTH " + this.getWidth());
		//canvas.restore();
	}


//	@Override
//	public boolean onTouchEvent(MotionEvent event) {
//		Log.d(TAG_LOG, "onTouchEvent");
//		int progress = getProgress();
//		
//		switch (event.getAction()) {
//			case MotionEvent.ACTION_DOWN:
//				Log.d(TAG_LOG, "onTouchEvent " + event.getX() + " " + this.getWidth());
//				float seekPercent = event.getX()/this.getWidth();
//				
//				((CloudPlayer)(_cloudplayer)).seekTo(seekPercent);
//				
//				break;
//				
//			case MotionEvent.ACTION_MOVE:
//				break;
//		
//			default:
//				break;
//		}
//	
//		
//		
//		return true;
//	}
	
	
   
	
	
    /**
     * <p>Define the drawable used to draw the progress bar in
     * progress mode.</p>
     *
     * @param d the new drawable
     *
     * @see #getProgressDrawable()
     * @see #setIndeterminate(boolean)
     
    public void setProgressDrawable(Drawable d) {
        if (d != null) {
            d.setCallback(this);

            // Make sure the ProgressBar is always tall enough
            int drawableHeight = d.getMinimumHeight();
            if (mMaxHeight < drawableHeight) {
                mMaxHeight = drawableHeight;
                requestLayout();
            }
        }
        mProgressDrawable = d;
        if (!mIndeterminate) {
            mCurrentDrawable = d;
            postInvalidate();
        }
    }
     */
	
	
	

}