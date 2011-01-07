package com.soundcloud.android.adapter;

import java.util.ArrayList;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.TracklistRow;

public class TracklistAdapter extends LazyBaseAdapter {
	
	public static final String IMAGE = "TracklistAdapter_image";
	public static final String TAG = "TracklistAdapter";
	
	protected String _playingId = "";
	protected int _playingPosition = -1;

	public TracklistAdapter(LazyActivity context, ArrayList<Parcelable> data) {
		super(context, data);
	}
	
	@Override
	public View getView(int index, View row, ViewGroup parent)
	{
	
		TracklistRow rowView = null;
	
		if (row == null) {
			rowView = (TracklistRow) createRow();
		} else {
			rowView = (TracklistRow) row;
		}

		rowView.display(mData.get(index), mSelectedIndex == index,(_playingId != null &&  getTrackAt(index).getData(Track.key_id).contentEquals(_playingId)));
		
		BindResult result = BindResult.ERROR;
		try{ //put the bind in a try catch to catch any loading error (or the occasional bad url) 
			result = mImageLoader.bind(this, rowView.getRowIcon(), rowView.getIconRemoteUri());
		} catch (Exception e){};
		rowView.setTemporaryDrawable(result);
		return rowView;
	}

	@Override
	protected LazyRow createRow(){
		return new TracklistRow(mActivity);
	}
	
	public Track getTrackAt(int index)
	{
		return (Track) mData.get(index);
	}
	
	public void setPlayingId(String playingId){
		_playingId = playingId;	
		notifyDataSetChanged();
	}
	
	public void setPlayingPosition(int position){
		_playingPosition = position;	
	}
		
}