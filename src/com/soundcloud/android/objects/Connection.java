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
        Twitter(R.drawable.service_twitter, "twitter"),
        Facebook(R.drawable.service_facebook_profile, "facebook_profile", "facebook_page"),
        Myspace(R.drawable.service_myspace, "myspace"),
        /*Foursquare(R.drawable.service_foursquare, "foursquare"),*/
        Unknown(R.drawable.service_myspace, "unknown");

        public final int resId;
        public final String name;
        public final String[] names;

        Service(int resId, String... names) {
            this.resId = resId;
            this.names = names;
            this.name  = names[0];
        }
        static Service fromString(String s) {
            for (Service svc : EnumSet.allOf(Service.class)) {
                for (String n : svc.names) if (s.equalsIgnoreCase(n)) return svc;
            }
            return Unknown;
        }
    }
    public Connection() {}
    public Connection(Service s) {
        this._service = s;
        this.display_name = s.toString();
        this.active = false;
    }

    public boolean active() { return active; }
    private boolean active = true;
    private Service _service;

    public int id;
    public Date created_at;
    public String display_name;
    public boolean post_publish;
    public boolean post_favorite;
    public String service;
    public URI uri;

    public Service service() {
        if (_service == null) _service = Service.fromString(service);
        return _service;
    }

    public static List<Connection> addUnused(List<Connection> connections) {
        EnumSet<Service> networks = EnumSet.allOf(Service.class);
        networks.remove(Service.Unknown);

        for (Connection c : connections) networks.remove(c.service());
        for (Service t : networks) connections.add(new Connection(t));
        return connections;
    }
}
