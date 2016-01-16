package com.soundcloud.android.playlists;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistItem extends PlayableItem {

    public static PlaylistItem from(PropertySet propertySet) {
        return new PlaylistItem(propertySet);
    }

    public static PlaylistItem from(ApiPlaylist apiPlaylist) {
        return new PlaylistItem(apiPlaylist.toPropertySet());
    }

    public static Func1<PropertySet, PlaylistItem> fromPropertySet() {
        return new Func1<PropertySet, PlaylistItem>() {
            @Override
            public PlaylistItem call(PropertySet bindings) {
                return PlaylistItem.from(bindings);
            }
        };
    }

    public static Func1<List<PropertySet>, List<PlaylistItem>> fromPropertySets() {
        return new Func1<List<PropertySet>, List<PlaylistItem>>() {
            @Override
            public List<PlaylistItem> call(List<PropertySet> bindings) {
                List<PlaylistItem> playlistItems = new ArrayList<>(bindings.size());
                for (PropertySet source : bindings) {
                    playlistItems.add(from(source));
                }
                return playlistItems;
            }
        };
    }

    public static <T extends Iterable<ApiPlaylist>> Func1<T, List<PlaylistItem>> fromApiPlaylists() {
        return new Func1<T, List<PlaylistItem>>() {
            @Override
            public List<PlaylistItem> call(T sourceList) {
                List<PlaylistItem> playlistItems = new ArrayList<>();
                for (ApiPlaylist source : sourceList) {
                    playlistItems.add(from(source.toPropertySet()));
                }
                return playlistItems;
            }
        };
    }

    public PlaylistItem(PropertySet source) {
        super(source);
    }

    public int getTrackCount() {
        return source.get(PlaylistProperty.TRACK_COUNT);
    }

    public Optional<Boolean> isMarkedForOffline() {
        return Optional.fromNullable(source.getOrElseNull(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE));
    }

    public boolean isPosted() {
        return source.getOrElse(PlaylistProperty.IS_POSTED, false);
    }

    public OfflineState getDownloadState() {
        if (source.getOrElse(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, false)) {
            return source.getOrElse(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
        }
        return OfflineState.NOT_OFFLINE;
    }

    public List<String> getTags() {
        final Optional<List<String>> optionalTags = source.get(PlaylistProperty.TAGS);
        return optionalTags.isPresent() ? optionalTags.get() : Collections.<String>emptyList();
    }

    public long getDuration() {
        return source.get(PlayableProperty.PLAY_DURATION);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlaylistItem && ((PlaylistItem) o).source.equals(this.source);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }
}
