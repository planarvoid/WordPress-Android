package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.provider.Content;
import rx.Observable;
import rx.Subscriber;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use this storage facade to persist information about user-to-sound relations to the database. These relations
 * currently are: likes, reposts, track creations and playlist creations.
 *
 * @see SoundAssociation.Type
 */
public class SoundAssociationStorage extends ScheduledOperations {

    private final ContentResolver resolver;
    private final SoundAssociationDAO allSoundAssocsDAO, likesDAO, repostsDAO, trackCreationsDAO, playlistCreationsDAO;

    @Inject
    public SoundAssociationStorage() {
        super(ScSchedulers.STORAGE_SCHEDULER);
        resolver = SoundCloudApplication.instance.getContentResolver();
        allSoundAssocsDAO = new SoundAssociationDAO(resolver);
        likesDAO = SoundAssociationDAO.forContent(Content.ME_LIKES, resolver);
        repostsDAO = SoundAssociationDAO.forContent(Content.ME_REPOSTS, resolver);
        trackCreationsDAO = SoundAssociationDAO.forContent(Content.ME_SOUNDS, resolver);
        playlistCreationsDAO = SoundAssociationDAO.forContent(Content.ME_PLAYLISTS, resolver);
    }

    /**
     * Persists user-likes-this information to the database. This methods ensure that both a {@link SoundAssociation}
     * record will be created, as well as the likes counter cache on the playable to be updated and persisted.
     */
    public SoundAssociation addLike(Playable playable) {
        playable.user_like = true;
        playable.likes_count = Math.max(1, playable.likes_count + 1);
        SoundAssociation.Type assocType = (playable instanceof PublicApiTrack) ? SoundAssociation.Type.TRACK_LIKE : SoundAssociation.Type.PLAYLIST_LIKE;
        SoundAssociation like = new SoundAssociation(playable, new Date(), assocType);
        likesDAO.create(like);
        return like;
    }

    public Observable<SoundAssociation> addLikeAsync(final Playable playable) {
        return schedule(Observable.create(new Observable.OnSubscribe<SoundAssociation>() {
            @Override
            public void call(Subscriber<? super SoundAssociation> observer) {
                observer.onNext(addLike(playable));
                observer.onCompleted();
            }
        }));
    }

    /**
     * Persists user-unlikes-this information to the database. This methods ensure that both the {@link SoundAssociation}
     * record will be removed, as well as the likes counter cache on the playable to be updated and persisted.
     */
    public SoundAssociation removeLike(Playable playable) {
        playable.user_like = false;
        playable.likes_count = Math.max(0, playable.likes_count - 1);
        SoundAssociation.Type assocType = (playable instanceof PublicApiTrack) ? SoundAssociation.Type.TRACK_LIKE : SoundAssociation.Type.PLAYLIST_LIKE;
        SoundAssociation like = new SoundAssociation(playable, new Date(), assocType);
        likesDAO.delete(like);
        updatePlayable(playable);
        return like;
    }

    public Observable<SoundAssociation> removeLikeAsync(final Playable playable) {
        return schedule(Observable.create(new Observable.OnSubscribe<SoundAssociation>() {
            @Override
            public void call(Subscriber<? super SoundAssociation> observer) {
                observer.onNext(removeLike(playable));
                observer.onCompleted();
            }
        }));
    }

    /**
     * Persists user-reposted-this information to the database. This methods ensure that both a {@link SoundAssociation}
     * record will be created, as well as the reposts counter cache on the playable to be updated and persisted.
     */
    public SoundAssociation addRepost(Playable playable) {
        playable.user_repost = true;
        playable.reposts_count = Math.max(1, playable.reposts_count + 1);
        SoundAssociation.Type assocType = (playable instanceof PublicApiTrack) ? SoundAssociation.Type.TRACK_REPOST : SoundAssociation.Type.PLAYLIST_REPOST;
        SoundAssociation repost = new SoundAssociation(playable, new Date(), assocType);
        repostsDAO.create(repost);
        return repost;
    }

    public Observable<SoundAssociation> addRepostAsync(final Playable playable) {
        return schedule(Observable.create(new Observable.OnSubscribe<SoundAssociation>() {
            @Override
            public void call(Subscriber<? super SoundAssociation> observer) {
                observer.onNext(addRepost(playable));
                observer.onCompleted();
            }
        }));
    }

    /**
     * Persists user-unreposted-this information to the database. This methods ensure that both the {@link SoundAssociation}
     * record will be removed, as well as the reposts counter cache on the playable to be updated and persisted.
     */
    public SoundAssociation removeRepost(Playable playable) {
        playable.user_repost = false;
        playable.reposts_count = Math.max(0, playable.reposts_count - 1);
        SoundAssociation.Type assocType = (playable instanceof PublicApiTrack) ? SoundAssociation.Type.TRACK_REPOST : SoundAssociation.Type.PLAYLIST_REPOST;
        SoundAssociation repost = new SoundAssociation(playable, new Date(), assocType);
        repostsDAO.delete(repost);
        updatePlayable(playable);
        return repost;
    }

