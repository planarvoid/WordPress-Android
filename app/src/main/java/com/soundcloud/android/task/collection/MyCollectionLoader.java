package com.soundcloud.android.task.collection;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.CollectionStorage;
import com.soundcloud.android.dao.UserAssociationStore;
import com.soundcloud.android.model.ScModel;

import android.content.ContentResolver;
import android.util.Log;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.api.CloudAPI;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.List;

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
        ContentResolver resolver = api.getContext().getContentResolver();
        boolean keepGoing = true;
        int responseCode = EmptyListView.Status.OK;

        switch (params.getContent()){
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                // these don't sync with mini representations. we might only have ids
                List<Long> storedIds = new UserAssociationStore(resolver).getStoredIds(params.getPagedUri());

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

        List<T> newItems = SoundCloudApplication.MODEL_MANAGER.loadLocalContent(resolver,
                params.loadModel,
                params.getPagedUri());


        if (keepGoing) keepGoing = newItems.size() > 0;

        return new ReturnData<T>(newItems, params, null, responseCode, keepGoing, true);
    }
}
