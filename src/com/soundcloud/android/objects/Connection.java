package com.soundcloud.android.objects;


import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.net.URI;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection {
    enum Type { TWITTER, FACEBOOK, MYSPACE }

    public int id;
    public Date created_at;
    public String display_name;
    public boolean post_publish;
    public boolean post_favorite;
    public String type;
    public String service;
    public String uri;

    @Override
    public String toString() {
        return "Connection{" +
                "id=" + id +
                ", created_at=" + created_at +
                ", display_name='" + display_name + '\'' +
                ", post_publish=" + post_publish +
                ", post_favorite=" + post_favorite +
                ", type='" + type + '\'' +
                ", service='" + service + '\'' +
                ", uri=" + uri +
                '}';
    }
}
