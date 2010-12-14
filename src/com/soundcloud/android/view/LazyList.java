package com.soundcloud.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.soundcloud.android.adapter.LazyEndlessAdapter;

public class LazyList extends ListView {

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
	
}
