package com.soundcloud.android.task.collection;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.LocalCollectionPage;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.provider.Content;

import android.content.ContentResolver;
import android.util.Log;

import java.io.IOException;
import java.util.List;

/**
 * Loads collection from local storage. Model objects which are not in the database yet will
 * be resolved and stored automatically.
 *
 * This is used to potentially complete partially synced collections. If the collection is
 * fully cached in the database no remote lookups are performed.
 */
public class MyCollectionLoader<T extends ScModel> extends CollectionLoader<T> {

    @Override
    public ReturnData<T> load(AndroidCloudAPI api, CollectionParams<T> params) {

        ContentResolver resolver = api.getContext().getContentResolver();
        LocalData localData = new LocalData(resolver, params);

        boolean keepGoing = localData.idList.size() > 0;

        // if we already have all the data, this is a NOP
        try {
            SoundCloudApplication.MODEL_MANAGER.writeMissingCollectionItems(api, localData.idList, params.getContent(), false);

        } catch (IOException e) {
            Log.e(TAG, "error", e);
            keepGoing = false;
        }

        CollectionHolder<T> newItems = SoundCloudApplication.MODEL_MANAGER.loadLocalContent(resolver, params.loadModel, params.getPagedUri());
        return new ReturnData<T>(newItems, params, null, -1, keepGoing, true);
    }

    private static class LocalData {
        LocalCollection localCollection;
        LocalCollectionPage localCollectionPage;
        List<Long> idList;

        public LocalData(ContentResolver resolver, CollectionParams mParams) {
            localCollection = LocalCollection.fromContentUri(mParams.contentUri, resolver, true);
            idList = SoundCloudApplication.MODEL_MANAGER.getLocalIds(mParams.getContent(), SoundCloudApplication.getUserId(),
                    mParams.startIndex, mParams.maxToLoad);
        }

        @Override
        public String toString() {
            return "LocalData{" +
                    "localCollection=" + localCollection +
                    ", localCollectionPage=" + localCollectionPage +
                    ", idList=" + idList +
                    '}';
        }
    }


}
