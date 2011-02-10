
package com.soundcloud.android.objects;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.ref.WeakReference;
import java.sql.Date;

public class Comment extends BaseObj implements Parcelable, Comparable<Comment> {
    
    public long id;
    public Date created_at;
    
    public long user_id;
    public long track_id;
    public long timestamp;
    
    public String body;
    public String uri;
    
    public User user;

    public int xPos = -1;
    public boolean topLevelComment = false;
    public Bitmap avatar;
    
    public void calculateXPos(int parentWidth, long duration){
        this.xPos = (int) ((this.timestamp * parentWidth)/duration);
    }
    
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
    
    public void writeToParcel(Parcel out, int flags) {
        buildParcel(out,flags);
    }

    @Override
    public int compareTo(Comment comment) {
        if (comment.timestamp < timestamp)
            return -1;
        else if (comment.timestamp > timestamp)
            return 1;
        else
            return 0;
    }
}
