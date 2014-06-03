package com.soundcloud.android.model;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/**
 * Models a SoundCloud URN
 * see http://eng-doc.int.s-cloud.net/guidelines/urns/
 */
public abstract class Urn implements Parcelable {

    public static final String SCHEME = "soundcloud";
    public static final String SOUNDS_TYPE = "sounds";
    public static final String TRACKS_TYPE = "tracks";
    public static final String PLAYLISTS_TYPE = "playlists";
    public static final String USERS_TYPE = "users";

    private static final Pattern URN_PATTERN = Pattern.compile("^soundcloud:(" + SOUNDS_TYPE +
            "|" + TRACKS_TYPE + "|" + PLAYLISTS_TYPE + "|" + USERS_TYPE + "):-?\\d+");

    public static final Creator<Urn> CREATOR = new Creator<Urn>() {
        @Override
        public Urn createFromParcel(Parcel source) {
            return Urn.parse(urnString(source.readString(), source.readLong()));
        }

        @Override
        public Urn[] newArray(int size) {
            return new Urn[size];
        }
    };

    @NotNull
    @Deprecated
    public final String type;
    @Deprecated
    public final long numericId;

    public static boolean isValidUrn(Uri uri) {
        return isValidUrn(uri.toString());
    }

    public static boolean isValidUrn(String uri) {
        return URN_PATTERN.matcher(uri).matches();
    }

    @NotNull
    public static Urn parse(String uriString) {
        if (!isValidUrn(uriString)) {
            throw new IllegalArgumentException("Not a valid SoundCloud URN: " + uriString);
        }
        Uri uri = Uri.parse(uriString);
        final String specific = uri.getSchemeSpecificPart();
        final String[] components = specific.split(":", 2);
        final long id = ScTextUtils.safeParseLong(components[1]);
        if (id == -1) {
            throw new IllegalArgumentException("Invalid id from uri : " + uri);
        }

        final String type = fixType(components[0]);
        if (SOUNDS_TYPE.equals(type) || TRACKS_TYPE.equals(type)) {
            return forTrack(id);
        } else if (PLAYLISTS_TYPE.equals(type)) {
            return forPlaylist(id);
        } else {
            return forUser(id);
        }
    }

    @NotNull
    public static TrackUrn forTrack(long id) {
        return new TrackUrn(id);
    }

    @NotNull
    public static PlaylistUrn forPlaylist(long id) {
        return new PlaylistUrn(id);
    }

    @NotNull
    public static UserUrn forUser(long id) {
        final long normalizedId = Math.max(0, id); // to account for anonymous users
        return new UserUrn(normalizedId);
    }

    private static String urnString(String type, long id) {
        return SCHEME + ":" + type + ":" + id;
    }

    protected Urn(@NotNull String type, long numericId) {
        this.type = type;
        this.numericId = numericId;
    }

    private static String fixType(String type) {
        return type.replace("//", "");
    }

    public boolean isSound() {
        return TRACKS_TYPE.equalsIgnoreCase(type) || PLAYLISTS_TYPE.equalsIgnoreCase(type) || SOUNDS_TYPE.equalsIgnoreCase(type);
    }

    public Uri contentProviderUri() {
        if (SOUNDS_TYPE.equals(type)) return Content.TRACK.forId(numericId);
        else if (TRACKS_TYPE.equals(type)) return Content.TRACK.forId(numericId);
        else if (USERS_TYPE.equals(type)) return Content.USER.forId(numericId);
        else if (PLAYLISTS_TYPE.equals(type)) return Content.PLAYLIST.forId(numericId);
        else throw new IllegalStateException("Unsupported content type: " + type);
    }

    @Override
    public String toString() {
        return urnString(type, numericId);
    }

    public String toEncodedString() {
        try {
            return URLEncoder.encode(toString(), Charsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Urn urn = (Urn) o;
        return Objects.equal(type, urn.type) && numericId == urn.numericId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, numericId);
    }

    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeLong(numericId);
    }
}
