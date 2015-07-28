package com.soundcloud.android.model;

import com.soundcloud.android.Consts;
import com.soundcloud.java.strings.Charsets;
import org.jetbrains.annotations.NotNull;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Models a URN. For specs around SoundCloud URNs, see http://eng-doc.int.s-cloud.net/guidelines/urns/
 */
public final class Urn implements Parcelable, Comparable<Urn> {

    private static final String COLON = ":";
    public static final String SOUNDCLOUD_SCHEME = "soundcloud";
    public static final Urn NOT_SET = new Urn(SOUNDCLOUD_SCHEME + COLON + "unknown", Consts.NOT_SET);

    private static final String SOUNDS_TYPE = "sounds";
    private static final String TRACKS_TYPE = "tracks";
    private static final String PLAYLISTS_TYPE = "playlists";
    private static final String USERS_TYPE = "users";

    private static final String NUMERIC_ID_PATTERN = ":(-?\\d+)";
    private static final String TRACK_PATTERN = SOUNDCLOUD_SCHEME + COLON + TRACKS_TYPE + NUMERIC_ID_PATTERN;
    private static final String LEGACY_TRACK_PATTERN = SOUNDCLOUD_SCHEME + COLON + SOUNDS_TYPE + NUMERIC_ID_PATTERN;
    private static final String PLAYLIST_PATTERN = SOUNDCLOUD_SCHEME + COLON + PLAYLISTS_TYPE + NUMERIC_ID_PATTERN;
    private static final String USER_PATTERN = SOUNDCLOUD_SCHEME + COLON + USERS_TYPE + NUMERIC_ID_PATTERN;

    public static final Creator<Urn> CREATOR = new Creator<Urn>() {
        @Override
        public Urn createFromParcel(Parcel source) {
            return new Urn(source.readString());
        }

        @Override
        public Urn[] newArray(int size) {
            return new Urn[size];
        }
    };

    @NotNull private final String content;
    private final long numericId;

    public static boolean isSoundCloudUrn(String urnString) {
        return urnString.matches(TRACK_PATTERN)
                || urnString.matches(LEGACY_TRACK_PATTERN)
                || urnString.matches(PLAYLIST_PATTERN)
                || urnString.matches(USER_PATTERN);
    }

    @NotNull
    public static Urn forTrack(long id) {
        return new Urn(SOUNDCLOUD_SCHEME + COLON + TRACKS_TYPE, id);
    }

    @NotNull
    public static Urn forPlaylist(long id) {
        return new Urn(SOUNDCLOUD_SCHEME + COLON + PLAYLISTS_TYPE, id);
    }

    @NotNull
    public static Urn forUser(long id) {
        final long normalizedId = Math.max(0, id); // to account for anonymous users
        return new Urn(SOUNDCLOUD_SCHEME + COLON + USERS_TYPE, normalizedId);
    }

    public Urn(String content) {
        this.content = content.replaceFirst("soundcloud:sounds:", "soundcloud:tracks:");
        // since we access this part so frequently, we're pre-caching it for faster access later on
        this.numericId = parseNumericId();
    }

    private Urn(String prefix, long numericId) {
        this.content = prefix + COLON + numericId;
        this.numericId = numericId;
    }

    public boolean isSound() {
        return isTrack() || isPlaylist();
    }

    public boolean isTrack() {
        return content.matches(TRACK_PATTERN) || content.matches(LEGACY_TRACK_PATTERN);
    }

    public boolean isPlaylist() {
        return content.matches(PLAYLIST_PATTERN);
    }

    public boolean isUser() {
        return content.matches(USER_PATTERN);
    }

    public long getNumericId() {
        return numericId;
    }

    private long parseNumericId() {
        final Matcher matcher = Pattern.compile(NUMERIC_ID_PATTERN).matcher(content);
        if (isSoundCloudUrn(content) && matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return Consts.NOT_SET;
    }

    @Override
    public int compareTo(@NotNull Urn another) {
        return this.content.compareTo(another.content);
    }

    @Override
    public String toString() {
        return content;
    }

    public String toEncodedString() {
        try {
            return URLEncoder.encode(toString(), Charsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
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
        return urn.content.equals(this.content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(content);
    }
}
