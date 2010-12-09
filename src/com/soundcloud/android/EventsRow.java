package com.soundcloud.android;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.utils.RemoteImageView;

public class EventsRow extends TracklistRow {
	
	  public EventsRow(Context _context) {
		  super(_context);
	  }
	  
	  @Override
	  protected Track getTrackFromParcelable(Parcelable p){
		  return (Track) ((Event) p).getDataParcelable(Event.key_track);
	  }
	  
	 
	
	  
	
}
