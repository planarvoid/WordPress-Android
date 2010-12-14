package com.soundcloud.android.view;

import android.content.Context;
import android.os.Parcelable;

import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;

public class EventsRow extends TracklistRow {
	
	  public EventsRow(Context _context) {
		  super(_context);
	  }
	  
	  @Override
	  protected Track getTrackFromParcelable(Parcelable p){
		  return (Track) ((Event) p).getDataParcelable(Event.key_track);
	  }
	  
	 
	
	  
	
}
