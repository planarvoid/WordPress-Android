package com.soundcloud.android.task.collection;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.service.sync.ApiSyncer.getMissingModelsById;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.LocalCollectionPage;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserHolder;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;

import android.content.ContentResolver;
import android.database.Cursor;
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

        boolean keepGoing = localData.idList.size() == params.maxToLoad;

        // if we already have all the data, this is a NOP
        try {
            SoundCloudDB.bulkInsertModels(resolver,
                    getMissingModelsById(api, resolver, localData.idList, params.getContent(), false));
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            keepGoing = false;
        }

        CollectionHolder<T> newItems = loadLocalContent(resolver, params);
        newItems.resolve(api.getContext());

        return new ReturnData<T>(newItems, params, null, -1, keepGoing, true);
    }

    private static class LocalData {
        LocalCollection localCollection;
        LocalCollectionPage localCollectionPage;
        List<Long> idList;

        public LocalData(ContentResolver resolver, CollectionParams mParams) {
            localCollection = LocalCollection.fromContentUri(mParams.contentUri, resolver, true);
            idList = Content.match(mParams.contentUri).getLocalIds(resolver,
                    SoundCloudApplication.getUserId(), mParams.startIndex, mParams.maxToLoad);
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

    @SuppressWarnings("unchecked")
    private CollectionHolder<T> loadLocalContent(ContentResolver resolver, CollectionParams<T> params) {
        Cursor itemsCursor = resolver.query(
                SoundCloudDB.addPagingParams(params.contentUri, params.startIndex, params.maxToLoad)
                , null, null, null, null);

        if (Track.class.equals(params.loadModel)) {
            return (CollectionHolder<T>) TrackHolder.fromCursor(itemsCursor);
        } else if (User.class.equals(params.loadModel)) {
            return(CollectionHolder<T>) UserHolder.fromCursor(itemsCursor);
        } else if (Activity.class.equals(params.loadModel)) {
            return (CollectionHolder<T>) Activities.fromCursor(itemsCursor);
        } else {
            throw new IllegalArgumentException("NOT HANDLED YET " + params.loadModel);
        }
    }
}
