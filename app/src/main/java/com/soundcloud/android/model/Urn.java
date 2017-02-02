package com.soundcloud.android.model;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.soundcloud.android.Consts;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;

import java.util.Arrays;

public final class Urn extends ContentStringHelper<Urn> {

    public static final Function<String, Urn> STRING_TO_URN = input -> new Urn(input);

    public static final Urn NOT_SET = new Urn(UrnNamespace.SOUNDCLOUD, UrnCollection.UNKNOWN, Consts.NOT_SET);

    private static final String SEPARATOR = ":";

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

    @NonNull private final String content;

    private UrnNamespace namespace;
    private UrnCollection collection;
    private String namespaceValue;
    private String collectionValue;
    private String stringId;
    private long longId;

    public Urn(@Nullable String urnString) {
        this.content = parseContent(urnString);
    }

    public Urn(UrnNamespace namespace, UrnCollection collection, long id) {
        this.content = buildFrom(namespace, collection, id);
    }

    public Urn(UrnNamespace namespace, UrnCollection collection, String stringId) {
        this.content = buildFrom(namespace, collection, stringId);
    }

    @Override
    @NonNull
    String getContent() {
        return content;
    }

    public static Urn forTrack(long id) {
        return new Urn(UrnNamespace.SOUNDCLOUD, UrnCollection.TRACKS, id);
    }

    public static Urn forPlaylist(long id) {
        UrnNamespace namespace = id >= 0
                                 ? UrnNamespace.SOUNDCLOUD
                                 : UrnNamespace.LOCAL;
        return new Urn(namespace, UrnCollection.PLAYLISTS, id);
    }

    public static Urn newLocalPlaylist() {
        return forPlaylist(-System.currentTimeMillis());
    }

    public static Urn forUser(long id) {
        return new Urn(UrnNamespace.SOUNDCLOUD, UrnCollection.USERS, id);
    }

    public static Urn forComment(long id) {
        return new Urn(UrnNamespace.SOUNDCLOUD, UrnCollection.COMMENTS, id);
    }

    public static Urn forDayZero(long id) {
        return new Urn(UrnNamespace.SOUNDCLOUD, UrnCollection.DAY_ZERO, id);
    }

    public static Urn forTrackStation(long trackId) {
        return new Urn(UrnNamespace.SOUNDCLOUD, UrnCollection.TRACK_STATIONS, trackId);
    }

    public static Urn forArtistStation(long userId) {
        return new Urn(UrnNamespace.SOUNDCLOUD, UrnCollection.ARTIST_STATIONS, userId);
    }

    public static Urn forAd(String namespace, String id) {
        return new Urn(namespace + SEPARATOR + UrnCollection.ADS.value() + SEPARATOR + id);
    }

    public static Urn forGenre(String genre) {
        return new Urn(UrnNamespace.SOUNDCLOUD, UrnCollection.GENRES, genre);
    }

    public static Urn forNewForYou(String id) {
        return new Urn(UrnNamespace.SOUNDCLOUD, UrnCollection.NEW_FOR_YOU, id);
    }

    public boolean isPlayable() {
        return isTrack() || isPlaylist();
    }

    public boolean isTrack() {
        return isSoundCloud()
                && (collection == UrnCollection.SOUNDS || collection == UrnCollection.TRACKS);
    }

    public boolean isPlaylist() {
        return (isSoundCloud() || isLocal())
                && collection == UrnCollection.PLAYLISTS;
    }

    public boolean isLocal() {
        return namespace == UrnNamespace.LOCAL;
    }

    public boolean isUser() {
        return isSoundCloud()
                && collection == UrnCollection.USERS;
    }

    public boolean isStation() {
        return isSoundCloud()
                && (collection == UrnCollection.STATIONS
                || collection == UrnCollection.TRACK_STATIONS
                || collection == UrnCollection.ARTIST_STATIONS);
    }

    public boolean isTrackStation() {
        return isSoundCloud()
                && collection == UrnCollection.TRACK_STATIONS;
    }

    public boolean isArtistStation() {
        return isSoundCloud()
                && collection == UrnCollection.ARTIST_STATIONS;
    }

    public boolean isAd() {
        return collection == UrnCollection.ADS;
    }

    public long getNumericId() {
        return longId;
    }

    public String getStringId() {
        return stringId;
    }

    private boolean isSoundCloud() {
        return namespace == UrnNamespace.SOUNDCLOUD;
    }

    private void setNamespace(UrnNamespace namespace) {
        this.namespace = namespace;
        this.namespaceValue = namespace.value();
    }

    private void setCollection(UrnCollection collection) {
        this.collection = collection;
        this.collectionValue = collection.value();
    }

    private void setId(long id) {
        this.longId = id;
        setStringId(String.valueOf(id));
    }

    private void setStringId(String stringId) {
        this.stringId = stringId;
    }

    private String buildFrom(UrnNamespace namespace, UrnCollection collection, long id) {
        setNamespace(namespace);
        setCollection(collection);
        setId(id);
        return buildContent();
    }

    private String buildFrom(UrnNamespace namespace, UrnCollection collection, String id) {
        setNamespace(namespace);
        setCollection(collection);
        setStringId(id);
        return buildContent();
    }

    private String buildContent() {
        return Strings.joinOn(SEPARATOR).join(new String[]{namespaceValue, collectionValue, stringId});
    }

    private String parseContent(@Nullable String urnString) {
        if (urnString == null) {
            return buildFrom(UrnNamespace.OTHER, UrnCollection.UNKNOWN, Consts.NOT_SET);
        }

        String[] parts = urnString.split(SEPARATOR);

        parseNamespace(parts);
        parseCollection(parts);
        parseId(parts);

        if (isLegacySoundsCollection()) {
            return fromLegacySoundsCollection();
        } else {
            return urnString;
        }
    }

    private void parseNamespace(String[] parts) {
        if (parts.length > 0) {
            namespace = UrnNamespace.from(parts[0]);
            namespaceValue = parts[0];
        } else {
            setNamespace(UrnNamespace.OTHER);
        }
    }

    private void parseCollection(String[] parts) {
        if (parts.length > 1) {
            collection = UrnCollection.from(parts[1]);
            collectionValue = parts[1];
        } else {
            setCollection(UrnCollection.UNKNOWN);
        }
    }

    private void parseId(String[] parts) {
        if (parts.length > 2) {
            String[] ids = Arrays.copyOfRange(parts, 2, parts.length);
            stringId = Strings.joinOn(SEPARATOR).join(ids);
            try {
                longId = Long.valueOf(stringId);
            } catch (NumberFormatException e) {
                longId = Consts.NOT_SET;
            }
        } else {
            stringId = Strings.EMPTY;
            longId = Consts.NOT_SET;
        }
    }

    private boolean isLegacySoundsCollection() {
        return isSoundCloud() && collection == UrnCollection.SOUNDS;
    }

    private String fromLegacySoundsCollection() {
        setCollection(UrnCollection.TRACKS);
        return buildContent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Urn that = (Urn) o;
        return MoreObjects.equal(this.content, that.content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
