package com.soundcloud.android.playback.service;

import com.google.common.base.Objects;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

public class PlaySessionSource implements Parcelable{

    public static final PlaySessionSource EMPTY = new PlaySessionSource();

    static final String PREF_KEY_ORIGIN_SCREEN_TAG = "origin_url"; //legacy
    static final String PREF_KEY_PLAYLIST_ID = "set_id"; //legacy

    public enum DiscoverySource {
        RECOMMENDER, EXPLORE;

        public String value() {
            return this.toString().toLowerCase(Locale.ENGLISH);
        }
    }

    private final String originScreen;
    private long playlistId = Playlist.NOT_SET;
    private long playlistOwnerId = User.NOT_SET;
    private String exploreVersion;

    public PlaySessionSource(Parcel in) {
        originScreen = in.readString();
        exploreVersion = in.readString();
        playlistId = in.readLong();
        playlistOwnerId = in.readLong();
    }

    public PlaySessionSource(SharedPreferences mSharedPreferences) {
        originScreen = mSharedPreferences.getString(PREF_KEY_ORIGIN_SCREEN_TAG, ScTextUtils.EMPTY_STRING);
        playlistId = mSharedPreferences.getLong(PREF_KEY_PLAYLIST_ID, ScModel.NOT_SET);
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

    public long getPlaylistId() {
        return playlistId;
    }

    public long getPlaylistOwnerId() {
        return playlistOwnerId;
    }

    public void setPlaylist(@NotNull Playlist playlist) {
        playlistId = playlist.getId();
        playlistOwnerId = playlist.getUserId();
    }

    public void setExploreVersion(String exploreVersion) {
        this.exploreVersion = exploreVersion;
    }

    public boolean originatedInExplore(){
        return originScreen.startsWith("explore");
    }

    public String getInitialSource() {
        return ScTextUtils.isNotBlank(exploreVersion) ? DiscoverySource.EXPLORE.value() : ScTextUtils.EMPTY_STRING;
    }

    public String getInitialSourceVersion() {
        return ScTextUtils.isNotBlank(exploreVersion) ? exploreVersion : ScTextUtils.EMPTY_STRING;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(originScreen);
        dest.writeString(exploreVersion);
        dest.writeLong(playlistId);
        dest.writeLong(playlistOwnerId);
    }

    public void saveToPreferences(SharedPreferences.Editor editor) {
        editor.putString(PREF_KEY_ORIGIN_SCREEN_TAG, originScreen);
        editor.putLong(PREF_KEY_PLAYLIST_ID, playlistId);
    }

    public static final Parcelable.Creator<PlaySessionSource> CREATOR = new Parcelable.Creator<PlaySessionSource>() {
        public PlaySessionSource createFromParcel(Parcel in) {
            return new PlaySessionSource(in);
        }

        public PlaySessionSource[] newArray(int size) {
            return new PlaySessionSource[size];
        }
    };

    public static void clearPreferenceKeys(SharedPreferences.Editor editor){
        editor.remove(PREF_KEY_ORIGIN_SCREEN_TAG);
        editor.remove(PREF_KEY_PLAYLIST_ID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlaySessionSource that = (PlaySessionSource) o;

        return Objects.equal(playlistId, that.playlistId)
                && Objects.equal(playlistOwnerId, that.playlistOwnerId)
                && Objects.equal(exploreVersion, that.exploreVersion)
                && Objects.equal(originScreen, that.originScreen);
    }

    @Override
    public int hashCode() {
        int result = originScreen.hashCode();
        result = 31 * result + (int) (playlistId ^ (playlistId >>> 32));
        result = 31 * result + (int) (playlistOwnerId ^ (playlistOwnerId >>> 32));
        result = 31 * result + (exploreVersion != null ? exploreVersion.hashCode() : 0);
        return result;
    }
}
