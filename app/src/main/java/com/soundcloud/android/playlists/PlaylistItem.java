package com.soundcloud.android.playlists;

import static com.soundcloud.android.utils.DateUtils.yearFromDateString;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import rx.functions.Func1;

import android.content.Context;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistItem extends PlayableItem {

    public static final String TYPE_PLAYLIST = "playlist";
    public static final String TYPE_ALBUM = "album";
    public static final String TYPE_EP = "ep";
    public static final String TYPE_SINGLE = "single";
    public static final String TYPE_COMPILATION = "compilation";

    @VisibleForTesting
    public static int getSetTypeTitle(String playableType) {
        switch (playableType) {
            case PlaylistItem.TYPE_PLAYLIST:
                return R.string.set_type_default_label;
            case PlaylistItem.TYPE_ALBUM:
                return R.string.set_type_album_label;
            case PlaylistItem.TYPE_EP:
                return R.string.set_type_ep_label;
            case PlaylistItem.TYPE_SINGLE:
                return R.string.set_type_single_label;
            case PlaylistItem.TYPE_COMPILATION:
                return R.string.set_type_compilation_label;
            default:
                return R.string.set_type_default_label;
        }
    }

    public static int getSetTypeLabel(String playableType) {
        switch (playableType) {
            case PlaylistItem.TYPE_PLAYLIST:
                return R.string.set_type_default_label_for_text;
            case PlaylistItem.TYPE_ALBUM:
                return R.string.set_type_album_label_for_text;
            case PlaylistItem.TYPE_EP:
                return R.string.set_type_ep_label_for_text;
            case PlaylistItem.TYPE_SINGLE:
                return R.string.set_type_single_label_for_text;
            case PlaylistItem.TYPE_COMPILATION:
                return R.string.set_type_compilation_label_for_text;
            default:
                return R.string.set_type_default_label_for_text;
        }
    }

    public static PlaylistItem from(PropertySet propertySet) {
        return new PlaylistItem(propertySet);
    }

    public static PlaylistItem from(ApiPlaylist apiPlaylist) {
        return new PlaylistItem(apiPlaylist.toPropertySet());
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

    public PlaylistItem(PropertySet source) {
        super(source);
    }

    @Override
    public String getPlayableType() {
        if (isAlbum()) {
            return source.getOrElse(PlaylistProperty.SET_TYPE, TYPE_ALBUM);
        } else {
            return TYPE_PLAYLIST;
        }
    }

    public int getTrackCount() {
        return source.get(PlaylistProperty.TRACK_COUNT);
    }

    public Optional<Boolean> isMarkedForOffline() {
        return Optional.fromNullable(source.getOrElseNull(OfflineProperty.IS_MARKED_FOR_OFFLINE));
    }

    public OfflineState getDownloadState() {
        return source.getOrElse(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
    }

    public List<String> getTags() {
        final Optional<List<String>> optionalTags = source.get(PlaylistProperty.TAGS);
        return optionalTags.isPresent() ? optionalTags.get() : Collections.<String>emptyList();
    }

    public long getDuration() {
        return source.get(PlaylistProperty.PLAYLIST_DURATION);
    }

    public String getPermalinkUrl() {
        return source.get(PlaylistProperty.PERMALINK_URL);
    }

    public boolean isLocalPlaylist() {
        return getUrn().getNumericId() < 0;
    }

    public boolean isLikedByUser() {
        return source.get(PlaylistProperty.IS_USER_LIKE);
    }

    public boolean isRepostedByUser() {
        return source.getOrElse(PlaylistProperty.IS_USER_REPOST, false);
    }

    public boolean isPublic() {
        return !isPrivate();
    }

    public boolean isAlbum() {
        return source.getOrElse(PlaylistProperty.IS_ALBUM, false);
    }

    public String getReleaseYear() {
        String releaseDate = source.getOrElse(PlaylistProperty.RELEASE_DATE, Strings.EMPTY);
        if (releaseDate.isEmpty()) return Strings.EMPTY;

        try {
            return Integer.toString(yearFromDateString(releaseDate, "yyyy-MM-dd"));
        } catch (ParseException e) {
            return Strings.EMPTY;
        }
    }

    public String getLabel(Context context) {
        final String title = context.getString(getSetTypeTitle(getPlayableType()));

        if (!isAlbum()) return title;

        StringBuilder builder = new StringBuilder();
        builder.append(title);
        String releaseYear = getReleaseYear();
        if (!releaseYear.isEmpty()) {
            builder.append(String.format(" · %s", releaseYear));
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlaylistItem && ((PlaylistItem) o).source.equals(this.source);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).addValue(source).toString();
    }
}
