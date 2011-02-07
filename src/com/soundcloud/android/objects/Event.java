
package com.soundcloud.android.objects;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event extends BaseObj implements Parcelable {

    public int id;

    public String type;

    public String tags;

    public String label;

    public Origin origin;

    public static class Origin extends Track {
        public Track sharedTrack;
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
            return origin.sharedTrack;

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

    @Override
    public int describeContents() {
        return 0;
    }

}
