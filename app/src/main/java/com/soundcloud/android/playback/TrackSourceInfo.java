package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.discovery.charts.ChartSourceInfo;
import com.soundcloud.android.discovery.recommendations.RecommendationsSourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StationsSourceInfo;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;

public class TrackSourceInfo {

    private final String originScreen;
    private final boolean userTriggered;

    private String source;
    private String sourceVersion;

    private Urn reposter = Urn.NOT_SET;
    private Urn collectionUrn = Urn.NOT_SET;
    private Urn playlistOwnerUrn = Urn.NOT_SET;
    private int playlistPosition;

    private SearchQuerySourceInfo searchQuerySourceInfo;
    private PromotedSourceInfo promotedSourceInfo;
    private StationsSourceInfo stationsSourceInfo;
    private RecommendationsSourceInfo recommendationsSourceInfo;
    private ChartSourceInfo chartSourceInfo;

    public TrackSourceInfo(String originScreen, boolean userTriggered) {
        this.originScreen = originScreen;
        this.userTriggered = userTriggered;
    }

    public void setSource(String source, String sourceVersion) {
        this.source = source;
        this.sourceVersion = sourceVersion;
    }

    public void setOriginPlaylist(Urn playlistUrn, int position, Urn playlistOwnerUrn) {
        this.collectionUrn = playlistUrn;
        this.playlistPosition = position;
        this.playlistOwnerUrn = playlistOwnerUrn;
    }

    public void setStationSourceInfo(Urn stationUrn, StationsSourceInfo sourceInfo) {
        this.collectionUrn = stationUrn;
        this.stationsSourceInfo = sourceInfo;
    }

    public void setRecommendationsSourceInfo(RecommendationsSourceInfo sourceInfo) {
        this.recommendationsSourceInfo = sourceInfo;
    }

    public void setSearchQuerySourceInfo(SearchQuerySourceInfo searchQuerySourceInfo) {
        this.searchQuerySourceInfo = searchQuerySourceInfo;
    }

    public void setPromotedSourceInfo(PromotedSourceInfo promotedSourceInfo) {
        this.promotedSourceInfo = promotedSourceInfo;
    }

    public void setChartSourceInfo(ChartSourceInfo chartSourceInfo) {
        this.chartSourceInfo = chartSourceInfo;
    }

    public void setReposter(Urn reposter) {
        this.reposter = reposter;
    }

    public SearchQuerySourceInfo getSearchQuerySourceInfo() {
        return searchQuerySourceInfo;
    }

    public PromotedSourceInfo getPromotedSourceInfo() {
        return promotedSourceInfo;
    }

    public StationsSourceInfo getStationsSourceInfo() {
        return stationsSourceInfo;
    }

    public RecommendationsSourceInfo getRecommendationsSourceInfo() {
        return recommendationsSourceInfo;
    }

    public boolean hasStationsSourceInfo() {
        return stationsSourceInfo != null;
    }

    public boolean getIsUserTriggered() {
        return userTriggered;
    }

    public String getOriginScreen() {
        return originScreen;
    }

