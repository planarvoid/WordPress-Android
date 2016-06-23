package com.soundcloud.android.playlists;

import static com.soundcloud.android.utils.DateUtils.yearFromDateString;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import rx.functions.Func1;

import android.content.Context;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistItem extends PlayableItem {

    private static final Map<String, Integer> SET_TYPE_TO_LABEL_MAP = new HashMap<String, Integer>() {{
        put("album", R.string.set_type_album_label);
        put("ep", R.string.set_type_ep_label);
        put("single", R.string.set_type_single_label);
        put("compilation", R.string.set_type_compilation_label);
    }};

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

    public int getTrackCount() {
        return source.get(PlaylistProperty.TRACK_COUNT);
    }

    public Optional<Boolean> isMarkedForOffline() {
        return Optional.fromNullable(source.getOrElseNull(OfflineProperty.IS_MARKED_FOR_OFFLINE));
    }

    public boolean isPostedByUser() {
        return source.getOrElse(PlaylistProperty.IS_POSTED, false);
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

    public int getSetTypeLabel() {
        if (!isAlbum()) return R.string.set_type_default_label;

        String setType = source.getOrElse(PlaylistProperty.SET_TYPE, "album");

        if (SET_TYPE_TO_LABEL_MAP.containsKey(setType)) return SET_TYPE_TO_LABEL_MAP.get(setType);

        return R.string.set_type_album_label;
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
        if (!isAlbum()) return context.getString(getSetTypeLabel());

        StringBuilder builder = new StringBuilder();
        builder.append(context.getString(getSetTypeLabel()));
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
}
