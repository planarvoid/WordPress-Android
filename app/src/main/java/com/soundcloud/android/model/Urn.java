package com.soundcloud.android.model;

import com.google.common.base.Charsets;
import com.soundcloud.android.api.http.HttpProperties;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.image.ImageSize;
import org.jetbrains.annotations.NotNull;

import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Models a SoundCloud URN
 * see http://eng-doc.int.s-cloud.net/guidelines/urns/
 */
public class Urn {

    public static final String SCHEME = "soundcloud";
    public static final String SOUNDS_TYPE = "sounds";
    public static final String TRACKS_TYPE = "tracks";
    public static final String PLAYLISTS_TYPE = "playlists";
    public static final String USERS_TYPE = "users";

    public @NotNull final Uri uri;
    public @NotNull final String type;
    public @NotNull final String id;
    public final long numericId;

    @NotNull
    public static Urn parse(String uri) {
        return new Urn(uri);
    }

    @NotNull
    public static Urn forTrack(long id) {
        return new Urn(SCHEME + ":" + SOUNDS_TYPE + ":" + id);
    }

    @NotNull
    public static Urn forPlaylist(long id) {
        return new Urn(SCHEME + ":" + PLAYLISTS_TYPE + ":" + id);
    }

    @NotNull
    public static Urn forUser(long id) {
        final long normalizedId = Math.max(0, id); // to account for anonymous users
        return new Urn(SCHEME + ":" + USERS_TYPE + ":" + normalizedId);
    }

    private Urn(@NotNull String uri) {
        this(Uri.parse(uri));
    }

    private Urn(@NotNull Uri uri) {
        if (!SCHEME.equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("not a soundcloud uri");
        }
        final String specific = uri.getSchemeSpecificPart();
        final String[] components = specific.split(":", 2);
        if (components != null && components.length == 2) {
            type = fixType(components[0]);
            id = components[1];
            long n = -1;
            try {
                n = Long.parseLong(id);
            } catch (NumberFormatException ignored) {
            }
            numericId = n;
        } else {
            throw new IllegalArgumentException("invalid uri: "+uri);
        }
        this.uri = uri;
    }

    private static String fixType(String type){
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
        return uri.toString();
    }

    public String toEncodedString() {
        try {
            return URLEncoder.encode(uri.toString(), Charsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Urn urn = (Urn) o;
        return uri.equals(urn.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }
}
