package com.soundcloud.android.collections;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class CollectionsOptions {

    static Builder builder() {
        return new AutoValue_CollectionsOptions
                .Builder()
                .showPosts(false)
                .showLikes(false)
                .showOfflineOnly(false)
                .sortByTitle(false);
    }

    public abstract boolean showLikes();

    public abstract boolean showPosts();

    public abstract boolean showOfflineOnly();

    public abstract boolean sortByTitle();

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder showLikes(boolean enabled);
        abstract Builder showPosts(boolean enabled);
        abstract Builder showOfflineOnly(boolean enabled);
        abstract Builder sortByTitle(boolean enabled);
        abstract CollectionsOptions build();
    }

}
