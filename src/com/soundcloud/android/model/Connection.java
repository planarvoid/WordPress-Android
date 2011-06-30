package com.soundcloud.android.model;


import com.soundcloud.android.R;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection implements Comparable<Connection>, Parcelable {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeLong(created_at.getTime());
        dest.writeString(display_name);
        dest.writeInt(post_publish ? 1 : 0);
        dest.writeInt(post_favorite ? 1 : 0);
        dest.writeString(service);
        dest.writeString(uri.toString());
    }

    public static Creator<Connection> CREATOR =  new Creator<Connection>() {
        @Override
        public Connection createFromParcel(Parcel source) {
            Connection connection = new Connection();
            connection.id = source.readInt();
            connection.created_at = new Date(source.readLong());
            connection.display_name = source.readString();
            connection.post_publish = source.readInt() == 1;
            connection.post_favorite = source.readInt() == 1;
            connection.service = source.readString();
            connection.uri = URI.create(source.readString());
            return connection;
        }

        @Override
        public Connection[] newArray(int size) {
            return new Connection[size];
        }
    };

    public enum Service {
        Facebook(R.drawable.service_facebook_profile, true, false, "facebook_profile", "facebook_page"),
        Twitter(R.drawable.service_twitter, true, false, "twitter"),
        Foursquare(R.drawable.service_foursquare, true, false, "foursquare"),
        Tumblr(R.drawable.service_tumblr, true, false, "tumblr"),
        Myspace(R.drawable.service_myspace, true, true, "myspace"),
        Unknown(R.drawable.service_myspace, false, false, "unknown");

        public final int resId;
        public final String name;
        public final String[] names;
        public final boolean enabled, deprecated;


        /**
         * @param resId        icon resource
         * @param enabled      service is enabled
         * @param deprecated   service is deprecated (no new connections possible)
         * @param names        names used for this service
         */
        Service(int resId, boolean enabled, boolean deprecated, String... names) {
            this.resId = resId;
            this.names = names;
            this.name  = names[0];
            this.enabled = enabled;
            this.deprecated = deprecated;
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
          Service svc = it.next();
          if (!svc.enabled || svc.deprecated) it.remove();
        }

        for (Connection c : connections) networks.remove(c.service());
        for (Service t : networks) connections.add(new Connection(t));
        Collections.sort(connections);
        return connections;
    }

    public static boolean checkConnectionListForService(List<Connection> haystack, Connection.Service needle) {
        if (haystack != null) {
            for (Connection c : haystack){
                if (c.service() == needle){
                    return true;
                }
            }
        }
        return false;
    }
}
