package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import rx.functions.Func1;

import android.support.v4.util.ArrayMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@AutoValue
public abstract class EntityStateChangedEvent implements UrnEvent {

    public static final int ENTITY_SYNCED = 0;
    public static final int LIKE = 2;
    public static final int REPOST = 3;
    public static final int MARKED_FOR_OFFLINE = 4;
    public static final int TRACK_ADDED_TO_PLAYLIST = 5;
    public static final int TRACK_REMOVED_FROM_PLAYLIST = 6;
    public static final int FOLLOWING = 7;
    public static final int PLAYLIST_CREATED = 8;
    public static final int PLAYLIST_DELETED = 9;
    public static final int PLAYLIST_PUSHED_TO_SERVER = 10;

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

    public static EntityStateChangedEvent fromSync(Collection<PropertySet> changedEntities) {
        return create(ENTITY_SYNCED, changedEntities);
    }

    public static EntityStateChangedEvent fromSync(PropertySet changedEntity) {
        return create(ENTITY_SYNCED, Collections.singleton(changedEntity));
    }

    public static EntityStateChangedEvent fromLike(Urn urn, boolean liked, int likesCount) {
        return create(LIKE, PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlayableProperty.IS_LIKED.bind(liked),
                PlayableProperty.LIKES_COUNT.bind(likesCount)));
    }

    public static EntityStateChangedEvent fromLike(PropertySet newLikeState) {
        return create(LIKE, newLikeState);
    }

    public static EntityStateChangedEvent fromFollowing(PropertySet newFollowingState) {
        return create(FOLLOWING, newFollowingState);
    }

    public static EntityStateChangedEvent fromRepost(Urn urn, boolean reposted) {
        return fromRepost(PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlayableProperty.IS_REPOSTED.bind(reposted)));
    }

    public static EntityStateChangedEvent fromRepost(PropertySet newRepostState) {
        return create(REPOST, newRepostState);
    }

    public static EntityStateChangedEvent fromMarkedForOffline(Urn urn, boolean isMarkedForOffline) {
        return create(MARKED_FOR_OFFLINE, PropertySet.from(
                PlayableProperty.URN.bind(urn),
                OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE.bind(isMarkedForOffline)));
    }

    public static EntityStateChangedEvent fromLikesMarkedForOffline(boolean isMarkedForOffline) {
        return create(MARKED_FOR_OFFLINE, PropertySet.from(
                PlayableProperty.URN.bind(Urn.NOT_SET),
                OfflineProperty.Collection.OFFLINE_LIKES.bind(isMarkedForOffline)));
    }

    public static EntityStateChangedEvent fromPlaylistCreated(Urn newPlaylistUrn) {
        return create(PLAYLIST_CREATED, PropertySet.from(PlaylistProperty.URN.bind(newPlaylistUrn)));
    }

    public static EntityStateChangedEvent fromPlaylistDeleted(Urn playlist) {
        return create(PLAYLIST_DELETED, PropertySet.from(PlaylistProperty.URN.bind(playlist)));
    }

    public static EntityStateChangedEvent fromPlaylistPushedToServer(Urn localUrn, PropertySet playlist) {
        Map<Urn, PropertySet> changeMap = Collections.singletonMap(localUrn, playlist);
        return new AutoValue_EntityStateChangedEvent(PLAYLIST_PUSHED_TO_SERVER, changeMap);
    }

    public static EntityStateChangedEvent fromTrackAddedToPlaylist(Urn playlistUrn, int trackCount) {
        return fromTrackAddedToPlaylist(PropertySet.from(
                PlayableProperty.URN.bind(playlistUrn),
                PlaylistProperty.TRACK_COUNT.bind(trackCount)));
    }

    public static EntityStateChangedEvent fromTrackAddedToPlaylist(PropertySet newPlaylistState) {
        return create(TRACK_ADDED_TO_PLAYLIST, newPlaylistState);
    }

    public static EntityStateChangedEvent fromTrackRemovedFromPlaylist(PropertySet newPlaylistState) {
        return create(TRACK_REMOVED_FROM_PLAYLIST, newPlaylistState);
    }

    static EntityStateChangedEvent create(int kind, Collection<PropertySet> changedEntities) {
        ArrayMap changeMap = new ArrayMap<>(changedEntities.size());
        for (PropertySet entity : changedEntities) {
            changeMap.put(entity.get(EntityProperty.URN), entity);
        }
        return new AutoValue_EntityStateChangedEvent(kind, changeMap);
    }

    static EntityStateChangedEvent create(int kind, PropertySet changedEntity) {
        return create(kind, Collections.singleton(changedEntity));
    }

    public abstract int getKind();

    public abstract Map<Urn, PropertySet> getChangeMap();

    public Urn getFirstUrn() {
        return getChangeMap().keySet().iterator().next();
    }

    /**
     * @return for a single change event, this returns the single change set; if more than one entity changed,
     * returns the first available change set.
     */
    public PropertySet getNextChangeSet() {
        return getChangeMap().values().iterator().next();
    }

    public boolean isSingularChange() {
        return getChangeMap().size() == 1;
    }

    public boolean isTrackLikeEvent() {
        return isSingularChange() && getFirstUrn().isTrack() && getKind() == LIKE;
    }

    public boolean isPlaylistLike() {
        return isSingularChange() && getFirstUrn().isPlaylist() && getKind() == LIKE;
    }

    public boolean isLike() {
        return isSingularChange() && getKind() == LIKE;
    }

    private boolean isTrackAddedEvent() {
        return getKind() == TRACK_ADDED_TO_PLAYLIST;
    }

    private boolean isTrackRemovedEvent() {
        return getKind() == TRACK_REMOVED_FROM_PLAYLIST;
    }

    private boolean isOfflineLikesEvent() {
        return getKind() == MARKED_FOR_OFFLINE && getNextChangeSet().contains(OfflineProperty.Collection.OFFLINE_LIKES);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("kind", getKind()).add("changeMap", getChangeMap()).toString();
    }


}
