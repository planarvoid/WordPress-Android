package com.soundcloud.android.objects;


import com.soundcloud.android.R;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.net.URI;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection {
    public enum Service {
        Twitter(R.drawable.service_twitter),
        Facebook(R.drawable.service_facebook_profile),
        Myspace(R.drawable.service_myspace),
        Foursquare(R.drawable.service_myspace),
        Unknown(R.drawable.service_myspace);

        public final int resId;

        Service(int resId) {
            this.resId = resId;
        }
        static Service fromString(String s) {
            if ("facebook_profile".equals(s) || "facebook_page".equals(s)) {
                return Facebook;
            } else {
                try {
                    return Enum.valueOf(Service.class,
                    s.substring(0, 1).toUpperCase() + s.substring(1, s.length()));
                } catch (IllegalArgumentException e) {
                    return Unknown;
                }
            }
        }
    }
    public Connection() {}
    public Connection(Service t) {
        this._type = t;
        this.display_name = t.toString();
        this.active = false;
    }

    public boolean active() { return active; }
    private boolean active = true;
    private Service _type;

    public int id;
    public Date created_at;
    public String display_name;
    public boolean post_publish;
    public boolean post_favorite;
    public String type;
    public String service;
    public URI uri;

    public Service type() {
        if (_type == null) _type = Service.fromString(type);
        return _type;
    }

    public static List<Connection> addUnused(List<Connection> connections) {
        EnumSet<Service> networks = EnumSet.allOf(Service.class);
        for (Connection c : connections) networks.remove(c.type());
        for (Service t : networks) connections.add(new Connection(t));
        return connections;
    }
}
