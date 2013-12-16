package com.soundcloud.android.playback.service;

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

    static String PREF_KEY_ORIGIN_SCREEN_TAG = "origin_url"; //legacy
    static String PREF_KEY_PLAYLIST_ID = "set_id"; //legacy

    public enum DiscoverySource {
        RECOMMENDER, EXPLORE;

        public String value() {
            return this.toString().toLowerCase(Locale.ENGLISH);
        }
    }

    private final String mOriginScreen;
    private long mPlaylistId = Playlist.NOT_SET;
    private long mPlaylistOwnerId = User.NOT_SET;
    private String mExploreVersion;

    public PlaySessionSource(Parcel in) {
        mPlaylistId = in.readLong();
        mOriginScreen = in.readString();
        mExploreVersion = in.readString();
    }

    public PlaySessionSource(SharedPreferences mSharedPreferences) {
        mOriginScreen = mSharedPreferences.getString(PREF_KEY_ORIGIN_SCREEN_TAG, ScTextUtils.EMPTY_STRING);
        mPlaylistId = mSharedPreferences.getLong(PREF_KEY_PLAYLIST_ID, ScModel.NOT_SET);
    }

    private PlaySessionSource() {
        this(ScTextUtils.EMPTY_STRING);
    }

    public PlaySessionSource(Screen screen) {
        this(screen.get());
    }

    public PlaySessionSource(String originScreen) {
        mOriginScreen = originScreen;
    }

    public String getOriginScreen() {
        return mOriginScreen;
    }

    public long getPlaylistId() {
        return mPlaylistId;
    }

    public void setPlaylist(@NotNull Playlist playlist) {
        mPlaylistId = playlist.getId();
        mPlaylistOwnerId = playlist.getUserId();
    }

    public void setExploreVersion(String exploreVersion) {
        this.mExploreVersion = exploreVersion;
    }

    // TODO, finalize this once we implement page tracking
    public boolean originatedInExplore(){
        return mOriginScreen.startsWith("explore");
    }

    public String getInitialSource() {
        return ScTextUtils.isNotBlank(mExploreVersion) ? DiscoverySource.EXPLORE.value() : ScTextUtils.EMPTY_STRING;
    }

    public String getInitialSourceVersion() {
        return ScTextUtils.isNotBlank(mExploreVersion) ? mExploreVersion : ScTextUtils.EMPTY_STRING;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mPlaylistId);
        dest.writeString(mOriginScreen);
        dest.writeString(mExploreVersion);
    }

    public void saveToPreferences(SharedPreferences.Editor editor) {
        editor.putString(PREF_KEY_ORIGIN_SCREEN_TAG, mOriginScreen);
        editor.putLong(PREF_KEY_PLAYLIST_ID, mPlaylistId);
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

        if (mPlaylistId != that.mPlaylistId) return false;
        if (mExploreVersion != null ? !mExploreVersion.equals(that.mExploreVersion) : that.mExploreVersion != null)
            return false;
        if (!mOriginScreen.equals(that.mOriginScreen)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mOriginScreen.hashCode();
        result = 31 * result + (int) (mPlaylistId ^ (mPlaylistId >>> 32));
        result = 31 * result + (mExploreVersion != null ? mExploreVersion.hashCode() : 0);
        return result;
    }
}
