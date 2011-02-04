
package com.soundcloud.android.objects;

import java.util.List;

import org.apache.http.NameValuePair;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class Comment extends BaseObj implements Parcelable {

    public static final String MODEL = "overcast.comment";

    public static final String key_id = "id";

    public static final String key_uri = "uri";

    public static final String key_body = "body";

    public static final String key_timestamp = "created_at";

    public static final String key_timestamp_formatted = "timestamp_formatted";

    public static final String key_track_id = "track_id";

    public static final String key_created_at = "created_at";

    public static final String key_reply_to = "reply_to";

    public static final String key_user = "user";

    public static final String key_user_id = "user_id";

    public static final String key_username = "username";

    public static final String key_user_permalink = "user_permalink";

    public static final String key_user_avatar_url = "user_avatar_url";

    private Bundle data;

    public enum Parcelables {
        track, user, comment
    }

    public void resolveData() {
        // if ( data.getString(Comment.key_timestamp) == "null" ||
        // data.getString(Comment.key_timestamp) == null ||
        // data.getString(Comment.key_timestamp) == "")
        // data.putString(Comment.key_timestamp, "-1");
        // else
        // data.putString(Comment.key_timestamp,
        // data.getString(Comment.key_timestamp));
    }

    public Comment() {
    }

    public Comment(Parcel in) {
        readFromParcel(in);
    }

    public Comment(Comment comment) {
        // mappppp
    }

    public List<NameValuePair> mapDataToParams() {
        List<NameValuePair> params = new java.util.ArrayList<NameValuePair>();
        // params.add(new BasicNameValuePair("comment[body]", getBody());
        // params.add(new BasicNameValuePair("comment[created_at]",
        // getTimestamp());
        // params.add(new BasicNameValuePair("comment[reply_to]", getReplyTo());
        return params;
    }

    public static final Parcelable.Creator<Comment> CREATOR = new Parcelable.Creator<Comment>() {
        public Comment createFromParcel(Parcel in) {
            return new Comment(in);
        }

        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };

    @Override
    public void readFromParcel(Parcel in) {
        data = in.readBundle();
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int arg1) {
        // TODO Auto-generated method stub
        out.writeBundle(data);
    }

}
