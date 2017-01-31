package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.UpdatablePlaylistItem;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;

public class PlaylistItem extends PlayableItem implements UpdatablePlaylistItem {

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

    public static PlaylistItem from(ApiPlaylist apiPlaylist, boolean repost) {
        PlaylistItem playlistItem = PlaylistItem.from(apiPlaylist);
        playlistItem.setRepost(repost);
        return playlistItem;
    }
    public static PlaylistItem fromLiked(ApiPlaylist apiPlaylist, boolean liked) {
        PlaylistItem playlistItem = PlaylistItem.from(apiPlaylist);
        playlistItem.setLikedByCurrentUser(liked);
        return playlistItem;
    }


    public static PlaylistItem from(Playlist playlist) {
        return new PlaylistItem(PropertySet.from(
                PlaylistProperty.URN.bind(playlist.urn()),
                PlaylistProperty.TITLE.bind(playlist.title()),
                PlaylistProperty.CREATED_AT.bind(playlist.createdAt()),
                PlaylistProperty.CREATOR_URN.bind(playlist.creatorUrn()),
                PlaylistProperty.CREATOR_NAME.bind(playlist.creatorName()),
                PlaylistProperty.PLAYLIST_DURATION.bind(playlist.duration()),
                PlaylistProperty.TRACK_COUNT.bind(playlist.trackCount()),
                PlaylistProperty.IS_PRIVATE.bind(playlist.isPrivate()),
                PlaylistProperty.GENRE.bind(playlist.genre()),
                PlaylistProperty.SET_TYPE.bind(playlist.setType()),
                PlaylistProperty.IS_USER_LIKE.bind(playlist.isLikedByCurrentUser().or(false)),
                OfflineProperty.IS_MARKED_FOR_OFFLINE.bind(playlist.isMarkedForOffline().or(false)),
                OfflineProperty.OFFLINE_STATE.bind(playlist.offlineState().or(OfflineState.NOT_OFFLINE)),
                PlayableProperty.IS_USER_REPOST.bind(playlist.isRepostedByCurrentUser().or(false)),
                EntityProperty.IMAGE_URL_TEMPLATE.bind(playlist.imageUrlTemplate()),
                PlaylistProperty.IS_ALBUM.bind(playlist.isAlbum()),
                PlaylistProperty.RELEASE_DATE.bind(playlist.releaseDate()),
                PlaylistProperty.TAGS.bind(playlist.tags()),
                PlaylistProperty.LIKES_COUNT.bind(playlist.likesCount()),
                PlaylistProperty.REPOSTS_COUNT.bind(playlist.repostCount()),
                PlaylistProperty.PERMALINK_URL.bind(playlist.permalinkUrl().or(Strings.EMPTY))
        ));
    }

    public static PlaylistItem from(ApiPlaylist apiPlaylist) {
        return new PlaylistItem(PropertySet.from(
                PlaylistProperty.URN.bind(apiPlaylist.getUrn()),
                PlaylistProperty.TITLE.bind(apiPlaylist.getTitle()),
                PlaylistProperty.CREATED_AT.bind(apiPlaylist.getCreatedAt()),
                PlaylistProperty.PLAYLIST_DURATION.bind(apiPlaylist.getDuration()),
                PlaylistProperty.PERMALINK_URL.bind(apiPlaylist.getPermalinkUrl()),
                PlaylistProperty.IS_PRIVATE.bind(!apiPlaylist.isPublic()),
                PlaylistProperty.TRACK_COUNT.bind(apiPlaylist.getTrackCount()),
                PlaylistProperty.LIKES_COUNT.bind(apiPlaylist.getStats().getLikesCount()),
                PlaylistProperty.REPOSTS_COUNT.bind(apiPlaylist.getStats().getRepostsCount()),
                PlaylistProperty.CREATOR_NAME.bind(apiPlaylist.getUsername()),
                PlaylistProperty.CREATOR_URN.bind(apiPlaylist.getUser() != null ? apiPlaylist.getUser().getUrn() : Urn.NOT_SET),
                PlaylistProperty.TAGS.bind(Optional.fromNullable(apiPlaylist.getTags())),
                PlaylistProperty.GENRE.bind(Optional.fromNullable(apiPlaylist.getGenre())),
                EntityProperty.IMAGE_URL_TEMPLATE.bind(apiPlaylist.getImageUrlTemplate()),
                PlaylistProperty.IS_ALBUM.bind(apiPlaylist.isAlbum()),
                PlaylistProperty.SET_TYPE.bind(apiPlaylist.getSetType()),
                PlaylistProperty.RELEASE_DATE.bind(apiPlaylist.getReleaseDate()),
                // TODO : These properties should be removed from our domain entities, and only appear in view models
                PlaylistProperty.IS_USER_LIKE.bind(false),
                PlaylistProperty.IS_USER_REPOST.bind(false),
                OfflineProperty.OFFLINE_STATE.bind(OfflineState.NOT_OFFLINE),
                OfflineProperty.IS_MARKED_FOR_OFFLINE.bind(false)
        ));
    }

