package com.soundcloud.android.view.quickaction;

import android.content.Context;

import android.graphics.*;
import android.graphics.drawable.Drawable;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import com.soundcloud.android.R;

/**
 * Popup window, shows action list as icon and text like the one in Gallery3D app. 
 * 
 * @author Lorensius. W. T
 */
public class QuickAction extends PopupWindows {
	private View mRootView;
	private LayoutInflater inflater;
	private ViewGroup mTrack;
	private ScrollView mScroller;
	private OnActionItemClickListener mListener;
	
	protected static final int ANIM_GROW_FROM_LEFT = 1;
	protected static final int ANIM_GROW_FROM_RIGHT = 2;
	protected static final int ANIM_GROW_FROM_CENTER = 3;
	protected static final int ANIM_REFLECT = 4;
	protected static final int ANIM_AUTO = 5;
	
	private int mChildPos;
	private int animStyle;
    private BubbleDrawable mBgDrawable;

    private int mArrowWidth;
    private final int ARROW_HEIGHT = 15;
    private final int ARROW_WIDTH = 15;

    /**
	 * Constructor.
	 * 
	 * @param context Context
	 */
	public QuickAction(Context context) {
		super(context);
		
		inflater 		= (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		setRootViewId(R.layout.quickaction_popup);
	   
		animStyle		= ANIM_AUTO;
		mChildPos		= 0;

        Paint bgPaint = new Paint();
        bgPaint.setColor(0xFFFFFFFF);
        bgPaint.setAntiAlias(true);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setMaskFilter(new BlurMaskFilter(1, BlurMaskFilter.Blur.INNER));

        mBgDrawable = new BubbleDrawable();
        mBgDrawable.setBgPaint(bgPaint);
        mBgDrawable.setBgGradientColors(new int[]{0xffe2e2e2, 0xffd7d7d7, 0xffe2e2e2});

        Paint linePaint = new Paint();
        linePaint.setColor(0xFF000000);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);

        mBgDrawable.setLinePaint(linePaint);

        final float density = context.getResources().getDisplayMetrics().density;
        mArrowWidth = (int) (ARROW_WIDTH * density);
        mBgDrawable.setArc((int) (5 * density));
        mBgDrawable.setArrowWidth(mArrowWidth);
        mBgDrawable.setArrowHeight((int) (ARROW_HEIGHT * density));
        setBackgroundDrawable(mBgDrawable);
	}

	/**
	 * Set root view.
	 * 
	 * @param id Layout resource id
	 */
	public void setRootViewId(int id) {
		mRootView	= (ViewGroup) inflater.inflate(id, null);
		mTrack 		= (ViewGroup) mRootView.findViewById(R.id.tracks);
		mScroller	= (ScrollView) mRootView.findViewById(R.id.scroller);
		
		setContentView(mRootView);
	}
	
	/**
	 * Set animation style
	 * 
	 * @param animStyle animation style, default is set to ANIM_AUTO
	 */
	public void setAnimStyle(int animStyle) {
		this.animStyle = animStyle;
	}
	
