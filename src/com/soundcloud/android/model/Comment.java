
package com.soundcloud.android.model;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.CloudUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonView;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Parcel;

import java.util.Comparator;
import java.util.Date;

public class Comment extends ScModel implements Origin {
    @JsonView(Views.Mini.class) public Date created_at;
    @JsonView(Views.Mini.class) public long user_id;
    @JsonView(Views.Mini.class) public long track_id;
    @JsonView(Views.Mini.class) public long timestamp; // should be null (non-timed comment)
    @JsonView(Views.Mini.class) public Track track;
    @JsonView(Views.Mini.class) public String body;
    @JsonView(Views.Mini.class) public String uri;
    @JsonView(Views.Mini.class) public User user;

    // non-API related fields
    public long reply_to_id;
    public String reply_to_username;
    public int xPos = -1;
    public boolean topLevelComment = false;
    public Bitmap avatar;

    public Comment nextComment; //pointer to the next comment at this timestamp

    public Comment(Cursor c, boolean view) {
        track = new Track(c);
        if (view) {
            id = c.getLong(c.getColumnIndex(DBHelper.ActivityView.COMMENT_ID));
            user_id = c.getLong(c.getColumnIndex(DBHelper.ActivityView.USER_ID));
            user = User.fromActivityView(c);
            body = c.getString(c.getColumnIndex(DBHelper.ActivityView.COMMENT_BODY));
            timestamp = c.getLong(c.getColumnIndex(DBHelper.ActivityView.COMMENT_TIMESTAMP));
            created_at = new Date(c.getLong(c.getColumnIndex(DBHelper.ActivityView.COMMENT_CREATED_AT)));
        } else {
            id = c.getLong(c.getColumnIndex(DBHelper.Comments._ID));
            user_id = c.getLong(c.getColumnIndex(DBHelper.Comments.USER_ID));
            body = c.getString(c.getColumnIndex(DBHelper.Comments.BODY));
            timestamp = c.getLong(c.getColumnIndex(DBHelper.Comments.TIMESTAMP));
            created_at = new Date(c.getLong(c.getColumnIndex(DBHelper.Comments.CREATED_AT)));
        }
    }

    public void calculateXPos(int parentWidth, long duration){
        this.xPos = (int) ((this.timestamp * parentWidth)/duration);
    }

    public Comment() {
    }

    @Override @JsonIgnore
    public Track getTrack() {
        return track;
    }

    @Override @JsonIgnore
    public User getUser() {
        return user;
    }

    public void prefetchAvatar(Context c) {
        if (user != null && CloudUtils.checkIconShouldLoad(user.avatar_url)) {
            ImageLoader.get(c).prefetch(Consts.GraphicSize.formatUriForList(c, user.avatar_url));
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

    public static Comment build(Context context,
                                long userId,
                                long trackId,
                                long timestamp,
                                String commentBody,
                                long replyToId,
                                String replyToUsername){
        Comment comment = new Comment();
        comment.track_id = trackId;
        comment.created_at = new Date(System.currentTimeMillis());
        comment.user_id = userId;
        comment.user = SoundCloudDB.getUserById(context.getContentResolver(), comment.user_id);
        comment.timestamp = timestamp;
        comment.body = commentBody;
        comment.reply_to_id = replyToId;
        comment.reply_to_username = replyToUsername;
        return comment;
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
