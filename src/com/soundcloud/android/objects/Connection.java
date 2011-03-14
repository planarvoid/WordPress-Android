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
public class Connection implements Comparable {
    public enum Service {
        Twitter(R.drawable.service_twitter, true, 1, "twitter"),
        Facebook(R.drawable.service_facebook_profile, true, 0, "facebook_profile", "facebook_page"),
        Myspace(R.drawable.service_myspace, true, 3, "myspace"),
        Foursquare(R.drawable.service_foursquare, true, 2, "foursquare"),
        Unknown(R.drawable.service_myspace, false, 4, "unknown");

        public final int resId;
        public final String name;
        public final String[] names;
        public final boolean enabled;
        public final int rank;

        Service(int resId, boolean enabled, int rank, String... names) {
            this.resId = resId;
            this.rank = rank;
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

    public int getRank(){
        if (_service == null) _service = Service.fromString(service);
        return _service.rank;
    }



    public int compareTo(Object another) {

        int rank1 = getRank();
        int rank2 = ((Connection) another).getRank();

        if (rank1 > rank2){
            return +1;
        }else if (rank1 < rank2){
            return -1;
        }else{
            return 0;
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
