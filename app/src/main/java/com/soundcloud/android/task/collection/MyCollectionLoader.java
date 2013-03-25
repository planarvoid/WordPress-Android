package com.soundcloud.android.task.collection;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.dao.CollectionStorage;
import com.soundcloud.android.dao.UserAssociationStore;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.api.CloudAPI;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

/**
 * Loads collection from local storage. Model objects which are not in the database yet will
 * be resolved and stored automatically.
 * <p/>
 * This is used to potentially complete partially synced collections. If the collection is
 * fully cached in the database no remote lookups are performed.
 */
public class MyCollectionLoader<T extends ScModel> extends CollectionLoader<T> {

    @Override
    public ReturnData<T> load(AndroidCloudAPI api, CollectionParams<T> params) {
        final Context context = api.getContext();
        ContentResolver resolver = context.getContentResolver();
        boolean keepGoing = true;
        int responseCode = EmptyListView.Status.OK;

        switch (params.getContent()){
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                // these don't sync with mini representations. we might only have ids
                List<Long> storedIds = new UserAssociationStore(context).getStoredIds(params.getPagedUri());

                // if we already have all the data, this is a NOP
                try {
                    new CollectionStorage(context).fetchAndStoreMissingCollectionItems(api, storedIds, params.getContent(), false);
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
                if (Track.class.equals(resourceType)) {
                    items.add(new Track(itemsCursor));
                } else if (User.class.equals(resourceType)) {
                    items.add(new User(itemsCursor));
                } else if (Friend.class.equals(resourceType)) {
                    items.add(new Friend(new User(itemsCursor)));
                } else if (SoundAssociation.class.equals(resourceType)) {
                    items.add(new SoundAssociation(itemsCursor));
                } else if (Playlist.class.equals(resourceType)) {
                    items.add(new Playlist(itemsCursor));
                } else {
                    throw new IllegalArgumentException("NOT HANDLED YET " + resourceType);
                }
        }
        if (itemsCursor != null) itemsCursor.close();
        //noinspection unchecked
        return (List<T>) items;
    }
}
