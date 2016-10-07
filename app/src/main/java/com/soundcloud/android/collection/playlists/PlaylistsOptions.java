package com.soundcloud.android.collection.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.strings.Strings;

@AutoValue
public abstract class PlaylistsOptions {

    public static final PlaylistsOptions SHOW_ALL = new AutoValue_PlaylistsOptions.Builder()
            .showLikes(true)
            .showPosts(true)
            .showOfflineOnly(false)
            .sortByTitle(false)
            .textFilter(Strings.EMPTY)
            .build();

    public static Builder builder() {
        return new AutoValue_PlaylistsOptions
                .Builder()
                .showPosts(false)
                .showLikes(false)
                .showOfflineOnly(false)
                .sortByTitle(false)
                .textFilter(Strings.EMPTY);
    }

    public static Builder builder(PlaylistsOptions options) {
        return new AutoValue_PlaylistsOptions
                .Builder()
                .showPosts(options.showPosts())
                .showLikes(options.showLikes())
                .showOfflineOnly(options.showOfflineOnly())
                .sortByTitle(options.sortByTitle())
                .textFilter(options.textFilter());
    }

    public abstract boolean showLikes();

    public abstract boolean showPosts();

    public abstract boolean showOfflineOnly();

    public abstract boolean sortByTitle();

    public abstract String textFilter();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder showLikes(boolean enabled);

        public abstract Builder showPosts(boolean enabled);

        public abstract Builder showOfflineOnly(boolean enabled);

        public abstract Builder sortByTitle(boolean enabled);

        public abstract Builder textFilter(String query);

        public abstract PlaylistsOptions build();
    }

}
