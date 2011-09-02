package com.soundcloud.android.model;

import com.soundcloud.android.json.Views;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonView;

import java.util.Date;

public class TrackSharing implements Origin {
    @JsonProperty @JsonView(Views.Mini.class) public Track track;
    @JsonProperty @JsonView(Views.Mini.class) public SharingNote sharing_note;

    public static class SharingNote {
        @JsonProperty @JsonView(Views.Mini.class) public String text;
        @JsonProperty @JsonView(Views.Mini.class) public Date created_at;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SharingNote that = (SharingNote) o;

            return !(created_at != null ? !created_at.equals(that.created_at) : that.created_at != null)
                && !(text != null ? !text.equals(that.text) : that.text != null);

        }

        @Override
        public int hashCode() {
            int result = text != null ? text.hashCode() : 0;
            result = 31 * result + (created_at != null ? created_at.hashCode() : 0);
            return result;
        }
    }

    @Override  @JsonIgnore
    public Track getTrack() {
        return track;
    }

    @Override @JsonIgnore
    public User getUser() {
        return track.user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackSharing that = (TrackSharing) o;

        return !(sharing_note != null ? !sharing_note.equals(that.sharing_note) : that.sharing_note != null)
            && !(track != null ? !track.equals(that.track) : that.track != null);

    }

    @Override
    public int hashCode() {
        int result = track != null ? track.hashCode() : 0;
        result = 31 * result + (sharing_note != null ? sharing_note.hashCode() : 0);
        return result;
    }
}
