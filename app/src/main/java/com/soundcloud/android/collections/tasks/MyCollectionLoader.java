package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.api.legacy.model.Friend;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.api.CloudAPI;
import org.apache.http.HttpStatus;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads collection from local storage. Model objects which are not in the database yet will
 * be resolved and stored automatically.
 * <p/>
 * This is used to potentially complete partially synced collections. If the collection is
 * fully cached in the database no remote lookups are performed.
 */
@Deprecated
public class MyCollectionLoader<T extends ScModel> implements CollectionLoader<T> {

    @Override
    public ReturnData<T> load(PublicCloudAPI api, CollectionParams<T> params) {
        final Context context = SoundCloudApplication.instance;
        final ContentResolver resolver = context.getContentResolver();
        boolean keepGoing = true;
        int responseCode = EmptyView.Status.OK;

        switch (params.getContent()){
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                // these don't sync with mini representations. we might only have ids
                List<Long> storedIds = new UserAssociationStorage(context).getStoredIds(params.getPagedUri());

                // if we already have all the data, this is a NOP
                try {
                    new CollectionStorage(resolver).fetchAndStoreMissingCollectionItems(api, storedIds, params.getContent(), false);
                } catch (CloudAPI.InvalidTokenException e) {
                    // TODO, move this once we centralize our error handling
                    // InvalidTokenException should expose the response code so we don't have to hardcode it here
                    responseCode = HttpStatus.SC_UNAUTHORIZED;
                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                    keepGoing = false;
                }
        }

        List<T> newItems = loadLocalContent(resolver,
                params.loadModel,
                params.getPagedUri());


        if (keepGoing) keepGoing = newItems.size() > 0;

        return new ReturnData<T>(newItems, params, null, responseCode, keepGoing, true);
    }

    // TODO: this is horrible, leftover from ScModelManager
    private static
    <T extends ScModel> List<T> loadLocalContent(ContentResolver resolver,
                                                 Class<T> resourceType,
                                                 Uri localUri)
    {
        Cursor itemsCursor = resolver.query(localUri, null, null, null, null);
        List<ScModel> items = new ArrayList<ScModel>();
        if (itemsCursor != null) {
            while (itemsCursor.moveToNext())
                if (PublicApiTrack.class.equals(resourceType)) {
                    items.add(SoundCloudApplication.sModelManager.getCachedTrackFromCursor(itemsCursor));
                } else if (PublicApiUser.class.equals(resourceType)) {
                    items.add(SoundCloudApplication.sModelManager.getCachedUserFromCursor(itemsCursor));
                } else if (Friend.class.equals(resourceType)) {
                    items.add(new Friend(SoundCloudApplication.sModelManager.getCachedUserFromCursor(itemsCursor)));
                } else if (SoundAssociation.class.equals(resourceType)) {
                    items.add(new SoundAssociation(itemsCursor));
                } else if (UserAssociation.class.equals(resourceType)) {
                    items.add(new UserAssociation(itemsCursor));
                } else if (PublicApiPlaylist.class.equals(resourceType)) {
                    items.add(SoundCloudApplication.sModelManager.getCachedPlaylistFromCursor(itemsCursor));
                } else {
                    throw new IllegalArgumentException("NOT HANDLED YET " + resourceType);
                }
        }
        if (itemsCursor != null) itemsCursor.close();
        //noinspection unchecked
        return (List<T>) items;
    }
}
