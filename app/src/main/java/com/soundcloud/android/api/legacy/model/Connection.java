package com.soundcloud.android.api.legacy.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection extends PublicApiResource implements Comparable<Connection>, Parcelable {
    private boolean active = true;
    private Service _service;

    public String type;
    public Date created_at;
    public String display_name;
    public boolean post_publish;
    @JsonProperty("post_favorite") public boolean post_like;
    public String service;
    public Uri uri;

    public Connection() {}

    public Connection(Service s) {
        this._service = s;
        this.display_name = s.toString();
        this.active = false;
    }

    public Connection(Cursor c){
        setId(c.getLong(c.getColumnIndex(TableColumns.Connections._ID)));
        service = c.getString(c.getColumnIndex(TableColumns.Connections.SERVICE));
        _service = Service.fromString(service);
        type = c.getString(c.getColumnIndex(TableColumns.Connections.TYPE));
        created_at = new Date(c.getLong(c.getColumnIndex(TableColumns.Connections.CREATED_AT)));
        display_name = c.getString(c.getColumnIndex(TableColumns.Connections.DISPLAY_NAME));
        active = c.getInt(c.getColumnIndex(TableColumns.Connections.ACTIVE)) == 1;
        post_publish = c.getInt(c.getColumnIndex(TableColumns.Connections.POST_PUBLISH)) == 1;
        post_like = c.getInt(c.getColumnIndex(TableColumns.Connections.POST_LIKE)) == 1;
        uri = Uri.parse(c.getString(c.getColumnIndex(TableColumns.Connections.URI)));
    }

    @JsonProperty("uri")
    public void setUri(String uriString){
        uri = Uri.parse(uriString);
    }

    public boolean isActive() { return active; }

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
    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Connections._ID, getId());
        cv.put(TableColumns.Connections.USER_ID, SoundCloudApplication.instance.getAccountOperations().getLoggedInUserId()); // not sure if we should infer the user id here
        cv.put(TableColumns.Connections.SERVICE, service);
        cv.put(TableColumns.Connections.TYPE, type);
        cv.put(TableColumns.Connections.CREATED_AT, created_at.getTime());
        cv.put(TableColumns.Connections.DISPLAY_NAME, display_name);
        cv.put(TableColumns.Connections.ACTIVE, active);
        cv.put(TableColumns.Connections.POST_PUBLISH, post_publish);
        cv.put(TableColumns.Connections.POST_LIKE, post_like);
        cv.put(TableColumns.Connections.URI, uri.toString());
        return cv;
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.ME_CONNECTIONS.uri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(getId());
        dest.writeLong(created_at == null ? 0 : created_at.getTime());
        dest.writeString(display_name);
        dest.writeInt(post_publish ? 1 : 0);
        dest.writeInt(post_like ? 1 : 0);
        dest.writeString(service);
        dest.writeString(type);
        dest.writeString(uri == null ? null : uri.toString());
    }

    public static Creator<Connection> CREATOR =  new Creator<Connection>() {
        @Override
        public Connection createFromParcel(Parcel source) {
            Connection connection = new Connection();
            connection.setId(source.readLong());
            connection.created_at = new Date(source.readLong());
            connection.display_name = source.readString();
            connection.post_publish = source.readInt() == 1;
            connection.post_like = source.readInt() == 1;
            connection.service = source.readString();
            connection.type = source.readString();
            String uri = source.readString();
            connection.uri = uri == null ? null : Uri.parse(uri);
            return connection;
        }

        @Override
        public Connection[] newArray(int size) {
            return new Connection[size];
        }
    };

    @Override
    public String toString() {
        return "Connection{" +
                "active=" + active +
                ", id=" + getId() +
                ", created_at=" + created_at +
                ", display_name='" + display_name + '\'' +
                ", post_publish=" + post_publish +
                ", post_like=" + post_like +
                ", service='" + service + '\'' +
                ", type='" + type + '\'' +
                ", uri=" + uri +
                '}';
    }

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

    public static List<Connection> addUnused(Set<Connection> connections) {
        List<Connection> all = new ArrayList<Connection>(connections);

        EnumSet<Service> networks = EnumSet.allOf(Service.class);
        for (Iterator<Service> it = networks.iterator(); it.hasNext();) {
          Service svc = it.next();
          if (!svc.enabled || svc.deprecated) it.remove();
        }

        for (Connection c : all) networks.remove(c.service());
        for (Service t : networks) all.add(new Connection(t));
        Collections.sort(all);
        return all;
    }

    public static boolean checkConnectionListForService(Set<Connection> haystack, Service needle) {
        if (haystack != null) {
            for (Connection c : haystack){
                if (c.service() == needle){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Connection)) return false;
        if (!super.equals(o)) return false;

        Connection that = (Connection) o;

        if (getId() != that.getId()) return false;
        if (display_name != null ? !display_name.equals(that.display_name) : that.display_name != null) return false;
        if (service != null ? !service.equals(that.service) : that.service != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (getId() ^ (getId() >>> 32));
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (display_name != null ? display_name.hashCode() : 0);
        result = 31 * result + (service != null ? service.hashCode() : 0);
        return result;
    }

    @Override
    public Uri toUri() {
        return Content.ME_CONNECTIONS.forId(getId());
    }

}
