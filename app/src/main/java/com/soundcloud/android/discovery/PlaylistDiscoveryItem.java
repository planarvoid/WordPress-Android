package com.soundcloud.android.discovery;

import java.util.List;

public class PlaylistDiscoveryItem extends DiscoveryItem {

    private final List<String> popularTags;
    private final List<String> recentTags;

    protected PlaylistDiscoveryItem(List<String> popularTags, List<String> recentTags) {
        super(Kind.PlaylistTagsItem);
        this.popularTags = popularTags;
        this.recentTags = recentTags;
    }

    public List<String> getPopularTags() {
        return popularTags;
    }

    public List<String> getRecentTags() {
        return recentTags;
    }
}
