package com.soundcloud.android;

import java.util.ArrayList;

import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;

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
	  return (Track) ((Event) mData.get(index)).getDataParcelable(Event.key_track);
	}
		
}