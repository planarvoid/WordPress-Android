
package com.soundcloud.android.objects;

import org.apache.james.mime4j.field.datetime.DateTime;

import android.os.Parcel;
import android.os.Parcelable;

public class Comment extends BaseObj implements Parcelable {
    
    public Long id;

    public DateTime created_at;
    
    public Long user_id;
    
    public Long track_id;
    
    public DateTime timestamp;    
    
    public String body;
    
    public String uri;
    
    public User user;

    public Comment() {
    }

    public Comment(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator<Comment> CREATOR = new Parcelable.Creator<Comment>() {
        public Comment createFromParcel(Parcel in) {
            return new Comment(in);
        }

        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };

}
