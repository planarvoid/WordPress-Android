package com.soundcloud.android.task;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.LocalCollectionPage;
import com.soundcloud.android.model.ModelBase;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;

import static com.soundcloud.android.SoundCloudApplication.CONNECTIVITY_SERVICE;
import static com.soundcloud.android.SoundCloudApplication.TAG;

public class LoadRemoteCollectionTask extends LoadCollectionTask {

    protected String mNextHref;
    private long mLastRefresh;
    public Request mRequest;


    public LoadRemoteCollectionTask(SoundCloudApplication app, Class<?> loadModel, Uri contentUri, int pageIndex, boolean refresh, Request request) {
        super(app, loadModel, contentUri, pageIndex, refresh);
        mRequest = request;
    }

    public void setLastRefresh(long lastRefresh) {
        mLastRefresh = lastRefresh;
    }

    @Override
    protected Boolean doInBackground(String... params) {

        Cursor c = null;
        LocalCollection localCollection = null;
        LocalCollectionPage localCollectionPage = null;

        if (mContentUri != null){
            localCollection = com.soundcloud.android.model.LocalCollection.fromContentUri(mApp.getContentResolver(), mContentUri);
            if (localCollection == null) {
                localCollection = com.soundcloud.android.model.LocalCollection.insertLocalCollection(mApp.getContentResolver(), mContentUri);
            } else {
                localCollectionPage = LocalCollectionPage.fromCollectionAndIndex(mApp.getContentResolver(), localCollection.id, mPageIndex);
                if (localCollectionPage != null) {
                    localCollectionPage.applyEtag(mRequest);
                }
            }
        }

        // fetch if there is no local uri, no stored colleciton for this page,
        if (mContentUri == null || localCollectionPage == null ||

                (mRefresh && System.currentTimeMillis() - mLastRefresh > Consts.DEFAULT_REFRESH_MINIMUM)) {
            try {
                HttpResponse resp = mApp.get(mRequest);
                mResponseCode = resp.getStatusLine().getStatusCode();
                if (mResponseCode != HttpStatus.SC_OK && mResponseCode != HttpStatus.SC_NOT_MODIFIED) {
                    throw new IOException("Invalid response: " + resp.getStatusLine());
                }

                if (mResponseCode == HttpStatus.SC_OK) {

                    // we have new content. wipe out everything for now (or it gets tricky)
                    if (localCollection != null) {
                        mApp.getContentResolver().delete(ScContentProvider.Content.COLLECTION_PAGES,
                                DBHelper.CollectionPages.COLLECTION_ID + " = ? AND " + DBHelper.CollectionPages.PAGE_INDEX + " > ?",
                                new String[]{String.valueOf(localCollection.id), String.valueOf(mPageIndex)});
                    }

                    // process new items and publish them
                    CollectionHolder holder = getCollection(resp.getEntity().getContent(), mNewItems);
                    mNextHref = holder == null || TextUtils.isEmpty(holder.next_href) ? null : holder.next_href;
                    keepGoing = !TextUtils.isEmpty(mNextHref);
                    for (Parcelable p : mNewItems) {
                        ((ModelBase) p).resolve(mApp);
                    }
                    // publish what we have, commit new items in the background, no need to wait
                    publishProgress(mNewItems);

                    // store items if we have items and a content uri
                    if (mContentUri != null && mNewItems != null) {

                        ContentValues cv = new ContentValues();
                        cv.put(DBHelper.CollectionPages.COLLECTION_ID, localCollection.id);
                        cv.put(DBHelper.CollectionPages.PAGE_INDEX, mPageIndex);
                        cv.put(DBHelper.CollectionPages.ETAG, Http.etag(resp));
                        cv.put(DBHelper.CollectionPages.SIZE, mNewItems.size());
                        if (mNextHref != null) {
                            cv.put(DBHelper.CollectionPages.NEXT_HREF, mNextHref);
                        }

                        // insert new page
                        mApp.getContentResolver().insert(ScContentProvider.Content.COLLECTION_PAGES, cv);
                        SoundCloudDB.bulkInsertParcelables(mApp,mNewItems,mContentUri,getCollectionOwner(),
                                mPageIndex * Consts.COLLECTION_PAGE_SIZE);
                    }
                    return true;
                }
            } catch (IOException e) {
                Log.e(TAG, "error", e);
            }
        }

       keepGoing = !TextUtils.isEmpty(mNextHref);

       // if we get this far, we either failed, or our etags matched up, so see if we can load locally in super.doInBg.
       return super.doInBackground(params);
    }

    private long getCollectionOwner(){
        final int uriCode = mApp.getContentUriMatcher().match(mContentUri);
        if (uriCode < 200){ // mine
            return mApp.getCurrentUserId();
        } else if (mContentUri.getPathSegments().size() > 2){
            return Long.parseLong(mContentUri.getPathSegments().get(1));
        } else{
            return -1;
        }
    }


}
