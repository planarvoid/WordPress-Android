
package com.soundcloud.android.objects;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event extends BaseObj implements Parcelable {

    public int id;

    public String type;

    public String tags;

    public String label;

    public Origin origin;

    public static class Origin extends Track {
        public Track track;
    }


    public Event() {
    }

    public Event(Parcel in) {
        readFromParcel(in);
    }

    public Track getTrack() {
        if (type.equalsIgnoreCase("track"))
            return origin;
        else if (type.equalsIgnoreCase("track-sharing"))
            return origin.track;
        return null;
    }

    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        public Event[] newArray(int size) {
            return new Event[size];
        }
    };
    
    public void writeToParcel(Parcel out, int flags) {
        buildParcel(out,flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
