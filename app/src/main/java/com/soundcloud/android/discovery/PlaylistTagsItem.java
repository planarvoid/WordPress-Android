package com.soundcloud.android.discovery;

import java.util.List;

public class PlaylistTagsItem extends DiscoveryItem {

    private final List<String> popularTags;
    private final List<String> recentTags;

    public PlaylistTagsItem(List<String> popularTags, List<String> recentTags) {
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

    public Boolean isEmpty() {
        return popularTags.isEmpty() && recentTags.isEmpty();
    }
}
