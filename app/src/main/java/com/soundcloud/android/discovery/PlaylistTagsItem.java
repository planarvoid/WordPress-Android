package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class PlaylistTagsItem extends DiscoveryItem {


    public static PlaylistTagsItem create(List<String> popularTags,
                                          List<String> recentTags) {
        return new AutoValue_PlaylistTagsItem(Kind.PlaylistTagsItem,
                                              popularTags,
                                              recentTags);
    }

    public abstract List<String> getPopularTags();

    public abstract List<String> getRecentTags();

    public Boolean isEmpty() {
        return getPopularTags().isEmpty() && getRecentTags().isEmpty();
    }

}
