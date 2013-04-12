package com.soundcloud.android.model.act;

import com.soundcloud.android.model.PlayableHolder;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class TrackSharingActivity extends TrackActivity implements PlayableHolder {

    public TrackSharingActivity() {
        super();
    }

    public TrackSharingActivity(Cursor cursor) {
        super(cursor);
    }

    public TrackSharingActivity(Parcel in) {
        super(in);
    }

    @Override
    public Type getType() {
        return Type.TRACK_SHARING;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest,flags);
        dest.writeParcelable(track, 0);
    }

    public static final Parcelable.Creator<TrackSharingActivity> CREATOR = new Parcelable.Creator<TrackSharingActivity>() {
        public TrackSharingActivity createFromParcel(Parcel in) {
            return new TrackSharingActivity(in);
        }
        public TrackSharingActivity[] newArray(int size) {
            return new TrackSharingActivity[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrackSharingActivity)) return false;
        if (!super.equals(o)) return false;

        TrackSharingActivity that = (TrackSharingActivity) o;
        if (track != null ? !track.equals(that.track) : that.track != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (track != null ? track.hashCode() : 0);
        return result;
    }
}
