package com.soundcloud.android.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import com.soundcloud.android.model.ScResource;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Deprecated
public class SoundCloudDB {

    /**
     * Inserts a list of resources into the database
     * @param resolver
     * @param resources
     * @return
     */
    public static int bulkInsertResources(ContentResolver resolver, List<? extends ScResource> resources) {
        if (resources == null) return 0;
        BulkInsertMap map = new BulkInsertMap();
        for (ScResource resource : resources) {
            if (resource != null) resource.putFullContentValues(map);
        }
        return map.insert(resolver);
    }

    /**
     * Inserts a list of resources and their corresponding dependencies to a given URI.
     * Contains special handling based on Content requirements.
     * @param resolver
     * @param resources
     * @param collectionUri
     * @param ownerId
     * @return the number of insertsions
     */
    public static int insertCollection(ContentResolver resolver,
                                       @NotNull List<? extends ScResource> resources,
                                       @NotNull Uri collectionUri,
                                       long ownerId) {
        if (ownerId < 0) {
            throw new IllegalArgumentException("need valid ownerId for collection");
        }

        BulkInsertMap map = new BulkInsertMap();
        for (int i=0; i < resources.size(); i++) {
            ScResource r = resources.get(i);
            if (r != null) {
                r.putFullContentValues(map);
                long id = r.id;
                ContentValues contentValues = new ContentValues();
                switch (Content.match(collectionUri)) {
                    case PLAY_QUEUE:
                        contentValues.put(DBHelper.PlayQueue.POSITION, i);
                        contentValues.put(DBHelper.PlayQueue.TRACK_ID, id);
                        contentValues.put(DBHelper.CollectionItems.USER_ID, ownerId);
                        break;
                    case PLAYLIST_TRACKS:
                        contentValues.put(DBHelper.PlaylistTracks.POSITION, i);
                        contentValues.put(DBHelper.PlaylistTracks.TRACK_ID, id);
                        break;
                    default:
                        contentValues.put(DBHelper.CollectionItems.POSITION, i);
                        contentValues.put(DBHelper.CollectionItems.ITEM_ID, id);
                        contentValues.put(DBHelper.CollectionItems.USER_ID, ownerId);
                        break;
                }
                map.add(collectionUri,contentValues);
            }
        }
        return map.insert(resolver);
    }


}
