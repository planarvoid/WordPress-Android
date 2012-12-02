package com.soundcloud.android.task.collection;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.provider.ScContentProvider;
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

        List<Long> idList = SoundCloudDB.idCursorToList(resolver.query(
                SoundCloudDB.addPagingParams(params.contentUri, params.startIndex, params.maxToLoad)
                        .appendQueryParameter(ScContentProvider.Parameter.IDS_ONLY,"1").build(),
                null, null, null, null)
        );

        boolean keepGoing = idList.size() > 0;
        // if we already have all the data, this is a NOP
        try {
            SoundCloudApplication.MODEL_MANAGER.fetchMissingCollectionItems(api, idList, params.getContent(), false, -1);

        } catch (IOException e) {
            Log.e(TAG, "error", e);
            keepGoing = false;
        }

        CollectionHolder<T> newItems = SoundCloudApplication.MODEL_MANAGER.loadLocalContent(resolver, params.loadModel, params.getPagedUri());
        return new ReturnData<T>(newItems, params, null, -1, keepGoing, true);
    }
}
