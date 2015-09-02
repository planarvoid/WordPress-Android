package com.soundcloud.android.playback;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.objects.MoreObjects;

public class TrackSourceInfo {

    private final String originScreen;
    private final boolean userTriggered;

    private String source;
    private String sourceVersion;

    private Urn reposter = Urn.NOT_SET;
    private Urn playlistUrn = Urn.NOT_SET;
    private Urn playlistOwnerUrn = Urn.NOT_SET;
    private int playlistPosition;

    private SearchQuerySourceInfo searchQuerySourceInfo;
    private PromotedSourceInfo promotedSourceInfo;

    public TrackSourceInfo(String originScreen, boolean userTriggered) {
        this.originScreen = originScreen;
        this.userTriggered = userTriggered;
    }

    public void setSource(String source, String sourceVersion) {
        this.source = source;
        this.sourceVersion = sourceVersion;
    }

    public void setOriginPlaylist(Urn playlistUrn, int position, Urn playlistOwnerUrn) {
        this.playlistUrn = playlistUrn;
        this.playlistPosition = position;
        this.playlistOwnerUrn = playlistOwnerUrn;
    }

    public void setSearchQuerySourceInfo(SearchQuerySourceInfo searchQuerySourceInfo) {
        this.searchQuerySourceInfo = searchQuerySourceInfo;
    }

    public void setPromotedSourceInfo(PromotedSourceInfo promotedSourceInfo) {
        this.promotedSourceInfo = promotedSourceInfo;
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

    public Urn getPlaylistUrn() {
        return playlistUrn;
    }

    public int getPlaylistPosition() {
        return playlistPosition;
    }

    public Urn getPlaylistOwnerUrn() {
        return playlistOwnerUrn;
    }

    public boolean hasSource() {
        return ScTextUtils.isNotBlank(source);
    }

    public boolean isFromPlaylist() {
        return playlistUrn != Urn.NOT_SET;
    }

    public boolean isFromSearchQuery() {
        return searchQuerySourceInfo != null;
    }

    public boolean hasReposter() {
        return reposter != Urn.NOT_SET;
    }

    public Urn getReposter() {
        return reposter;
    }

    public boolean isFromPromoted() { return promotedSourceInfo != null; }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper toStringHelper = MoreObjects.toStringHelper(TrackSourceInfo.class)
                .add("originScreen", originScreen)
                .add("userTriggered", userTriggered);

        if (hasSource()) {
            toStringHelper.add("source", source).add("sourceVersion", sourceVersion);
        }
        if (isFromPlaylist()) {
            toStringHelper.add("playlistUrn", playlistUrn)
                    .add("playlistPos", playlistPosition)
                    .add("playlistOwnerUrn", playlistOwnerUrn);
        }

        if (isFromSearchQuery()) {
            toStringHelper.add("searchQuerySourceInfo", searchQuerySourceInfo);
        }

        return toStringHelper.toString();
    }

}
