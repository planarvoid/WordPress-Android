package com.soundcloud.android.discovery;

import java.util.List;

class PlaylistDiscoveryItem extends DiscoveryItem {

    private final List<String> popularTags;
    private final List<String> recentTags;

    PlaylistDiscoveryItem(List<String> popularTags, List<String> recentTags) {
        super(Kind.PlaylistTagsItem);
        this.popularTags = popularTags;
        this.recentTags = recentTags;
    }

    List<String> getPopularTags() {
        return popularTags;
    }

    List<String> getRecentTags() {
        return recentTags;
    }
}