    public String getSource() {
        return source;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public Urn getCollectionUrn() {
        return collectionUrn;
    }

    public boolean hasCollectionUrn() {
        return !collectionUrn.equals(Urn.NOT_SET);
    }

    public int getPlaylistPosition() {
        return playlistPosition;
    }

    public Urn getPlaylistOwnerUrn() {
        return playlistOwnerUrn;
    }

    public ChartSourceInfo getChartSourceInfo() {
        return chartSourceInfo;
    }

    public boolean hasSource() {
        return Strings.isNotBlank(source);
    }

    public boolean isFromPlaylist() {
        return collectionUrn != Urn.NOT_SET && collectionUrn.isPlaylist();
    }

    public boolean isFromStation() {
        return collectionUrn != Urn.NOT_SET && collectionUrn.isStation();
    }

    public boolean isFromRecommendations() {
        return recommendationsSourceInfo != null;
    }

    public boolean isFromSearchQuery() {
        return searchQuerySourceInfo != null;
    }

    public boolean isFromChart() {
        return chartSourceInfo != null;
    }

    public boolean hasReposter() {
        return reposter != Urn.NOT_SET;
    }

    public Urn getReposter() {
        return reposter;
    }

    public boolean isFromPromoted() {
        return promotedSourceInfo != null;
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper toStringHelper = MoreObjects.toStringHelper(TrackSourceInfo.class)
                                                                     .add("originScreen", originScreen)
                                                                     .add("userTriggered", userTriggered);

        if (hasSource()) {
            toStringHelper.add("source", source).add("sourceVersion", sourceVersion);
        }

        toStringHelper.add("collectionUrn", collectionUrn);
        if (isFromPlaylist()) {
            toStringHelper
                    .add("playlistPos", playlistPosition)
                    .add("playlistOwnerUrn", playlistOwnerUrn);
        }

        if (isFromSearchQuery()) {
            toStringHelper.add("searchQuerySourceInfo", searchQuerySourceInfo);
        }

        return toStringHelper.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackSourceInfo that = (TrackSourceInfo) o;

        if (userTriggered != that.userTriggered) return false;
        if (playlistPosition != that.playlistPosition) return false;
        if (originScreen != null ? !originScreen.equals(that.originScreen) : that.originScreen != null) return false;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (sourceVersion != null ? !sourceVersion.equals(that.sourceVersion) : that.sourceVersion != null)
            return false;
        if (reposter != null ? !reposter.equals(that.reposter) : that.reposter != null) return false;
        if (collectionUrn != null ? !collectionUrn.equals(that.collectionUrn) : that.collectionUrn != null)
            return false;
        if (playlistOwnerUrn != null ? !playlistOwnerUrn.equals(that.playlistOwnerUrn) : that.playlistOwnerUrn != null)
            return false;
        if (searchQuerySourceInfo != null ?
            !searchQuerySourceInfo.equals(that.searchQuerySourceInfo) :
            that.searchQuerySourceInfo != null) return false;
        if (promotedSourceInfo != null ?
            !promotedSourceInfo.equals(that.promotedSourceInfo) :
            that.promotedSourceInfo != null) return false;
        if (stationsSourceInfo != null ?
            !stationsSourceInfo.equals(that.stationsSourceInfo) :
            that.stationsSourceInfo != null) return false;
        if (recommendationsSourceInfo != null ?
            !recommendationsSourceInfo.equals(that.recommendationsSourceInfo) :
            that.recommendationsSourceInfo != null) return false;
        return chartSourceInfo != null ?
               chartSourceInfo.equals(that.chartSourceInfo) :
               that.chartSourceInfo == null;

    }

    @Override
    public int hashCode() {
        int result = originScreen != null ? originScreen.hashCode() : 0;
        result = 31 * result + (userTriggered ? 1 : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (sourceVersion != null ? sourceVersion.hashCode() : 0);
        result = 31 * result + (reposter != null ? reposter.hashCode() : 0);
        result = 31 * result + (collectionUrn != null ? collectionUrn.hashCode() : 0);
        result = 31 * result + (playlistOwnerUrn != null ? playlistOwnerUrn.hashCode() : 0);
        result = 31 * result + playlistPosition;
        result = 31 * result + (searchQuerySourceInfo != null ? searchQuerySourceInfo.hashCode() : 0);
        result = 31 * result + (promotedSourceInfo != null ? promotedSourceInfo.hashCode() : 0);
        result = 31 * result + (stationsSourceInfo != null ? stationsSourceInfo.hashCode() : 0);
        result = 31 * result + (recommendationsSourceInfo != null ? recommendationsSourceInfo.hashCode() : 0);
        result = 31 * result + (chartSourceInfo != null ? chartSourceInfo.hashCode() : 0);
        return result;
    }
}
