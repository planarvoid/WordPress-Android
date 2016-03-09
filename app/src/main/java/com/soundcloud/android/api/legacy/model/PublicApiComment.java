
package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.api.legacy.json.Views;
import com.soundcloud.android.api.legacy.model.behavior.RelatesToPlayable;
import com.soundcloud.android.api.legacy.model.behavior.RelatesToUser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.Nullable;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/*
"origin": {
        "track_id": 19446062,
        "track": {
          "stream_url": "https://api.soundcloud.com/tracks/19446062/stream",
          "permalink": "lullaby",
          "user_uri": "https://api.soundcloud.com/users/133201",
          "id": 19446062,
          "permalink_url": "http://soundcloud.com/jberkel/lullaby",
          "user_id": 133201,
          "uri": "https://api.soundcloud.com/tracks/19446062",
          "title": "lullaby",
          "kind": "track"
        },
        "created_at": "2011/07/23 11:52:28 +0000",
        "id": 21623911,
        "user_id": 4253183,
        "uri": "https://api.soundcloud.com/comments/21623911",
        "body": "its a very quiet lullaby....eddie...",
        "user": {
          "permalink": "eddiesongwriter",
          "id": 4253183,
          "permalink_url": "http://soundcloud.com/eddiesongwriter",
          "uri": "https://api.soundcloud.com/users/4253183",
          "kind": "user",
          "avatar_url": "http://i1.sndcdn.com/avatars-000003378812-m83f4d-large.jpg?0dfc9e6",
          "username": "EddieSongWriter"
        },
 */


@Deprecated
public class PublicApiComment extends PublicApiResource implements RelatesToUser, RelatesToPlayable {

    public static final Parcelable.Creator<PublicApiComment> CREATOR = new Parcelable.Creator<PublicApiComment>() {
        public PublicApiComment createFromParcel(Parcel in) {
            PublicApiComment t = new PublicApiComment();
            t.createdAt = new Date(in.readLong());
            t.user_id = in.readLong();
            t.track_id = in.readLong();
            t.timestamp = in.readLong();
            t.track = in.readParcelable(PublicApiTrack.class.getClassLoader());
            t.body = in.readString();
            t.uri = in.readString();
            t.user = in.readParcelable(PublicApiUser.class.getClassLoader());
            return t;
        }

        public PublicApiComment[] newArray(int size) {
            return new PublicApiComment[size];
        }
    };

    public static final String EXTRA = "comment";
    private static final String UNUSED = "unused";

    @JsonProperty @JsonView(Views.Mini.class) public long user_id;
    @JsonProperty @JsonView(Views.Mini.class) public long track_id;
    @JsonProperty @JsonView(Views.Mini.class) public long timestamp; // should be null (non-timed comment)
    @JsonProperty @JsonView(Views.Mini.class) public PublicApiTrack track;
    @JsonProperty @JsonView(Views.Mini.class) public String body;
    @JsonProperty @JsonView(Views.Mini.class) public String uri;
    @JsonProperty @JsonView(Views.Mini.class) public PublicApiUser user;

    // non-API related fields
    public long reply_to_id;
    public String reply_to_username;
    public int xPos = -1;

    // keep the ignore or jackson will try to write this value on Samsung S4 (or perhaps more devices)
    @JsonIgnore @Nullable public Bitmap avatar;

    private Date createdAt;

    public PublicApiComment() {
    }

    @Override @JsonIgnore
    public PublicApiUser getUser() {
        return user;
    }

    public void setUser(PublicApiUser user) {
        this.user = user;
    }

    @Override @JsonIgnore
    public PublicApiTrack getPlayable() {
        return track;
    }

    @JsonIgnore
    @SuppressWarnings(UNUSED) // ModelCitizen needs this
    public PublicApiTrack getTrack() {
        return this.track;
    }

    @SuppressWarnings(UNUSED) // ModelCitizen needs this
    public void setTrack(PublicApiTrack track) {
        this.track = track;
    }

    @JsonIgnore
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Date getCreatedAt() {
        return createdAt;
    }

    @JsonProperty("created_at")
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setCreatedAt(Date date) {
        this.createdAt = date;
    }

    @JsonIgnore
    @SuppressWarnings(UNUSED) // ModelCitizen needs this
    public String getBody() {
        return this.body;
    }

    @SuppressWarnings(UNUSED) // ModelCitizen needs this
    public void setBody(String body) {
        this.body = body;
    }

    public static PublicApiComment build(PublicApiTrack track,
                                PublicApiUser user,
                                long timestamp,
                                String body,
                                long replyToId,
                                String replyToUsername){
        PublicApiComment comment = new PublicApiComment();
        comment.track_id = track.getId();
        comment.track = track;
        comment.user = user;
        comment.user_id = user.getId();
        comment.timestamp = timestamp;
        comment.createdAt = new Date(); // not actually used?
        comment.body = body;
        comment.reply_to_id = replyToId;
        comment.reply_to_username = replyToUsername;
        return comment;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(createdAt.getTime());
        out.writeLong(user_id);
        out.writeLong(track_id);
        out.writeLong(timestamp);
        out.writeParcelable(track, 0);
        out.writeString(body);
        out.writeString(uri);
        out.writeParcelable(user, 0);
    }

}
