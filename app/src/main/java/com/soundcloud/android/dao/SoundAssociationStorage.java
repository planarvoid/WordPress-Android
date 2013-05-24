package com.soundcloud.android.dao;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

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
public class SoundAssociationStorage {

    private final ContentResolver mResolver;
    private final SoundAssociationDAO mAllSoundAssocsDAO, mLikesDAO, mRepostsDAO, mTrackCreationsDAO, mPlaylistCreationsDAO;

    public SoundAssociationStorage() {
        mResolver = SoundCloudApplication.instance.getContentResolver();
        mAllSoundAssocsDAO = new SoundAssociationDAO(mResolver);
        mLikesDAO = SoundAssociationDAO.forContent(Content.ME_LIKES, mResolver);
        mRepostsDAO = SoundAssociationDAO.forContent(Content.ME_REPOSTS, mResolver);
        mTrackCreationsDAO = SoundAssociationDAO.forContent(Content.ME_SOUNDS, mResolver);
        mPlaylistCreationsDAO = SoundAssociationDAO.forContent(Content.ME_PLAYLISTS, mResolver);
    }

    /**
     * Persists user-likes-this information to the database. This method expects that the given instance already has
     * the up-to-date likes count set, and will in return ensure that both a {@link SoundAssociation} record will be
     * created, as well as the new likes count being updated on the {@link com.soundcloud.android.provider.DBHelper.Sounds}
     * table.
     */
    public SoundAssociation addLike(Playable playable) {
        SoundAssociation.Type assocType = (playable instanceof Track) ? SoundAssociation.Type.TRACK_LIKE : SoundAssociation.Type.PLAYLIST_LIKE;
        SoundAssociation like = new SoundAssociation(playable, new Date(), assocType);
        mLikesDAO.create(like);
        return like;
    }

    /**
     * Persists user-unlikes-this information to the database. This method expects that the given instance already has
     * the up-to-date likes count set, and will in return ensure that both the {@link SoundAssociation} record will be
     * removed, as well as the new likes count being updated on the {@link com.soundcloud.android.provider.DBHelper.Sounds}
     * table.
     */
    public SoundAssociation removeLike(Playable playable) {
        SoundAssociation.Type assocType = (playable instanceof Track) ? SoundAssociation.Type.TRACK_LIKE : SoundAssociation.Type.PLAYLIST_LIKE;
        SoundAssociation like = new SoundAssociation(playable, new Date(), assocType);
        mLikesDAO.delete(like);
        if (playable instanceof Track) {
            new TrackDAO(mResolver).update((Track) playable);
        } else {
            new PlaylistDAO(mResolver).update((Playlist) playable);
        }
        return like;
    }

    /**
     * Persists user-reposted-this information to the database. This method expects that the given instance already has
     * the up-to-date reposts count set, and will in return ensure that both a {@link SoundAssociation} record will be
     * created, as well as the new reposts count being updated on the {@link com.soundcloud.android.provider.DBHelper.Sounds}
     * table.
     */
    public SoundAssociation addRepost(Playable playable) {
        SoundAssociation.Type assocType = (playable instanceof Track) ? SoundAssociation.Type.TRACK_REPOST : SoundAssociation.Type.PLAYLIST_REPOST;
        SoundAssociation repost = new SoundAssociation(playable, new Date(), assocType);
        mRepostsDAO.create(repost);
        return repost;
    }

    /**
     * Persists user-unreposted-this information to the database. This method expects that the given instance already has
     * the up-to-date reposts count set, and will in return ensure that both the {@link SoundAssociation} record will be
     * removed, as well as the new reposts count being updated on the {@link com.soundcloud.android.provider.DBHelper.Sounds}
     * table.
     */
    public SoundAssociation removeRepost(Playable playable) {
        SoundAssociation.Type assocType = (playable instanceof Track) ? SoundAssociation.Type.TRACK_REPOST : SoundAssociation.Type.PLAYLIST_REPOST;
        SoundAssociation repost = new SoundAssociation(playable, new Date(), assocType);
        mRepostsDAO.delete(repost);
        return repost;
    }

    public SoundAssociation addCreation(Track playable) {
        SoundAssociation.Type assocType = SoundAssociation.Type.TRACK;
        SoundAssociation creation = new SoundAssociation(playable, playable.created_at, assocType);
        mTrackCreationsDAO.create(creation);
        return creation;
    }

    public SoundAssociation addCreation(Playlist playable) {
        SoundAssociation.Type assocType = SoundAssociation.Type.PLAYLIST;
        SoundAssociation creation = new SoundAssociation(playable, playable.created_at, assocType);
        mPlaylistCreationsDAO.create(creation);
        return creation;
    }

    public List<SoundAssociation> getSoundStreamItemsForCurrentUser() {
        return mAllSoundAssocsDAO.queryAllByUri(Content.ME_SOUNDS.uri);
    }

    public List<SoundAssociation> getLikesForCurrentUser() {
        return mAllSoundAssocsDAO.queryAllByUri(Content.ME_LIKES.uri);
    }

    public List<SoundAssociation> getPlaylistCreationsForCurrentUser() {
        return mAllSoundAssocsDAO.queryAllByUri(Content.ME_PLAYLISTS.uri);
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
        Cursor c = mResolver.query(contentUri,
                new String[]{DBHelper.SoundAssociationView._ID, DBHelper.SoundAssociationView._TYPE,
                        DBHelper.SoundAssociationView.SOUND_ASSOCIATION_TYPE}, null, null, null);

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
                    if (a.getPlayable().id == id && a.getResourceType() == resourceType
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
                        mResolver.delete(contentUri,
                                DBHelper.CollectionItems.ITEM_ID + " = ? AND " + DBHelper.CollectionItems.RESOURCE_TYPE + " = ?",
                                new String[]{String.valueOf(id), String.valueOf(type)});
                    }
                }
            }
        }

        mAllSoundAssocsDAO.createCollection(soundAssociations);

        return changed;
    }
}
