package com.soundcloud.android.adapter;

import java.util.ArrayList;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistRow;

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