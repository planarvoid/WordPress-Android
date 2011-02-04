
package com.soundcloud.android.objects;

import org.apache.james.mime4j.field.datetime.DateTime;
import org.codehaus.jackson.annotate.JsonProperty;

import android.os.Parcel;
import android.os.Parcelable;

public class Comment extends BaseObj implements Parcelable {
    
    private Long id;

    private DateTime created_at;
    
    private Long user_id;
    
    private Long track_id;
    
    private DateTime timestamp;    
    
    private String body;
    
    private String uri;
    
    private User user;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }
    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id;
    }
    @JsonProperty("created_at")
    public DateTime getCreatedAt() {
        return created_at;
    }
    @JsonProperty("created_at")
    public void setCreatedAt(DateTime created_at) {
        this.created_at = created_at;
    }
    @JsonProperty("user_id")
    public Long getUserId() {
        return user_id;
    }
    @JsonProperty("user_id")
    public void setUserId(Long user_id) {
        this.user_id = user_id;
    }
    @JsonProperty("track_id")
    public Long getTrackId() {
        return track_id;
    }
    @JsonProperty("track_id")
    public void setTrackId(Long track_id) {
        this.track_id = track_id;
    }
    @JsonProperty("timestamp")
    public DateTime getTimestamp() {
        return timestamp;
    }
    @JsonProperty("timestamp")
    public void setTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
    }
    @JsonProperty("body")
    public String getBody() {
        return body;
    }
    @JsonProperty("body")
    public void setBody(String body) {
        this.body = body;
    }
    @JsonProperty("uri")
    public String getUri() {
        return uri;
    }
    @JsonProperty("uri")
    public void setUri(String uri) {
        this.uri = uri;
    }
    @JsonProperty("user")
    public User getUser() {
        return user;
    }
    @JsonProperty("user")
    public void setUser(User user) {
        this.user = user;
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

}
