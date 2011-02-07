package com.soundcloud.android.objects;


import com.soundcloud.android.R;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.net.URI;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection {
    public enum Type {
        TWITTER(R.drawable.service_twitter),
        FACEBOOK(R.drawable.service_facebook_profile),
        MYSPACE(R.drawable.service_myspace);

        public int id;
        Type(int resId) { this.id = resId; }
    }

    public int id;
    public Date created_at;
    public String display_name;
    public boolean post_publish;
    public boolean post_favorite;
    public String type;
    public String service;
    public URI uri;

    public Type type() { return Enum.valueOf(Type.class, type.toUpperCase()); }

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
