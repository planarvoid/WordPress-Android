package com.soundcloud.android;

import java.util.List;

import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

public class LazyBaseAdapter extends BaseAdapter implements Filterable {
	
	protected int mSelectedIndex = -1;
	protected LazyActivity mActivity;
	protected List<Parcelable> mData;
	
	protected int mPage = 1;
	protected Boolean mDone = false;
	
	@SuppressWarnings("unchecked")
	public LazyBaseAdapter(LazyActivity context, List<? extends Parcelable> data) {
		mData = (List<Parcelable>) data;
		mActivity = context;
	}

	public List<Parcelable> getData() {
		return mData;
	}
	
	public int getCount() {
		return mData.size();
	}

	public Object getItem(int location) {
		return mData.get(location);
	}

	public long getItemId(int i) {
		return i;
	}

	public View getView(int index, View row, ViewGroup parent)
	{
		Log.i("LAZY","GET VIEW");
		
		LazyRow rowView = null;
	
		if (row == null) {
			rowView = createRow();
		} else {
			rowView = (LazyRow) row;
		}
	
		// update the cell renderer, and handle selection state
		rowView.display(mData.get(index), mSelectedIndex == index);
		
		return rowView;
	
	}
	
	protected LazyRow createRow(){
		return new LazyRow(mActivity);
	}
	
	public void setSelected(int position){
		mSelectedIndex = position;
	}
	
	public void clear(){
		mData.clear();
		reset();
	}
	
	public void reset(){
		mPage = 1;
		mSelectedIndex = -1;
	}
	public void incrementPage() {
		mPage++;
	}
	
	public int getPage() {
		return mPage;
	}
	
	public void setStopLoading(boolean done) {
		mDone = done;
	}

	public Filter getFilter() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
