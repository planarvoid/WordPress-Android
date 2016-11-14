package com.soundcloud.android.playback;

import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.discovery.charts.ChartSourceInfo;
import com.soundcloud.android.discovery.recommendations.RecommendationsSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

public class PlaySessionSource implements Parcelable {

    public static final PlaySessionSource EMPTY = new PlaySessionSource();
    public static final Creator<PlaySessionSource> CREATOR = new Creator<PlaySessionSource>() {
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
    static final String PREF_KEY_COLLECTION_SIZE = "collection_size";

    private final String originScreen;
    private Urn collectionUrn = Urn.NOT_SET;
    private Urn collectionOwnerUrn = Urn.NOT_SET;
    private int collectionSize = Consts.NOT_SET;

    private String exploreVersion;
    private SearchQuerySourceInfo searchQuerySourceInfo;
    private PromotedSourceInfo promotedSourceInfo;
    private RecommendationsSourceInfo recommendationsSourceInfo;
    private DiscoverySource discoverySource;
    private ChartSourceInfo chartSourceInfo;

    public static PlaySessionSource forPlaylist(Screen screen, Urn playlist, Urn playlistOwner, int playlistSize) {
        return forPlaylist(screen.get(), playlist, playlistOwner, playlistSize);
    }

    public static PlaySessionSource forPlaylist(String screen, Urn playlist, Urn playlistOwner, int playlistSize) {
        final PlaySessionSource source = new PlaySessionSource(screen);
        source.collectionUrn = playlist;
        source.collectionOwnerUrn = playlistOwner;
        source.collectionSize = playlistSize;
        return source;
    }

    public static PlaySessionSource forStation(Screen screen, Urn station) {
        return forStation(screen.get(), station, DiscoverySource.STATIONS);
    }

    public static PlaySessionSource forStation(String screen, Urn station, DiscoverySource discoverySource) {
        final PlaySessionSource source = new PlaySessionSource(screen);
        source.discoverySource = discoverySource;
        source.collectionUrn = station;
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

    public static PlaySessionSource forArtist(Screen screen, Urn artist) {
        return forArtist(screen.get(), artist);
    }

    public static PlaySessionSource forArtist(String screen, Urn artist) {
        final PlaySessionSource source = new PlaySessionSource(screen);
        source.collectionUrn = artist;
        return source;
    }

    public static PlaySessionSource forRecommendations(Screen screen, int queryPosition, Urn queryUrn) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.recommendationsSourceInfo = RecommendationsSourceInfo.create(queryPosition, queryUrn);
        return playSessionSource;
    }

    public static PlaySessionSource forChart(String screenTag,
                                             int queryPosition,
                                             Urn queryUrn,
                                             ChartType chartType,
                                             ChartCategory chartCategory, Urn genre) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screenTag);
        playSessionSource.chartSourceInfo = ChartSourceInfo.create(queryPosition, queryUrn, chartType, chartCategory, genre);
        return playSessionSource;
    }

    public static PlaySessionSource forHistory(Screen screen) {
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.discoverySource = DiscoverySource.HISTORY;
        return playSessionSource;
    }

    public PlaySessionSource(Parcel in) {
        originScreen = in.readString();
        exploreVersion = in.readString();
        collectionSize = in.readInt();
        collectionUrn = in.readParcelable(PlaySessionSource.class.getClassLoader());
        collectionOwnerUrn = in.readParcelable(PlaySessionSource.class.getClassLoader());
        searchQuerySourceInfo = in.readParcelable(SearchQuerySourceInfo.class.getClassLoader());
        promotedSourceInfo = in.readParcelable(PromotedSourceInfo.class.getClassLoader());
        discoverySource = (DiscoverySource) in.readSerializable();
    }

    public PlaySessionSource(SharedPreferences sharedPreferences) {
        originScreen = sharedPreferences.getString(PREF_KEY_ORIGIN_SCREEN_TAG, Strings.EMPTY);
        collectionUrn = readUrn(sharedPreferences, PREF_KEY_COLLECTION_URN);
        collectionOwnerUrn = readUrn(sharedPreferences, PREF_KEY_COLLECTION_OWNER_URN);
        collectionSize = sharedPreferences.getInt(PREF_KEY_COLLECTION_SIZE, Consts.NOT_SET);
    }

    private PlaySessionSource() {
        this(Strings.EMPTY);
    }

    public PlaySessionSource(Screen screen) {
        this(screen.get());
    }

    public PlaySessionSource(String originScreen) {
        this.originScreen = originScreen;
    }

    @Nullable
    DiscoverySource getDiscoverySource() {
        return discoverySource;
    }

    private Urn readUrn(SharedPreferences sharedPreferences, String key) {
        final String value = sharedPreferences.getString(key, Strings.EMPTY);
        if (value.isEmpty()) {
            return Urn.NOT_SET;
        } else {
            return new Urn(value);
        }
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

    public int getCollectionSize() {
        return collectionSize;
    }

    public boolean originatedInExplore() {
        return originScreen.startsWith("explore");
    }

    String getInitialSource() {
        if (Strings.isNotBlank(exploreVersion)) {
            return DiscoverySource.EXPLORE.value();
        } else if (isFromStreamTrack()) {
            return DiscoverySource.STREAM.value();
        } else if (hasDiscoverySource()) {
            return discoverySource.value();
        }
        return Strings.EMPTY;
    }

    public String getInitialSourceVersion() {
        return Strings.isNotBlank(exploreVersion) ? exploreVersion : Strings.EMPTY;
    }

    public boolean isFromSearchQuery() {
        return searchQuerySourceInfo != null;
    }

    public boolean isFromStations() {
        return getCollectionUrn().isStation();
    }

    public boolean isFromPromotedItem() {
        return promotedSourceInfo != null;
    }

    private boolean isFromPlaylist() {
        return getCollectionUrn().isPlaylist();
    }

    private boolean isFromStreamTrack() {
        return originScreen.equals(Screen.STREAM.get()) && !isFromPlaylist();
    }

    private boolean hasDiscoverySource() {
        return discoverySource != null;
    }

    public boolean isFromRecommendations() {
        return recommendationsSourceInfo != null;
    }

    public boolean isFromChart() {
        return chartSourceInfo != null;
    }

    public boolean isFromPlaylistHistory() {
        return discoverySource == DiscoverySource.HISTORY;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(originScreen);
        dest.writeString(exploreVersion);
        dest.writeInt(collectionSize);
        dest.writeParcelable(collectionUrn, 0);
        dest.writeParcelable(collectionOwnerUrn, 0);
        dest.writeParcelable(searchQuerySourceInfo, 0);
        dest.writeParcelable(promotedSourceInfo, 0);
        dest.writeSerializable(discoverySource);
    }

    public void saveToPreferences(SharedPreferences.Editor editor) {
        editor.putString(PREF_KEY_ORIGIN_SCREEN_TAG, originScreen);
        editor.putString(PREF_KEY_COLLECTION_URN, collectionUrn.toString());
        editor.putString(PREF_KEY_COLLECTION_OWNER_URN, collectionOwnerUrn.toString());
        editor.putInt(PREF_KEY_COLLECTION_SIZE, collectionSize);
    }

    public static void clearPreferenceKeys(SharedPreferences.Editor editor) {
        editor.remove(PREF_KEY_ORIGIN_SCREEN_TAG);
        editor.remove(PREF_KEY_COLLECTION_URN);
        editor.remove(PREF_KEY_COLLECTION_OWNER_URN);
        editor.remove(PREF_KEY_COLLECTION_SIZE);
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
                && collectionSize == that.collectionSize
                && MoreObjects.equal(exploreVersion, that.exploreVersion)
                && MoreObjects.equal(originScreen, that.originScreen)
                && MoreObjects.equal(promotedSourceInfo, that.promotedSourceInfo)
                && MoreObjects.equal(recommendationsSourceInfo, that.recommendationsSourceInfo)
                && MoreObjects.equal(chartSourceInfo, that.chartSourceInfo)
                && MoreObjects.equal(discoverySource, that.discoverySource);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(collectionUrn,
                                    collectionOwnerUrn,
                                    collectionSize,
                                    exploreVersion,
                                    originScreen,
                                    recommendationsSourceInfo,
                                    chartSourceInfo,
                                    discoverySource);
    }

    public void setSearchQuerySourceInfo(SearchQuerySourceInfo searchQuerySourceInfo) {
        this.searchQuerySourceInfo = searchQuerySourceInfo;
    }

    @Nullable
    public SearchQuerySourceInfo getSearchQuerySourceInfo() {
        return searchQuerySourceInfo;
    }

    @Nullable
    public RecommendationsSourceInfo getRecommendationsSourceInfo() {
        return recommendationsSourceInfo;
    }

    public void setPromotedSourceInfo(PromotedSourceInfo promotedSourceInfo) {
        this.promotedSourceInfo = promotedSourceInfo;
    }

    public ChartSourceInfo getChartSourceInfo() {
        return chartSourceInfo;
    }

    @Nullable
    public PromotedSourceInfo getPromotedSourceInfo() {
        return promotedSourceInfo;
    }

    public boolean originatedFromDeeplink() {
        return originScreen.equals(Screen.DEEPLINK.get());
    }

    public boolean originatedInSearchSuggestions() {
        return originScreen.startsWith(Screen.SEARCH_SUGGESTIONS.get());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("originScreen", originScreen)
                          .add("collectionUrn", collectionUrn)
                          .add("collectionOwnerUrn", collectionOwnerUrn)
                          .add("exploreVersion", exploreVersion)
                          .add("searchQuerySourceInfo", searchQuerySourceInfo)
                          .add("promotedSourceInfo", promotedSourceInfo)
                          .add("discoverySource", discoverySource)
                          .toString();
    }
}