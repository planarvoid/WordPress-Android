package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.objects.MoreObjects;
import org.jetbrains.annotations.Nullable;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

public class PlaySessionSource implements Parcelable {

    public static final PlaySessionSource EMPTY = new PlaySessionSource();
    public static final Parcelable.Creator<PlaySessionSource> CREATOR = new Parcelable.Creator<PlaySessionSource>() {
        public PlaySessionSource createFromParcel(Parcel in) {
            return new PlaySessionSource(in);
        }

        public PlaySessionSource[] newArray(int size) {
            return new PlaySessionSource[size];
        }
    };
    static final String PREF_KEY_ORIGIN_SCREEN_TAG = "origin_url"; // legacy name

    static final String PREF_KEY_COLLECTION_URN = "collection_urn";
    static final String PREF_KEY_COLLECTION_OWNER_URN = "collection_owner_urn";

    private final String originScreen;
    private Urn collectionUrn = Urn.NOT_SET;
    private Urn collectionOwnerUrn = Urn.NOT_SET;

    private String exploreVersion;
    private SearchQuerySourceInfo searchQuerySourceInfo;
    private PromotedSourceInfo promotedSourceInfo;

    public static PlaySessionSource forPlaylist(Screen screen, Urn playlist, Urn playlistOwner) {
        return forPlaylist(screen.get(), playlist, playlistOwner);
    }

    public static PlaySessionSource forPlaylist(String screen, Urn playlist, Urn playlistOwner) {
        final PlaySessionSource source = new PlaySessionSource(screen);
        source.collectionUrn = playlist;
        source.collectionOwnerUrn = playlistOwner;
        return source;
    }

    public static PlaySessionSource forExplore(Screen screen, String version) {
        return forExplore(screen.get(), version);
    }

    public static PlaySessionSource forExplore(String screen, String version) {
        final PlaySessionSource source = new PlaySessionSource(screen);
        source.exploreVersion = version;
        return source;
    }

    public PlaySessionSource(Parcel in) {
        originScreen = in.readString();
        exploreVersion = in.readString();
        collectionUrn = in.readParcelable(PlaySessionSource.class.getClassLoader());
        collectionOwnerUrn = in.readParcelable(PlaySessionSource.class.getClassLoader());
        searchQuerySourceInfo = in.readParcelable(SearchQuerySourceInfo.class.getClassLoader());
        promotedSourceInfo = in.readParcelable(PromotedSourceInfo.class.getClassLoader());
    }

    public PlaySessionSource(SharedPreferences sharedPreferences) {
        originScreen = sharedPreferences.getString(PREF_KEY_ORIGIN_SCREEN_TAG, ScTextUtils.EMPTY_STRING);
        collectionUrn = readUrn(sharedPreferences, PREF_KEY_COLLECTION_URN);
        collectionOwnerUrn = readUrn(sharedPreferences, PREF_KEY_COLLECTION_OWNER_URN);
    }

    private Urn readUrn(SharedPreferences sharedPreferences, String key) {
        final String value = sharedPreferences.getString(key, ScTextUtils.EMPTY_STRING);
        if (value.isEmpty()) {
            return Urn.NOT_SET;
        } else {
            return new Urn(value);
        }
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

    public Urn getCollectionUrn() {
        return collectionUrn;
    }

    public Urn getCollectionOwnerUrn() {
        return collectionOwnerUrn;
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

    public boolean isFromQuery() {
        return searchQuerySourceInfo != null;
    }

    public boolean isFromPromotedItem() {
        return promotedSourceInfo != null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(originScreen);
        dest.writeString(exploreVersion);
        dest.writeParcelable(collectionUrn, 0);
        dest.writeParcelable(collectionOwnerUrn, 0);
        dest.writeParcelable(searchQuerySourceInfo, 0);
        dest.writeParcelable(promotedSourceInfo, 0);
    }

    public void saveToPreferences(SharedPreferences.Editor editor) {
        editor.putString(PREF_KEY_ORIGIN_SCREEN_TAG, originScreen);
        editor.putString(PREF_KEY_COLLECTION_URN, collectionUrn.toString());
        editor.putString(PREF_KEY_COLLECTION_OWNER_URN, collectionOwnerUrn.toString());
    }

    public static void clearPreferenceKeys(SharedPreferences.Editor editor) {
        editor.remove(PREF_KEY_ORIGIN_SCREEN_TAG);
        editor.remove(PREF_KEY_COLLECTION_URN);
        editor.remove(PREF_KEY_COLLECTION_OWNER_URN);
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

        return MoreObjects.equal(collectionUrn, that.collectionUrn)
                && MoreObjects.equal(collectionOwnerUrn, that.collectionOwnerUrn)
                && MoreObjects.equal(exploreVersion, that.exploreVersion)
                && MoreObjects.equal(originScreen, that.originScreen);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(collectionUrn, collectionOwnerUrn, exploreVersion, originScreen);
    }

    public void setSearchQuerySourceInfo(SearchQuerySourceInfo searchQuerySourceInfo) {
        this.searchQuerySourceInfo = searchQuerySourceInfo;
    }

    @Nullable
    public SearchQuerySourceInfo getSearchQuerySourceInfo() {
        return searchQuerySourceInfo;
    }

    public void setPromotedSourceInfo(PromotedSourceInfo promotedSourceInfo) {
        this.promotedSourceInfo = promotedSourceInfo;
    }

    @Nullable
    public PromotedSourceInfo getPromotedSourceInfo() {
        return promotedSourceInfo;
    }

    public void clearPromotedSourceInfo() {
        promotedSourceInfo = null;
    }

    public boolean originatedFromDeeplink() {
        return originScreen.equals(Screen.DEEPLINK.get());
    }

    public boolean originatedInSearchSuggestions() {
        return originScreen.startsWith(Screen.SEARCH_SUGGESTIONS.get());
    }

    public enum DiscoverySource {
        RECOMMENDER, EXPLORE;

        public String value() {
            return this.toString().toLowerCase(Locale.ENGLISH);
        }
    }
}
