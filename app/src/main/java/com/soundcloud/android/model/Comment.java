
package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.model.behavior.RelatesToPlayable;
import com.soundcloud.android.model.behavior.RelatesToUser;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.images.ImageOptionsFactory;
import com.soundcloud.android.utils.images.ImageSize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;

import java.util.Comparator;
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


public class Comment extends ScResource implements RelatesToUser, RelatesToPlayable {
    @JsonProperty @JsonView(Views.Mini.class) public Date created_at;
    @JsonProperty @JsonView(Views.Mini.class) public long user_id;
    @JsonProperty @JsonView(Views.Mini.class) public long track_id;
    @JsonProperty @JsonView(Views.Mini.class) public long timestamp; // should be null (non-timed comment)
    @JsonProperty @JsonView(Views.Mini.class) public Track track;
    @JsonProperty @JsonView(Views.Mini.class) public String body;
    @JsonProperty @JsonView(Views.Mini.class) public String uri;
    @JsonProperty @JsonView(Views.Mini.class) public User user;

    // non-API related fields
    public long reply_to_id;
    public String reply_to_username;
    public int xPos = -1;
    public boolean topLevelComment = false;

    // keep the ignore or jackson will try to write this value on Samsung S4 (or perhaps more devices)
    @JsonIgnore @Nullable public Bitmap avatar;

    public Comment nextComment; //pointer to the next comment at this timestamp

    public Comment() {
    }

    @Override
    public void putDependencyValues(@NotNull BulkInsertMap destination) {
        if (user != null) {
            user.putFullContentValues(destination);
        }
        if (track != null) {
            track.putFullContentValues(destination);
        }
    }

    @Override
    public Uri toUri() {
       return Content.COMMENTS.forId(mID);
    }


    @Override
    public Uri getBulkInsertUri() {
        return Content.COMMENTS.uri;
    }

    public Comment(Cursor c, boolean view) {
        if (view) {
            mID = c.getLong(c.getColumnIndex(DBHelper.ActivityView.COMMENT_ID));
            track_id = c.getLong(c.getColumnIndex(DBHelper.ActivityView.SOUND_ID));
            user_id = c.getLong(c.getColumnIndex(DBHelper.ActivityView.USER_ID));
            user = User.fromActivityView(c);
            body = c.getString(c.getColumnIndex(DBHelper.ActivityView.COMMENT_BODY));
            timestamp = c.getLong(c.getColumnIndex(DBHelper.ActivityView.COMMENT_TIMESTAMP));
            created_at = new Date(c.getLong(c.getColumnIndex(DBHelper.ActivityView.COMMENT_CREATED_AT)));
        } else {
            mID = c.getLong(c.getColumnIndex(DBHelper.Comments._ID));
            track_id = c.getLong(c.getColumnIndex(DBHelper.Comments.TRACK_ID));
            user_id = c.getLong(c.getColumnIndex(DBHelper.Comments.USER_ID));
            body = c.getString(c.getColumnIndex(DBHelper.Comments.BODY));
            timestamp = c.getLong(c.getColumnIndex(DBHelper.Comments.TIMESTAMP));
            created_at = new Date(c.getLong(c.getColumnIndex(DBHelper.Comments.CREATED_AT)));
        }
    }

    public void calculateXPos(int parentWidth, long duration){
        if (duration != 0) {
            xPos = (int) ((timestamp * parentWidth)/duration);
        }
    }

    public void prefetchAvatar(Context c) {
        if (shouldLoadIcon()) {
            ImageLoader.getInstance().loadImage(ImageSize.formatUriForList(c, user.avatar_url), ImageOptionsFactory.prefetch(), null);
        }
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(DBHelper.Comments.CREATED_AT, created_at.getTime());
        cv.put(DBHelper.Comments.BODY, body);
        cv.put(DBHelper.Comments.USER_ID, user_id);
        cv.put(DBHelper.Comments.TRACK_ID, track_id);
        cv.put(DBHelper.Comments.TIMESTAMP, timestamp);
        return cv;
    }

    public boolean shouldLoadIcon() {
        return user != null && user.shouldLoadIcon();
    }

    @Override @JsonIgnore
    public User getUser() {
        return user;
    }

    @Override @JsonIgnore
    public Track getPlayable() {
        return track;
    }

    public static class CompareTimestamp implements Comparator<Comment> {
        public static final Comparator<Comment> INSTANCE = new CompareTimestamp();
        private CompareTimestamp() {}

        @Override
        public int compare(Comment c1, Comment c2) {
            if (c1.timestamp > c2.timestamp)
                return -1;
            else if (c1.timestamp < c2.timestamp)
                return 1;
            else
                return c2.created_at.compareTo(c1.created_at);
        }
    }

    public static Comment build(Track track,
                                User user,
                                long timestamp,
                                String body,
                                long replyToId,
                                String replyToUsername){
        Comment comment = new Comment();
        comment.track_id = track.mID;
        comment.track = track;
        comment.user = user;
        comment.user_id = user.mID;
        comment.timestamp = timestamp;
        comment.created_at = new Date(); // not actually used?
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
        out.writeLong(created_at.getTime());
        out.writeLong(user_id);
        out.writeLong(track_id);
        out.writeLong(timestamp);
        out.writeParcelable(track, 0);
        out.writeString(body);
        out.writeString(uri);
        out.writeParcelable(user, 0);
    }

    public static final Creator<Comment> CREATOR = new Creator<Comment>() {
        public Comment createFromParcel(Parcel in) {
            Comment t = new Comment();
            t.created_at = new Date(in.readLong());
            t.user_id = in.readLong();
            t.track_id = in.readLong();
            t.timestamp = in.readLong();
            t.track = in.readParcelable(Track.class.getClassLoader());
            t.body = in.readString();
            t.uri = in.readString();
            t.user = in.readParcelable(User.class.getClassLoader());
            return t;
        }

        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };
}