    public PlaylistItem(PropertySet source) {
        super(source);
    }

    @Override
    public PlaylistItem updatedWithOfflineState(OfflineState offlineState) {
        super.updatedWithOfflineState(offlineState);
        return this;
    }

    public PlaylistItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        super.updatedWithLike(likeStatus);
        return this;
    }

    public PlaylistItem updateLikeState(boolean isLiked) {
        super.updateLikeState(isLiked);
        return this;
    }


    public PlaylistItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        super.updatedWithRepost(repostStatus);
        return this;
    }

    @Override
    public PlaylistItem updatedWithTrackCount(int trackCount) {
        this.setTrackCount(trackCount);
        return this;
    }

    @Override
    public PlaylistItem updatedWithMarkedForOffline(boolean markedForOffline) {
        this.setMarkedForOffline(markedForOffline);
        return this;
    }

    @Override
    public PlaylistItem updatedWithPlaylist(Playlist playlist) {
        return from(playlist);
    }

    @Override
    public String getPlayableType() {
        if (isAlbum()) {
            return source.getOrElse(PlaylistProperty.SET_TYPE, TYPE_ALBUM);
        } else {
            return TYPE_PLAYLIST;
        }
    }

    public String getSetType() {
        return source.get(PlaylistProperty.SET_TYPE);
    }

    public int getTrackCount() {
        return source.get(PlaylistProperty.TRACK_COUNT);
    }

    public void setTrackCount(int trackCount) {
        source.put(PlaylistProperty.TRACK_COUNT, trackCount);
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

    @Override
    public long getDuration() {
        return source.get(PlaylistProperty.PLAYLIST_DURATION);
    }

    public boolean isPublic() {
        return !isPrivate();
    }

    public boolean isAlbum() {
        return source.getOrElse(PlaylistProperty.IS_ALBUM, false);
    }

    public String getReleaseDate() {
        return source.getOrElse(PlaylistProperty.RELEASE_DATE, Strings.EMPTY);
    }

    public String getLabel(Resources resources) {
        return PlaylistUtils.formatPlaylistTitle(resources, getPlayableType(), isAlbum(), getReleaseDate());
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(source, 0);
    }

    public PlaylistItem(Parcel in) {
        super(in.readParcelable(PlaylistItem.class.getClassLoader()));
    }

    public static final Parcelable.Creator<PlaylistItem> CREATOR = new Parcelable.Creator<PlaylistItem>() {
        public PlaylistItem createFromParcel(Parcel in) {
            return new PlaylistItem(in);
        }

        public PlaylistItem[] newArray(int size) {
            return new PlaylistItem[size];
        }
    };

    public void setMarkedForOffline(boolean markedForOffline) {
        source.put(OfflineProperty.IS_MARKED_FOR_OFFLINE, markedForOffline);
    }

    public void setOfflineState(OfflineState offlineState) {
        source.put(OfflineProperty.OFFLINE_STATE, offlineState);
    }
}