	/**
	 * Set listener for action item clicked.
	 * 
	 * @param listener Listener
	 */
	public void setOnActionItemClickListener(OnActionItemClickListener listener) {
		mListener = listener;

        for (int i = 0; i < mTrack.getChildCount(); i++){
            if (mTrack.getChildAt(i) instanceof ActionItem) {
                final ActionItem actionItem = (ActionItem) mTrack.getChildAt(i);
                if (mListener == null) {
                    actionItem.setOnClickListener(null);
                } else {
                    final int finalI = i;
                    actionItem.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.onItemClick(finalI);
                            dismiss();
                        }
                    }
                    );
                }
            }
        }
	}
	
	/**
	 * Add action item
	 * 
	 * @param action  {@link ActionItem}
	 */
	public void addActionItem(ActionItem actionItem) {
		final int pos =  mChildPos;
		
		actionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mListener != null) mListener.onItemClick(pos);
				dismiss();
			}
		});

		actionItem.setFocusable(true);
		actionItem.setClickable(true);

        if (mTrack.getChildCount() > 0){
            View v = new View(mContext);
            v.setBackgroundColor(0xffb5b5b5);
            mTrack.addView(v,new LinearLayout.LayoutParams((int) (1*mContext.getResources().getDisplayMetrics().density),LayoutParams.FILL_PARENT));
        }

		mTrack.addView(actionItem);
		
		mChildPos++;
	}
	
	/**
	 * Show popup window. Popup is automatically positioned, on top or bottom of anchor view.
	 * 
	 */
	public void show (View anchor) {
		preShow();
		
		int xPos, yPos;
		
		int[] location 		= new int[2];
	
		anchor.getLocationOnScreen(location);

		Rect anchorRect 	= new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1] 
		                	+ anchor.getHeight());

		mRootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		mRootView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	
		int rootHeight 		= mRootView.getMeasuredHeight();
		int rootWidth		= mRootView.getMeasuredWidth();
		
		int screenWidth 	= mWindowManager.getDefaultDisplay().getWidth();
		int screenHeight	= mWindowManager.getDefaultDisplay().getHeight();
		
		//automatically get X coord of popup (top left)
		if ((anchorRect.left + rootWidth) > screenWidth) {
			xPos = anchorRect.left - (rootWidth-anchor.getWidth());
		} else {
			if (anchor.getWidth() > rootWidth) {
				xPos = anchorRect.centerX() - (rootWidth/2);
			} else {
				xPos = anchorRect.left;
			}
		}
		
		int dyTop			= anchorRect.top;
		int dyBottom		= screenHeight - anchorRect.bottom;

		boolean onTop		= (dyTop > dyBottom) ? true : false;

		if (onTop) {
			if (rootHeight > dyTop) {
				yPos 			= 15;
				LayoutParams l 	= mScroller.getLayoutParams();
				l.height		= dyTop - anchor.getHeight();
			} else {
				yPos = anchorRect.top - rootHeight;
			}
		} else {
			yPos = anchorRect.bottom;
			
			if (rootHeight > dyBottom) { 
				LayoutParams l 	= mScroller.getLayoutParams();
				l.height		= dyBottom;
			}
		}
		
		mBgDrawable.setArrowTop(!onTop);
        mBgDrawable.setArrowOffsetX(anchorRect.centerX()-xPos);

		setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);
		
		mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
	}
	
	/**
	 * Set animation style
	 * 
	 * @param screenWidth screen width
	 * @param requestedX distance from left edge
	 * @param onTop flag to indicate where the popup should be displayed. Set TRUE if displayed on top of anchor view
	 * 		  and vice versa
	 */
	private void setAnimationStyle(int screenWidth, int requestedX, boolean onTop) {
		int arrowPos = requestedX - mArrowWidth/2;

		switch (animStyle) {
		case ANIM_GROW_FROM_LEFT:
			mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left : R.style.Animations_PopDownMenu_Left);
			break;
					
		case ANIM_GROW_FROM_RIGHT:
			mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
			break;
					
		case ANIM_GROW_FROM_CENTER:
			mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center : R.style.Animations_PopDownMenu_Center);
		break;
			
		case ANIM_REFLECT:
			mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Reflect : R.style.Animations_PopDownMenu_Reflect);
		break;
		
		case ANIM_AUTO:
			if (arrowPos <= screenWidth/4) {
				mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left : R.style.Animations_PopDownMenu_Left);
			} else if (arrowPos > screenWidth/4 && arrowPos < 3 * (screenWidth/4)) {
				mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center : R.style.Animations_PopDownMenu_Center);
			} else {
				mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
			}
					
			break;
		}
	}
	

	/**
	 * Listener for item click
	 *
	 */
	public interface OnActionItemClickListener {
		public abstract void onItemClick(int pos);
	}
}