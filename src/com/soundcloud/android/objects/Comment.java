
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

    public WeakReference<Bitmap> avatar;
    public int xPos = -1;
    
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

    @Override
    public int compareTo(Comment comment) {
        return created_at.compareTo(comment.created_at);
    }
}
