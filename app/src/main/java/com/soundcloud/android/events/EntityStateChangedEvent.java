package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import rx.functions.Func1;

import android.support.v4.util.ArrayMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public final class EntityStateChangedEvent implements UrnEvent {

    public static final int ENTITY_SYNCED = 0;
    public static final int LIKE = 2;
    public static final int REPOST = 3;
    public static final int MARKED_FOR_OFFLINE = 4;
    public static final int TRACK_ADDED_TO_PLAYLIST = 5;
    public static final int TRACK_REMOVED_FROM_PLAYLIST = 6;
    public static final int FOLLOWING = 7;

    public static final Func1<EntityStateChangedEvent, Boolean> IS_TRACK_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isSingularChange() && event.getFirstUrn().isTrack();
        }
    };

    public static final Func1<EntityStateChangedEvent, Boolean> IS_TRACK_LIKE_EVENT_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isTrackLikeEvent();
        }
    };

    public static final Func1<EntityStateChangedEvent, Boolean> IS_PLAYLIST_OFFLINE_CONTENT_EVENT_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isSingularChange() && event.getFirstUrn().isPlaylist() && event.getKind() == MARKED_FOR_OFFLINE;
        }
    };

    public static final Func1<EntityStateChangedEvent, Boolean> IS_OFFLINE_LIKES_EVENT_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isOfflineLikesEvent();
        }
    };

    public static final Func1<EntityStateChangedEvent, Boolean> IS_TRACK_LIKED_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isTrackLikeEvent() && event.getNextChangeSet().get(TrackProperty.IS_LIKED);
        }
    };

    public static final Func1<? super EntityStateChangedEvent, Boolean> IS_PLAYLIST_LIKED_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isPlaylistLike() && event.getNextChangeSet().get(PlaylistProperty.IS_LIKED);
        }
    };

    public static final Func1<EntityStateChangedEvent, Boolean> IS_TRACK_UNLIKED_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isTrackLikeEvent() && !event.getNextChangeSet().get(TrackProperty.IS_LIKED);
        }
    };

    public static final Func1<EntityStateChangedEvent, Boolean> IS_PLAYLIST_UNLIKED_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isPlaylistLike() && !event.getNextChangeSet().get(PlaylistProperty.IS_LIKED);
        }
    };

    public static final Func1<EntityStateChangedEvent, Boolean> IS_TRACK_ADDED_TO_PLAYLIST_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isTrackAddedEvent();
        }
    };

    public static final Func1<EntityStateChangedEvent, Boolean> IS_PLAYLIST_CONTENT_CHANGED_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isTrackAddedEvent() || event.isTrackRemovedEvent();
        }
    };

    public static final Func1<EntityStateChangedEvent, Urn> TO_URN = new Func1<EntityStateChangedEvent, Urn>() {
        @Override
        public Urn call(EntityStateChangedEvent entityStateChangedEvent) {
            return entityStateChangedEvent.getFirstUrn();
        }
    };
    public static final Func1<EntityStateChangedEvent, PropertySet> TO_SINGULAR_CHANGE = new Func1<EntityStateChangedEvent, PropertySet>() {
        @Override
        public PropertySet call(EntityStateChangedEvent event) {
            return event.getNextChangeSet();
        }
    };

    private final int kind;
    private final Map<Urn, PropertySet> changeMap;

    public static EntityStateChangedEvent fromSync(Collection<PropertySet> changedEntities) {
        return new EntityStateChangedEvent(ENTITY_SYNCED, changedEntities);
    }

    public static EntityStateChangedEvent fromSync(PropertySet changedEntity) {
        return new EntityStateChangedEvent(ENTITY_SYNCED, Collections.singleton(changedEntity));
    }

    public static EntityStateChangedEvent fromLike(Urn urn, boolean liked, int likesCount) {
        return new EntityStateChangedEvent(LIKE, PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlayableProperty.IS_LIKED.bind(liked),
                PlayableProperty.LIKES_COUNT.bind(likesCount)));
    }

    public static EntityStateChangedEvent fromLike(PropertySet newLikeState) {
        return new EntityStateChangedEvent(LIKE, newLikeState);
    }

    public static EntityStateChangedEvent fromFollowing(PropertySet newFollowingState) {
        return new EntityStateChangedEvent(FOLLOWING, newFollowingState);
    }

    public static EntityStateChangedEvent fromRepost(Urn urn, boolean reposted) {
        return fromRepost(PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlayableProperty.IS_REPOSTED.bind(reposted)));
    }

    public static EntityStateChangedEvent fromRepost(PropertySet newRepostState) {
        return new EntityStateChangedEvent(REPOST, newRepostState);
    }

    public static EntityStateChangedEvent fromMarkedForOffline(Urn urn, boolean isMarkedForOffline) {
        return new EntityStateChangedEvent(MARKED_FOR_OFFLINE, PropertySet.from(
                PlayableProperty.URN.bind(urn),
                OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE.bind(isMarkedForOffline)));
    }

    public static EntityStateChangedEvent fromLikesMarkedForOffline(boolean isMarkedForOffline) {
        return new EntityStateChangedEvent(MARKED_FOR_OFFLINE, PropertySet.from(
                PlayableProperty.URN.bind(Urn.NOT_SET),
                OfflineProperty.Collection.OFFLINE_LIKES.bind(isMarkedForOffline)));
    }

    public static EntityStateChangedEvent fromTrackAddedToPlaylist(Urn playlistUrn, int trackCount) {
        return fromTrackAddedToPlaylist(PropertySet.from(
                PlayableProperty.URN.bind(playlistUrn),
                PlaylistProperty.TRACK_COUNT.bind(trackCount)));
    }

    public static EntityStateChangedEvent fromTrackAddedToPlaylist(PropertySet newPlaylistState) {
        return new EntityStateChangedEvent(TRACK_ADDED_TO_PLAYLIST, newPlaylistState);
    }

    public static EntityStateChangedEvent fromTrackRemovedFromPlaylist(PropertySet newPlaylistState) {
        return new EntityStateChangedEvent(TRACK_REMOVED_FROM_PLAYLIST, newPlaylistState);
    }

    EntityStateChangedEvent(int kind, Collection<PropertySet> changedEntities) {
        this.kind = kind;
        this.changeMap = new ArrayMap<>(changedEntities.size());
        for (PropertySet entity : changedEntities) {
            this.changeMap.put(entity.get(EntityProperty.URN), entity);
        }
    }

    EntityStateChangedEvent(int kind, PropertySet changedEntity) {
        this(kind, Collections.singleton(changedEntity));
    }

    public int getKind() {
        return kind;
    }

    public Map<Urn, PropertySet> getChangeMap() {
        return changeMap;
    }

    public Urn getFirstUrn() {
        return changeMap.keySet().iterator().next();
    }

    /**
     * @return for a single change event, this returns the single change set; if more than one entity changed,
     * returns the first available change set.
     */
    public PropertySet getNextChangeSet() {
        return changeMap.values().iterator().next();
    }

    public boolean isSingularChange() {
        return changeMap.size() == 1;
    }

    public boolean isTrackLikeEvent() {
        return isSingularChange() && getFirstUrn().isTrack() && kind == LIKE;
    }

    public boolean isPlaylistLike() {
        return isSingularChange() && getFirstUrn().isPlaylist() && kind == LIKE;
    }

    private boolean isTrackAddedEvent() {
        return kind == TRACK_ADDED_TO_PLAYLIST;
    }

    private boolean isTrackRemovedEvent() {
        return kind == TRACK_REMOVED_FROM_PLAYLIST;
    }

    private boolean isOfflineLikesEvent() {
        return kind == MARKED_FOR_OFFLINE && getNextChangeSet().contains(OfflineProperty.Collection.OFFLINE_LIKES);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("kind", kind).add("changeMap", changeMap).toString();
    }
}
