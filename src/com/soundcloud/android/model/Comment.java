
package com.soundcloud.android.model;

import com.soundcloud.android.Consts;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.utils.CloudUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonView;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.Comparator;

public class Comment extends ModelBase implements Origin {
    @JsonView(Views.Mini.class) public Date created_at;
    @JsonView(Views.Mini.class) public long user_id;
    @JsonView(Views.Mini.class) public long track_id;

    @JsonView(Views.Mini.class) @JsonDeserialize() public long timestamp; // should be null (non-timed comment)

    @JsonView(Views.Mini.class) public Track track;

    public long reply_to_id;
    public String reply_to_username;

    @JsonView(Views.Mini.class) public String body;
    @JsonView(Views.Mini.class) public String uri;

    @JsonView(Views.Mini.class) public User user;

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

    public Consts.GraphicSize getAvatarBarGraphicSize(Context c) {
        if (CloudUtils.isScreenXL(c)) {
            return Consts.GraphicSize.LARGE;
        } else {
            return c.getResources().getDisplayMetrics().density > 1 ?
                    Consts.GraphicSize.BADGE :
                    Consts.GraphicSize.SMALL;
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

    @Override @JsonIgnore
    public Track getTrack() {
        return track;
    }

    @Override @JsonIgnore
    public User getUser() {
        return user;
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

    public static class CompareCreatedAt implements Comparator<Comment> {
        public static final Comparator<Comment> INSTANCE = new CompareCreatedAt();
        private CompareCreatedAt() {}

        @Override
        public int compare(Comment c1, Comment c2) {
            return c2.created_at.compareTo(c1.created_at);
        }
    }
}
