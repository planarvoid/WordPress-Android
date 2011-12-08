
package com.soundcloud.android.model;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event extends ModelBase implements Origin {
    @JsonProperty public Date created_at;
    @JsonProperty public String type;
    @JsonProperty public String tags;

    public Origin origin;

    private CharSequence mElapsedTime;
    public String next_href;

    public Event() {
    }

    public Event(Parcel in) {
        readFromParcel(in);
    }

    public User getUser() {
        return origin == null ?  null : origin.getUser();
    }

    public Track getTrack() {
        return origin == null ? null : origin.getTrack();
    }

    public Comment getComment() {
        return origin instanceof Comment ? (Comment) origin : null;
    }

    @Override
    public void resolve(SoundCloudApplication application) {
        if (origin instanceof Comment) {
            Comment c = (Comment)origin;
            if (c.track.user == null) {
                 c.track.user = application.getLoggedInUser();
            }
        } else if (type.contentEquals(Types.FAVORITING)) {
            if (getTrack().user == null) {
                getTrack().user = application.getLoggedInUser();
            }
        }
    }

    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    public CharSequence getElapsedTime(Context c) {
        if (mElapsedTime == null) {
            mElapsedTime = CloudUtils.getTimeElapsed(c.getResources(),created_at.getTime());
        }
        return mElapsedTime;
    }

    public Class<?> getView(Class<?> defaultView) {
        if (isTrack() || isTrackSharing()) {
            return Views.Full.class;
        } else {
            return defaultView;
        }
    }

    public interface Types {
        String TRACK = "track";
        String TRACK_SHARING = "track-sharing";
        String COMMENT = "comment";
        String FAVORITING = "favoriting";
        String PLAYLIST = "playlist";
    }

    public Class<? extends Origin> getOriginClass() {
        if (isTrack()) {
            return Track.class;
        } else if (isTrackSharing()) {
            return TrackSharing.class;
        } else if (isComment()) {
            return Comment.class;
        } else if (isFavoriting()) {
            return Favoriting.class;
        } else if (isPlaylist()) {
         return Playlist.class;
        } throw new IllegalStateException("unknown type:" +type);
    }

    /* package */ boolean isTrack() {
        return Types.TRACK.equals(type);
    }

    /* package */ boolean isTrackSharing() {
        return Types.TRACK_SHARING.equals(type);
    }

    /* package */ boolean isComment() {
        return Types.COMMENT.equals(type);
    }

    /* package */ boolean isFavoriting() {
        return Types.FAVORITING.equals(type);
    }

    /* package */ boolean isPlaylist() {
        return Types.PLAYLIST.equals(type);
    }

    @Override
    public String toString() {
        return "Event{" +
                "type='" + type + '\'' +
                ", track=" + (getTrack() == null ? "" : getTrack().title) +
                ", user="  + (getUser() == null ? "" : getUser().username) +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Event event = (Event) o;

        if (created_at != null ? !created_at.equals(event.created_at) : event.created_at != null) return false;
        if (origin != null ? !origin.equals(event.origin) : event.origin != null) return false;
        if (tags != null ? !tags.equals(event.tags) : event.tags != null) return false;
        if (type != null ? !type.equals(event.type) : event.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (created_at != null ? created_at.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + (origin != null ? origin.hashCode() : 0);
        return result;
    }

    public static Request getRequestFromType(int type){
        switch (type){
            case Consts.EventTypes.INCOMING : {
                return Request.to(Endpoints.MY_ACTIVITIES);
            }
            case Consts.EventTypes.EXCLUSIVE : {
                return Request.to(Endpoints.MY_EXCLUSIVE_TRACKS);
            }
            case Consts.EventTypes.ACTIVITY : {
                return Request.to(Endpoints.MY_NEWS);
            }
        }
        return null;
    }

    public static String getSyncExtraFromType(int type){
        switch (type){
            case Consts.EventTypes.INCOMING : {
                return ApiSyncService.SyncExtras.INCOMING;
            }
            case Consts.EventTypes.EXCLUSIVE : {
                return ApiSyncService.SyncExtras.EXCLUSIVE;
            }
            case Consts.EventTypes.ACTIVITY : {
                return ApiSyncService.SyncExtras.ACTIVITY;
            }
        }
        return null;
    }

    public static Uri getContentUriFromType(int type){
        switch (type){
            case Consts.EventTypes.INCOMING : {
                return ScContentProvider.Content.ME_SOUND_STREAM;
            }
            case Consts.EventTypes.EXCLUSIVE : {
                return ScContentProvider.Content.ME_EXCLUSIVE_STREAM;
            }
            case Consts.EventTypes.ACTIVITY : {
                return ScContentProvider.Content.ME_ACTIVITIES;
            }
        }
        return null;
    }

}
