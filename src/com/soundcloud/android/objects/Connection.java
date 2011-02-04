package com.soundcloud.android.objects;


import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.net.URI;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection {
    enum Type { TWITTER, FACEBOOK, MYSPACE }

    public int id;

    public String type;

    public String displayName;
    public boolean postPublish;
    public boolean postFavorite;
    public URI uri;

    public Date created_at;

    @Override
    public String toString() {
        return "Connection{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", displayName='" + displayName + '\'' +
                ", postPublish=" + postPublish +
                ", postFavorite=" + postFavorite +
                ", uri=" + uri +
                ", created_at=" + created_at +
                '}';
    }
}
