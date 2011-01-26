package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.soundcloud.android.adapter.LazyEndlessAdapter;

public class LazyList extends ListView {
	
	private static final String TAG = "LazyList";

	public LazyList(Context context) {
        this(context, null);
    }
	
	public LazyList(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		
	}

	public LazyList(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);	
		

		
//		footerTextDone = new TextView(context);
//		footerTextDone.setGravity(Gravity.CENTER_HORIZONTAL);
//		footerTextDone.setLayoutParams(new ListView.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
//		footerTextDone.setText(context.getString(R.string.list_loading_done));
	}
	

	@Override 
	public ListAdapter getAdapter(){
		if (super.getAdapter() instanceof LazyEndlessAdapter){
			return ((LazyEndlessAdapter) super.getAdapter()).getWrappedAdapter();
		} else
			return super.getAdapter();
	}
	
	public LazyEndlessAdapter getWrapper(){
		if (super.getAdapter() instanceof LazyEndlessAdapter){
			return (LazyEndlessAdapter) super.getAdapter();
		} else
			return null;
	}
	
	@Override
	protected void layoutChildren(){
		//Log.i(TAG,"On Layout Children " + this.getId() + " " + this.getAdapter().getCount());
		try{
			super.layoutChildren();	
		} catch (Exception e){
			
		}
		
	}
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//Log.i(TAG,"On Measure " + this.getId() + " " + this.getAdapter().getCount());
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	 @Override 
     protected void onLayout(boolean changed, int l, int t, int r, int b) { 
		//Log.i(TAG,"On Layout " + this.getId() + " " + this.getAdapter().getCount());
		super.onLayout(changed, l, t, r, b);
	}
	 
	 @Override
	 protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		 //Log.i(TAG,"On Focus Changed " + this.getId() + " " + this.getAdapter().getCount());
		 super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
	 }
	 
	 @Override
	    protected void handleDataChanged() {
		 //Log.i(TAG,"Handle Data Changed" + this.getId() + " " + this.getAdapter().getCount());
		 super.handleDataChanged();
	 }
	 
	 @Override
	    protected void onAttachedToWindow() {
		 //Log.i(TAG,"list on attached to window " + this.getId());
	        super.onAttachedToWindow();
	    }

	    @Override
	    protected void onDetachedFromWindow() {
	    	//Log.i(TAG,"list on detached from window " + this.getId());
	        super.onDetachedFromWindow();
	    }

	    @Override
	    public void onWindowFocusChanged(boolean hasWindowFocus) {
	    	//Log.i(TAG,"onwindowfocuschanged " + this.getId());
	    	super.onWindowFocusChanged(hasWindowFocus);
	    }
	
}
