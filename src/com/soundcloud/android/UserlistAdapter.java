package com.soundcloud.android;

import java.util.ArrayList;

import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class UserlistAdapter extends LazyBaseAdapter {
	
	public static final String IMAGE = "UserlistAdapter_image";
	public static final String TAG = "UserlistAdapter";

	public UserlistAdapter(LazyActivity context, ArrayList<Parcelable> data) {
		super(context, data);
	}
	
	@Override
	public View getView(int index, View row, ViewGroup parent)
	{
	
		UserlistRow rowView = null;
	
		if (row == null) {
			rowView = (UserlistRow) createRow();
		} else {
			rowView = (UserlistRow) row;
		}
	
		// update the cell renderer, and handle selection state
		rowView.display(mData.get(index), mSelectedIndex == index);
		
		return rowView;
	
	}

	@Override
	protected LazyRow createRow(){
		return new UserlistRow(mActivity);
	}
		
}