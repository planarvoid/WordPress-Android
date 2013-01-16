package com.soundcloud.android.task.collection;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.provider.TypeIdList;

import android.content.ContentResolver;
import android.util.Log;

import java.io.IOException;

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
        TypeIdList typeIdsList = SoundCloudDB.getStoredTypeIds(resolver,params.contentUri, params.startIndex, params.maxToLoad);

        boolean keepGoing = typeIdsList.size() > 0;
        // if we already have all the data, this is a NOP
        try {
            SoundCloudApplication.MODEL_MANAGER.fetchMissingCollectionItems(api, typeIdsList, params.getContent(), false, -1);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            keepGoing = false;
        }

        CollectionHolder<T> newItems = SoundCloudApplication.MODEL_MANAGER.loadLocalContent(resolver, params.loadModel, params.getPagedUri());
        return new ReturnData<T>(newItems, params, null, keepGoing, true);
    }
}
