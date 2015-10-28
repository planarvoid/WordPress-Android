package com.soundcloud.android.collections;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PlaylistsOptions {

    public static Builder builder() {
        return new AutoValue_PlaylistsOptions
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
    public abstract static class Builder {
        public abstract Builder showLikes(boolean enabled);
        public abstract Builder showPosts(boolean enabled);
        public abstract Builder showOfflineOnly(boolean enabled);
        public abstract Builder sortByTitle(boolean enabled);
        public abstract PlaylistsOptions build();
    }

}