    public Observable<SoundAssociation> removeRepostAsync(final Playable playable) {
        return schedule(Observable.create(new Observable.OnSubscribe<SoundAssociation>() {
            @Override
            public void call(Subscriber<? super SoundAssociation> observer) {
                observer.onNext(removeRepost(playable));
                observer.onCompleted();
            }
        }));
    }

    private void updatePlayable(Playable playable) {
        if (playable instanceof PublicApiTrack) {
            new TrackDAO(resolver).update((PublicApiTrack) playable);
        } else {
            new PlaylistDAO(resolver).update((PublicApiPlaylist) playable);
        }
    }

    public SoundAssociation addCreation(final PublicApiTrack track) {
        return addCreation(track, trackCreationsDAO, SoundAssociation.Type.TRACK);
    }

    public SoundAssociation addCreation(final PublicApiPlaylist playlist) {
        return addCreation(playlist, playlistCreationsDAO, SoundAssociation.Type.PLAYLIST);
    }

    private SoundAssociation addCreation(Playable playable, BaseDAO<SoundAssociation> dao, SoundAssociation.Type assocType) {
        playable.created_at = new Date();
        SoundAssociation creation = new SoundAssociation(playable, playable.created_at, assocType);
        dao.create(creation);
        return creation;
    }

    public Observable<SoundAssociation> addCreationAsync(final PublicApiPlaylist playlist) {
        return addCreationAsync(playlist, playlistCreationsDAO, SoundAssociation.Type.PLAYLIST);
    }

    private Observable<SoundAssociation> addCreationAsync(final Playable playable, final BaseDAO<SoundAssociation> dao, final SoundAssociation.Type assocType) {
        return schedule(Observable.create(new Observable.OnSubscribe<SoundAssociation>() {
            @Override
            public void call(Subscriber<? super SoundAssociation> observer) {
                observer.onNext(addCreation(playable, dao, assocType));
                observer.onCompleted();
            }

        }));
    }

    public List<SoundAssociation> getSoundStreamItemsForCurrentUser() {
        return allSoundAssocsDAO.queryAllByUri(Content.ME_SOUNDS.uri);
    }

    public List<SoundAssociation> getLikesForCurrentUser() {
        return allSoundAssocsDAO.queryAllByUri(Content.ME_LIKES.uri);
    }

    public List<SoundAssociation> getPlaylistCreationsForCurrentUser() {
        return allSoundAssocsDAO.queryAllByUri(Content.ME_PLAYLISTS.uri);
    }

    public List<Long> getTrackLikesAsIds() {
        return likesDAO.buildQuery()
                .select(TableColumns.SoundAssociationView._ID)
                .where(TableColumns.SoundAssociationView._TYPE + " = ?", String.valueOf(PublicApiTrack.DB_TYPE_TRACK))
                .queryIds();
    }

    public Observable<List<Long>> getTrackLikesAsIdsAsync(){
        return schedule(Observable.create(new Observable.OnSubscribe<List<Long>>() {
            @Override
            public void call(Subscriber<? super List<Long>> observer) {
                observer.onNext(getTrackLikesAsIds());
                observer.onCompleted();
            }
        }));
    }

    /**
     * Sync this collection to the local database by removing any stale items and
     * inserting the sound associations (which will replace the existing items)
     * @param soundAssociations
     * @param contentUri
     * @return whether any items were added or removed
     */
    @Deprecated
    public boolean syncToLocal(List<SoundAssociation> soundAssociations, Uri contentUri) {
        // get current local id and types for this uri
        Cursor c = resolver.query(contentUri,
                new String[]{TableColumns.SoundAssociationView._ID, TableColumns.SoundAssociationView._TYPE,
                        TableColumns.SoundAssociationView.SOUND_ASSOCIATION_TYPE}, null, null, null);

        boolean changed = true; // assume changed by default
        if (c != null) {
            final int localCount = c.getCount();
            Map<Integer, ArrayList<Long>> deletions = new HashMap<Integer, ArrayList<Long>>();
            while (c.moveToNext()) {
                boolean found = false;
                final long id = c.getLong(0);
                final int resourceType = c.getInt(1);
                final int associationType = c.getInt(2);

                for (SoundAssociation a : soundAssociations) {
                    if (a.getPlayable().getId() == id && a.getResourceType() == resourceType
                            && a.associationType == associationType) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // this item no longer exists
                    if (!deletions.containsKey(resourceType)) {
                        deletions.put(resourceType, new ArrayList<Long>());
                    }
                    deletions.get(resourceType).add(id);
                }
            }
            c.close();

            if (deletions.isEmpty()) {
                // user hasn't removed anything, and if size is consistent we can assume the collection hasn't changed
                changed = localCount != soundAssociations.size();
            } else {
                for (Integer type : deletions.keySet()) {
                    for (Long id : deletions.get(type)) {
                        resolver.delete(contentUri,
                                TableColumns.CollectionItems.ITEM_ID + " = ? AND " + TableColumns.CollectionItems.RESOURCE_TYPE + " = ?",
                                new String[]{String.valueOf(id), String.valueOf(type)});
                    }
                }
            }
        }

        allSoundAssocsDAO.createCollection(soundAssociations);

        return changed;
    }
}
