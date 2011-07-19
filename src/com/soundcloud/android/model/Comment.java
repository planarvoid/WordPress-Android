
package com.soundcloud.android.model;

import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.sql.Date;
import java.util.Comparator;

public class Comment extends BaseObj implements Parcelable {

    public long id;
    public Date created_at;

    public long user_id;
    public long track_id;
    public long timestamp;

    public Track track;

    public long reply_to_id;
    public String reply_to_username;

    public String body;
    public String uri;

    public User user;

    public int xPos = -1;
    public boolean topLevelComment = false;
    public Bitmap avatar;

    public Comment nextComment; //pointer to the next comment at this timestamp

    public void calculateXPos(int parentWidth, long duration){
        this.xPos = (int) ((this.timestamp * parentWidth)/duration);
    }

    public Comment() {
    }

    public Comment(Parcel in) {
        readFromParcel(in);
    }

    public String getAvatarBarGraphicUrl(Context c){
        if (CloudUtils.isScreenXL(c)) {
              return CloudUtils.formatGraphicsUrl(user.avatar_url, Consts.GraphicsSizes.LARGE);
        } else {
            return c.getResources().getDisplayMetrics().density > 1 ?
                        CloudUtils.formatGraphicsUrl(user.avatar_url, Consts.GraphicsSizes.BADGE) :
                            CloudUtils.formatGraphicsUrl(user.avatar_url, Consts.GraphicsSizes.SMALL);
        }

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
    public void writeToParcel(Parcel out, int flags) {
        buildParcel(out,flags);
    }

    public static class CompareTimestamp implements Comparator<Comment>{
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

    public static class CompareCreatedAt implements Comparator<Comment>{
        @Override
        public int compare(Comment c1, Comment c2) {
            return c2.created_at.compareTo(c1.created_at);
        }
    }
}
