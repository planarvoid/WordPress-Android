package com.soundcloud.android.playback.service;

import com.google.common.base.Objects;
import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

public class PlaySessionSource implements Parcelable {

    public static final PlaySessionSource EMPTY = new PlaySessionSource();

    static final String PREF_KEY_ORIGIN_SCREEN_TAG = "origin_url"; //legacy
    static final String PREF_KEY_PLAYLIST_ID = "set_id"; //legacy

    private final String originScreen;
    private Urn playlistUrn = Urn.NOT_SET;
    private Urn playlistOwnerUrn = Urn.NOT_SET;
    private String exploreVersion;

    public PlaySessionSource(Parcel in) {
        originScreen = in.readString();
        exploreVersion = in.readString();
        playlistUrn = in.readParcelable(PlaySessionSource.class.getClassLoader());
        playlistOwnerUrn = in.readParcelable(PlaySessionSource.class.getClassLoader());
    }

    public PlaySessionSource(SharedPreferences sharedPreferences) {
        originScreen = sharedPreferences.getString(PREF_KEY_ORIGIN_SCREEN_TAG, ScTextUtils.EMPTY_STRING);
        playlistUrn = Urn.forPlaylist(sharedPreferences.getLong(PREF_KEY_PLAYLIST_ID, Consts.NOT_SET));
    }

    private PlaySessionSource() {
        this(ScTextUtils.EMPTY_STRING);
    }

    public PlaySessionSource(Screen screen) {
        this(screen.get());
    }

    public PlaySessionSource(String originScreen) {
        this.originScreen = originScreen;
    }

    public String getOriginScreen() {
        return originScreen;
    }

    public Urn getPlaylistUrn() {
        return playlistUrn;
    }

    public Urn getPlaylistOwnerUrn() {
        return playlistOwnerUrn;
    }

    public void setPlaylist(Urn playlist, Urn playlistOwner) {
        this.playlistUrn = playlist;
        this.playlistOwnerUrn = playlistOwner;
    }

    public void setExploreVersion(String exploreVersion) {
        this.exploreVersion = exploreVersion;
    }

    public boolean originatedInExplore() {
        return originScreen.startsWith("explore");
    }

    public String getInitialSource() {
        return ScTextUtils.isNotBlank(exploreVersion) ? DiscoverySource.EXPLORE.value() : ScTextUtils.EMPTY_STRING;
    }

    public String getInitialSourceVersion() {
        return ScTextUtils.isNotBlank(exploreVersion) ? exploreVersion : ScTextUtils.EMPTY_STRING;
    }

    public boolean isFromPlaylist() {
        return playlistUrn != Urn.NOT_SET;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(originScreen);
        dest.writeString(exploreVersion);
        dest.writeParcelable(playlistUrn, 0);
        dest.writeParcelable(playlistOwnerUrn, 0);
    }

    public void saveToPreferences(SharedPreferences.Editor editor) {
        editor.putString(PREF_KEY_ORIGIN_SCREEN_TAG, originScreen);
        editor.putLong(PREF_KEY_PLAYLIST_ID, playlistUrn.getNumericId());
    }

    public static final Parcelable.Creator<PlaySessionSource> CREATOR = new Parcelable.Creator<PlaySessionSource>() {
        public PlaySessionSource createFromParcel(Parcel in) {
            return new PlaySessionSource(in);
        }

        public PlaySessionSource[] newArray(int size) {
            return new PlaySessionSource[size];
        }
    };

    public static void clearPreferenceKeys(SharedPreferences.Editor editor) {
        editor.remove(PREF_KEY_ORIGIN_SCREEN_TAG);
        editor.remove(PREF_KEY_PLAYLIST_ID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PlaySessionSource that = (PlaySessionSource) o;

        return Objects.equal(playlistUrn, that.playlistUrn)
                && Objects.equal(playlistOwnerUrn, that.playlistOwnerUrn)
                && Objects.equal(exploreVersion, that.exploreVersion)
                && Objects.equal(originScreen, that.originScreen);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(playlistUrn, playlistOwnerUrn, exploreVersion, originScreen);
    }

    public enum DiscoverySource {
        RECOMMENDER, EXPLORE;

        public String value() {
            return this.toString().toLowerCase(Locale.ENGLISH);
        }
    }
}
