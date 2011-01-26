package com.soundcloud.android.adapter;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.view.LazyRow;

public class LazyExpandableBaseAdapter extends BaseExpandableListAdapter implements Filterable {
	
	protected int mSelectedGroupIndex = -1;
	protected int mSelectedChildIndex = -1;
	
	protected LazyActivity mActivity;
	protected ArrayList<Parcelable> mGroupData;
	protected ArrayList<ArrayList<Parcelable>> mChildData;
	
	protected int mPage = 0;
	protected Boolean mDone = false;
	
	@SuppressWarnings("unchecked")
	public LazyExpandableBaseAdapter(LazyActivity context, List<Parcelable> groupData,ArrayList<ArrayList<Parcelable>> mTrackData) {
		mGroupData = (ArrayList<Parcelable>) groupData;
		mChildData = mTrackData;
		mActivity = context;
		
	}

	public void setChildData(ArrayList<ArrayList<Parcelable>> childData) {
		mChildData = childData;
	}
	
	public void setGroupData(ArrayList<Parcelable> groupData) {
		mGroupData = groupData;
	}
	
	public ArrayList<ArrayList<Parcelable>> getChildData() {
		return mChildData;
	}
	
	public List<Parcelable> getGroupData() {
		return mGroupData;
	}
	
	
	
	protected LazyRow createChildRow(){
		return new LazyRow(mActivity);
	}
	
	protected LazyRow createGroupRow(){
		return new LazyRow(mActivity);
	}
	
	
	
	public void setSelected(int groupPosition, int childPosition){
		mSelectedGroupIndex = groupPosition;
		mSelectedChildIndex = childPosition;
	}
	
	public void reset(){
		mPage = 1;
	}
	
	public void clear(){
		reset();
		mGroupData.clear();
		mChildData.clear();
		notifyDataSetChanged();
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

	public Object getChild(int groupIndex, int childIndex) {
		if (mChildData != null)
			return mChildData.get(groupIndex).get(childIndex);
		else
			return null;
	}

	public long getChildId(int groupIndex, int childIndex) {
		return childIndex;
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View row, ViewGroup parent) {
		LazyRow rowView = null;
		
		if (row == null) {
			rowView = createChildRow();
		} else {
			rowView = (LazyRow) row;
		}
		
		// update the cell renderer, and handle selection state
		rowView.display(mChildData.get(groupPosition).get(childPosition), (mSelectedGroupIndex == groupPosition && mSelectedChildIndex == childPosition));
		
		return rowView;
	}

	public int getChildrenCount(int groupPosition) {
		if (mChildData != null)
			return mChildData.get(groupPosition).size();
		else
			return 0;
	}

	public Object getGroup(int groupIndex) {
		if (mGroupData != null)
			return mGroupData.get(groupIndex);
		else
			return null;
	}

	public int getGroupCount() {
		if (mGroupData != null)
			return mGroupData.size();
		else
			return 0;
	}

	public long getGroupId(int groupIndex) {
		return groupIndex;
	}

	public View getGroupView(int groupPosition, boolean isExpanded, View row, ViewGroup parent) {
		LazyRow rowView = null;
		
		if (row == null) {
			rowView = createGroupRow();
		} else {
			rowView = (LazyRow) row;
		}
		
		// update the cell renderer, and handle selection state
		rowView.display(mGroupData.get(groupPosition), false);
		
		return rowView;
	}

	public boolean hasStableIds() {
		return false;
	}

	public boolean isChildSelectable(int arg0, int arg1) {
		return true;
	}
	
	
}
