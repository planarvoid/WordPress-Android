package com.soundcloud.android.events;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.stations.StationProperty;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.objects.MoreObjects;
import rx.functions.Func1;

import android.support.v4.util.ArrayMap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class EntityStateChangedEvent implements UrnEvent {

    public static final int UPDATED = 0;
    public static final int REPOST = 3;
    public static final int PLAYLIST_EDITED = 4;
    public static final int TRACK_ADDED_TO_PLAYLIST = 5;
    public static final int TRACK_REMOVED_FROM_PLAYLIST = 6;
    public static final int FOLLOWING = 7;
    public static final int ENTITY_CREATED = 8;
    public static final int ENTITY_DELETED = 9;
    public static final int PLAYLIST_PUSHED_TO_SERVER = 10;
    public static final int STATIONS_COLLECTION_UPDATED = 11;
    public static final int PLAYLIST_MARKED_FOR_DOWNLOAD = 12;

    public static final Func1<EntityStateChangedEvent, Boolean> IS_TRACK_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isSingularChange() && event.getFirstUrn().isTrack();
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
            return event.isTrackAddedEvent() || event.isTrackRemovedEvent() || event.isPlaylistEditedEvent();
        }
    };

    public static final Func1<EntityStateChangedEvent, Boolean> IS_STATION_COLLECTION_UPDATED = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.getKind() == STATIONS_COLLECTION_UPDATED;
        }
    };

    public static final Func1<EntityStateChangedEvent, Urn> TO_URN = new Func1<EntityStateChangedEvent, Urn>() {
        @Override
        public Urn call(EntityStateChangedEvent entityStateChangedEvent) {
            return entityStateChangedEvent.getFirstUrn();
        }
    };

    public static final Func1<EntityStateChangedEvent, List<Urn>> TO_URNS = new Func1<EntityStateChangedEvent, List<Urn>>() {
        @Override
        public List<Urn> call(EntityStateChangedEvent entityStateChangedEvent) {
            return newArrayList(entityStateChangedEvent.getChangeMap().keySet());
        }
    };

    public static EntityStateChangedEvent forUpdate(Collection<PropertySet> propertiesSet) {
        return create(UPDATED, propertiesSet);
    }

    public static EntityStateChangedEvent forUpdate(PropertySet propertySet) {
        return create(UPDATED, propertySet);
    }

    public static EntityStateChangedEvent fromFollowing(PropertySet newFollowingState) {
        return create(FOLLOWING, newFollowingState);
    }

    public static EntityStateChangedEvent fromRepost(Urn urn, boolean reposted) {
        return create(REPOST, PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlayableProperty.IS_USER_REPOST.bind(reposted)));
    }

    public static EntityStateChangedEvent fromRepost(Collection<PropertySet> newRepostStates) {
        return create(REPOST, newRepostStates);
    }

    public static EntityStateChangedEvent fromEntityCreated(Urn urn) {
        return create(ENTITY_CREATED, PropertySet.from(EntityProperty.URN.bind(urn)));
    }

    public static EntityStateChangedEvent fromEntityCreated(Collection<PropertySet> properties) {
        return create(ENTITY_CREATED, properties);
    }

    public static EntityStateChangedEvent fromEntityDeleted(Urn urn) {
        return create(ENTITY_DELETED, PropertySet.from(EntityProperty.URN.bind(urn)));
    }

    public static EntityStateChangedEvent fromEntityDeleted(Collection<PropertySet> properties) {
        return create(ENTITY_DELETED, properties);
    }

    public static EntityStateChangedEvent fromPlaylistPushedToServer(Urn localUrn, PropertySet playlist) {
        Map<Urn, PropertySet> changeMap = Collections.singletonMap(localUrn, playlist);
        return new AutoValue_EntityStateChangedEvent(PLAYLIST_PUSHED_TO_SERVER, changeMap);
    }

    public static EntityStateChangedEvent fromStationsUpdated(Urn station) {
        return create(STATIONS_COLLECTION_UPDATED, PropertySet.from(StationProperty.URN.bind(station)));
    }

    public static EntityStateChangedEvent fromTrackAddedToPlaylist(Urn playlistUrn, int trackCount) {
        return fromTrackAddedToPlaylist(PropertySet.from(
                PlayableProperty.URN.bind(playlistUrn),
                PlaylistProperty.TRACK_COUNT.bind(trackCount)));
    }

    public static EntityStateChangedEvent fromTrackAddedToPlaylist(PropertySet newPlaylistState) {
        return create(TRACK_ADDED_TO_PLAYLIST, newPlaylistState);
    }

    public static EntityStateChangedEvent fromPlaylistEdited(PropertySet newPlaylistState) {
        return create(PLAYLIST_EDITED, newPlaylistState);
    }

    public static EntityStateChangedEvent fromTrackRemovedFromPlaylist(PropertySet newPlaylistState) {
        return create(TRACK_REMOVED_FROM_PLAYLIST, newPlaylistState);
    }

    public static EntityStateChangedEvent fromPlaylistsMarkedForDownload(List<Urn> playlistUrns) {
        return create(PLAYLIST_MARKED_FOR_DOWNLOAD, toMarkedForOfflinePropertySets(playlistUrns, true));
    }

    public static EntityStateChangedEvent fromPlaylistsUnmarkedForDownload(List<Urn> playlistUrns) {
        return create(PLAYLIST_MARKED_FOR_DOWNLOAD, toMarkedForOfflinePropertySets(playlistUrns, false));
    }

    private static Collection<PropertySet> toMarkedForOfflinePropertySets(List<Urn> playlistUrns,
                                                                          final boolean markedForOffline) {
        return MoreCollections.transform(playlistUrns, new Function<Urn, PropertySet>() {
            @Override
            public PropertySet apply(Urn urn) {
                return PropertySet.from(
                        PlayableProperty.URN.bind(urn),
                        OfflineProperty.IS_MARKED_FOR_OFFLINE.bind(markedForOffline));
            }
        });
    }

    static EntityStateChangedEvent create(int kind, Collection<PropertySet> changedEntities) {
        Map<Urn, PropertySet> changeMap = new ArrayMap<>(changedEntities.size());
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

    public boolean containsCreatedPlaylist() {
        return getFirstUrn().isPlaylist() && getKind() == ENTITY_CREATED;
    }

    public boolean containsDeletedPlaylist() {
        return getFirstUrn().isPlaylist() && getKind() == ENTITY_DELETED;
    }

    public boolean isFollowingKind() {
        return getKind() == FOLLOWING;
    }

    private boolean isTrackAddedEvent() {
        return getKind() == TRACK_ADDED_TO_PLAYLIST;
    }

    private boolean isPlaylistEditedEvent() {
        return getKind() == PLAYLIST_EDITED;
    }

    private boolean isTrackRemovedEvent() {
        return getKind() == TRACK_REMOVED_FROM_PLAYLIST;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("kind", getKind()).add("changeMap", getChangeMap()).toString();
    }

}
