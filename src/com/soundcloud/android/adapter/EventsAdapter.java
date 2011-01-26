package com.soundcloud.android.adapter;

import java.util.ArrayList;

import android.os.Parcelable;
import android.util.Log;

import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.view.EventsRow;
import com.soundcloud.android.view.LazyRow;

public class EventsAdapter extends TracklistAdapter {
	
	public static final String IMAGE = "EventsAdapter_image";
	public static final String TAG = "EventsAdapter";
	
	protected String _playingId = "";
	protected int _playingPosition = -1;

	public EventsAdapter(LazyActivity context, ArrayList<Parcelable> data) {
		super(context, data);
	}
	
	@Override
	protected LazyRow createRow(){
		return new EventsRow(mActivity);
	}
	
	@Override
	public Track getTrackAt(int index)
	{
	  return (Track) ((Event) mData.get(index)).getTrack();
	}
		
}