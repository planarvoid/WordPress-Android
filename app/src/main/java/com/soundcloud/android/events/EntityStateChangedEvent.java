package com.soundcloud.android.events;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import rx.functions.Func1;

import android.support.v4.util.ArrayMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public final class EntityStateChangedEvent {

    public static final int ENTITY_SYNCED = 0;
    public static final int DOWNLOAD = 1;
    public static final int LIKE = 2;
    public static final int REPOST = 3;
    public static final int MARKED_FOR_OFFLINE = 4;

    public static final Func1<EntityStateChangedEvent, Boolean> IS_TRACK_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isSingularChange() && event.getNextUrn().isTrack();
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
            return event.isSingularChange() && event.getNextUrn().isPlaylist() && event.getKind() == MARKED_FOR_OFFLINE;
        }
    };

    public static final Func1<EntityStateChangedEvent, Boolean> IS_TRACK_LIKED_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.isTrackLikeEvent() && event.getNextChangeSet().get(TrackProperty.IS_LIKED);
        }
    };

    public static final Func1<? super EntityStateChangedEvent,Boolean> IS_PLAYLIST_LIKED_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
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

    public static final Func1<EntityStateChangedEvent, Urn> TO_URN = new Func1<EntityStateChangedEvent, Urn>() {
        @Override
        public Urn call(EntityStateChangedEvent entityStateChangedEvent) {
            return entityStateChangedEvent.getNextUrn();
        }
    };

    private final int kind;
    private final Map<Urn, PropertySet> changeMap;

    public static EntityStateChangedEvent downloadStarted(Urn track) {
        return new EntityStateChangedEvent(DOWNLOAD,
                PropertySet.from(
                        TrackProperty.URN.bind(track),
                        TrackProperty.OFFLINE_DOWNLOADING.bind(true))
        );
    }

    public static EntityStateChangedEvent downloadFinished(Urn track) {
        return new EntityStateChangedEvent(DOWNLOAD,
                PropertySet.from(
                        TrackProperty.URN.bind(track),
                        TrackProperty.OFFLINE_DOWNLOADING.bind(false),
                        TrackProperty.OFFLINE_DOWNLOADED_AT.bind(new Date()))
        );
    }


    public static EntityStateChangedEvent downloadFinished(Collection<Urn> tracks) {
        Collection<PropertySet> changeSet = Collections2.transform(tracks, new Function<Urn, PropertySet>() {
            @Override
            public PropertySet apply(Urn urn) {
                return PropertySet.from(
                        TrackProperty.URN.bind(urn),
                        TrackProperty.OFFLINE_DOWNLOADING.bind(false),
                        TrackProperty.OFFLINE_DOWNLOADED_AT.bind(new Date()));
            }
        });

        return new EntityStateChangedEvent(DOWNLOAD, changeSet);
    }

    public static EntityStateChangedEvent downloadFailed(Urn track) {
        return new EntityStateChangedEvent(DOWNLOAD,
                PropertySet.from(
                        TrackProperty.URN.bind(track),
                        TrackProperty.OFFLINE_DOWNLOADING.bind(false),
                        TrackProperty.OFFLINE_UNAVAILABLE_AT.bind(new Date())
                )
        );
    }

    public static EntityStateChangedEvent downloadPending(Collection<Urn> tracks) {
        Collection<PropertySet> changeSet = Collections2.transform(tracks, new Function<Urn, PropertySet>() {
            @Override
            public PropertySet apply(Urn urn) {
                return PropertySet.from(
                        TrackProperty.URN.bind(urn),
                        TrackProperty.OFFLINE_DOWNLOADING.bind(false),
                        TrackProperty.OFFLINE_REQUESTED_AT.bind(new Date()));
            }
        });

        return new EntityStateChangedEvent(DOWNLOAD, changeSet);
    }

    public static EntityStateChangedEvent downloadRemoved(Collection<Urn> tracks) {
        Collection<PropertySet> changeSet = Collections2.transform(tracks, new Function<Urn, PropertySet>() {
            @Override
            public PropertySet apply(Urn urn) {
                return PropertySet.from(
                        TrackProperty.URN.bind(urn),
                        TrackProperty.OFFLINE_DOWNLOADING.bind(false),
                        TrackProperty.OFFLINE_REMOVED_AT.bind(new Date()));
            }
        });

        return new EntityStateChangedEvent(DOWNLOAD, changeSet);
    }

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

    public static EntityStateChangedEvent fromRepost(Urn urn, boolean reposted) {
        return fromRepost(PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlayableProperty.IS_REPOSTED.bind(reposted)));
    }

    public static EntityStateChangedEvent fromRepost(PropertySet newRepostState){
        return new EntityStateChangedEvent(REPOST, newRepostState);
    }

    public static EntityStateChangedEvent fromMarkedForOffline(Urn urn, boolean isMarkedForOffline) {
        return new EntityStateChangedEvent(MARKED_FOR_OFFLINE, PropertySet.from(
                PlayableProperty.URN.bind(urn),
                PlaylistProperty.IS_MARKED_FOR_OFFLINE.bind(isMarkedForOffline)));
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

    /**
     * @return for a single change event, this returns the single URN; if more than one entity changed,
     * returns the first available URN.
     */
    public Urn getNextUrn() {
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
        return isSingularChange() && getNextUrn().isTrack() && kind == LIKE;
    }

    public boolean isPlaylistLike() {
        return isSingularChange() && getNextUrn().isPlaylist() && kind == LIKE;
    }

}
