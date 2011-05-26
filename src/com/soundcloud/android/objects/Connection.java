package com.soundcloud.android.objects;


import com.soundcloud.android.R;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection implements Comparable<Connection> {
    private boolean active = true;
    private Service _service;

    public int id;
    public Date created_at;
    public String display_name;
    public boolean post_publish;
    public boolean post_favorite;
    public String service;
    public URI uri;

    public Connection() {}
    public Connection(Service s) {
        this._service = s;
        this.display_name = s.toString();
        this.active = false;
    }

    public boolean active() { return active; }
    public Service service() {
        if (_service == null) _service = Service.fromString(service);
        return _service;
    }

    public int compareTo(Connection another) {
        int rank1 = service().ordinal();
        int rank2 = another.service().ordinal();
        return (rank1 > rank2) ? 1 : ((rank1 < rank2) ? -1 : 0);
    }

    public enum Service {
        Facebook(R.drawable.service_facebook_profile, true, "facebook_profile", "facebook_page"),
        Twitter(R.drawable.service_twitter, true, "twitter"),
        Foursquare(R.drawable.service_foursquare, true, "foursquare"),
        Tumblr(R.drawable.service_tumblr, true, "tumblr"),
        Myspace(R.drawable.service_myspace, false, "myspace"),
        Unknown(R.drawable.service_myspace, false, "unknown");

        public final int resId;
        public final String name;
        public final String[] names;
        public final boolean enabled;

        Service(int resId, boolean enabled, String... names) {
            this.resId = resId;
            this.names = names;
            this.name  = names[0];
            this.enabled = enabled;
        }
        static Service fromString(String s) {
            for (Service svc : EnumSet.allOf(Service.class)) {
                for (String n : svc.names) if (s.equalsIgnoreCase(n)) return svc;
            }
            return Unknown;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Connection> addUnused(List<Connection> connections) {
        EnumSet<Service> networks = EnumSet.allOf(Service.class);
        for (Iterator<Service> it = networks.iterator(); it.hasNext();) {
          if (!it.next().enabled) it.remove();
        }

        for (Connection c : connections) networks.remove(c.service());
        for (Service t : networks) connections.add(new Connection(t));
        Collections.sort(connections);
        return connections;
    }
}
