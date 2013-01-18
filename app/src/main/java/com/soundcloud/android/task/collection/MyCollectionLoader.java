package com.soundcloud.android.task.collection;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.provider.SoundCloudDB;

import android.content.ContentResolver;
import android.util.Log;

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
        switch (params.getContent()){
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                // these don't sync with mini representations. we might only have ids
                List<Long> storedIds = SoundCloudDB.getStoredIds(resolver, params.contentUri, params.startIndex, params.maxToLoad);
                // if we already have all the data, this is a NOP
                try {
                    SoundCloudApplication.MODEL_MANAGER.fetchMissingCollectionItems(api, storedIds, params.getContent(), false, -1);
                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                    keepGoing = false;
                }
        }
        CollectionHolder<T> newItems = SoundCloudApplication.MODEL_MANAGER.loadLocalContent(resolver, params.loadModel, params.getPagedUri());
        if (keepGoing) keepGoing = newItems.size() > 0;
        return new ReturnData<T>(newItems, params, null, keepGoing, true);
    }
}
