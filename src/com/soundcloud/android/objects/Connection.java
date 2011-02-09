package com.soundcloud.android.objects;


import android.text.TextUtils;
import com.soundcloud.android.R;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.net.URI;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection {
    public static final String REQUEST = "/me/connections.json";

    public enum Type {
        Twitter(R.drawable.service_twitter),
        Facebook(R.drawable.service_facebook_profile),
        Myspace(R.drawable.service_myspace),
        Foursquare(R.drawable.service_myspace);

        public final int resId;

        Type(int resId) {
            this.resId = resId;
        }
        static Type fromString(String s) {
            return Enum.valueOf(Type.class,
                   s.substring(0, 1).toUpperCase() + s.substring(1, s.length()));
        }
    }
    public Connection() {}
    public Connection(Type t) {
        this._type = t;
        this.display_name = t.toString();
        this.active = false;
    }

    public boolean active() { return active; }
    private boolean active = true;
    private Type _type;

    public int id;
    public Date created_at;
    public String display_name;
    public boolean post_publish;
    public boolean post_favorite;
    public String type;
    public String service;
    public URI uri;

    public Type type() {
        if (_type == null) _type = Type.fromString(type);
        return _type;
    }

    public static List<Connection> addUnused(List<Connection> connections) {
        EnumSet<Type> networks = EnumSet.allOf(Type.class);
        for (Connection c : connections) networks.remove(c.type());
        for (Type t : networks) connections.add(new Connection(t));
        return connections;
    }
}
