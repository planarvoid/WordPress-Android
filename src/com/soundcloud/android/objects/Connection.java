package com.soundcloud.android.objects;


import org.codehaus.jackson.annotate.JsonProperty;

import java.net.URI;

public class Connection {
    enum Type { TWITTER, FACEBOOK, MYSPACE }

    public int id;

    public String type;

    public String displayName;
    public boolean postPublish;
    public boolean postFavorite;
    public URI uri;


    @Override
    public String toString() {
        return "Connection{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", displayName='" + displayName + '\'' +
                ", postPublish=" + postPublish +
                ", postFavorite=" + postFavorite +
                ", uri=" + uri +
                '}';
    }
}
