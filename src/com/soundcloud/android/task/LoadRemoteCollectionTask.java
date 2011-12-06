package com.soundcloud.android.task;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class LoadRemoteCollectionTask extends LoadCollectionTask {

    protected String mNextHref;
    public Request mRequest;

    public LoadRemoteCollectionTask(SoundCloudApplication app, Class<?> loadModel, Uri contentUri, int pageIndex, boolean refresh, Request request) {
        super(app, loadModel, contentUri, pageIndex, refresh);
        mRequest = request;
    }

    @Override
    protected Boolean doInBackground(String... params) {

        Cursor c = null;
        LocalCollection localCollection = null;
        LocalCollectionPage localCollectionPage = null;

        if (mContentUri != null){
            localCollection = getLocalCollection(mContentUri);
            if (localCollection == null) {
                localCollection = insertLocalCollection();
            } else {
                localCollectionPage = getLocalCollectionPage(localCollection.id,mPageIndex);
                if (localCollectionPage != null) {
                    Log.i("asdf","applying etag");
                    localCollectionPage.applyEtag(mRequest);
                }
            }
        }

        // fetch if there is no local uri, no stored colleciton for this page, or this is a refresh
        if (mContentUri == null || localCollectionPage == null || mRefresh) {
            Log.i("asdf","Trying remote load");
            try {
                HttpResponse resp = mApp.get(mRequest);
                mResponseCode = resp.getStatusLine().getStatusCode();
                Log.i("asdf","Got response code " + mResponseCode);
                if (mResponseCode != HttpStatus.SC_OK && mResponseCode != HttpStatus.SC_NOT_MODIFIED) {
                    throw new IOException("Invalid response: " + resp.getStatusLine());
                }

                if (mResponseCode == HttpStatus.SC_OK) {

                    // we have new content. wipe out everything for now (or it gets tricky)
                    if (localCollection != null) {
                        mApp.getContentResolver().delete(ScContentProvider.Content.RESOURCE_PAGES,
                                DBHelper.ResourcePages.RESOURCE_ID + " = ? AND " + DBHelper.ResourcePages.PAGE_INDEX + " > ?",
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
                        cv.put(DBHelper.ResourcePages.RESOURCE_ID, localCollection.id);
                        cv.put(DBHelper.ResourcePages.PAGE_INDEX, mPageIndex);
                        cv.put(DBHelper.ResourcePages.ETAG, Http.etag(resp));
                        cv.put(DBHelper.ResourcePages.SIZE, mNewItems.size());
                        if (mNextHref != null) {
                            cv.put(DBHelper.ResourcePages.NEXT_HREF, mNextHref);
                        }

                        // insert new page
                        mApp.getContentResolver().insert(ScContentProvider.Content.RESOURCE_PAGES, cv);
                        SoundCloudDB.bulkInsertParcelables(mApp,mNewItems,mContentUri,getCollectionOwner(),
                                mPageIndex * Consts.COLLECTION_PAGE_SIZE);
                    }
                    return true;
                }
            } catch (IOException e) {
                Log.e(TAG, "error", e);
            }
        }

        Log.i("asdf","Load from Local");
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

    private LocalCollection insertLocalCollection() {
        // insert if not there
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Resources.URI, mContentUri.toString());
        Uri inserted = mApp.getContentResolver().insert(ScContentProvider.Content.RESOURCES, cv);
        if (inserted != null) {
            return new LocalCollection(inserted.getLastPathSegment(),mContentUri);
        } else {
            return null;
        }
    }

    private LocalCollection getLocalCollection(Uri contentUri) {
        LocalCollection lc = null;
        Cursor c = mApp.getContentResolver().query(ScContentProvider.Content.RESOURCES, null, "uri = ?", new String[]{contentUri.toString()}, null);
        if (c != null && c.moveToFirst()) {
            lc = new LocalCollection(c);
        }
        if (c != null) c.close();
        return lc;
    }

    private LocalCollectionPage getLocalCollectionPage(int collectionId, int pageIndex) {
        LocalCollectionPage lcp = null;
        Cursor c = mApp.getContentResolver().query(ScContentProvider.Content.RESOURCE_PAGES, null,
                DBHelper.ResourcePages.RESOURCE_ID + " = ? AND " + DBHelper.ResourcePages.PAGE_INDEX + " = ?",
                new String[]{String.valueOf(collectionId), String.valueOf(pageIndex)}, null);

        if (c != null && c.moveToFirst()) {
            lcp = new LocalCollectionPage(c);
        }
        if (c != null) c.close();
        return lcp;
    }

    private class LocalCollection {
        int id;
        Uri uri;
        long last_refresh;

         public LocalCollection(Cursor c){
             id = c.getInt(c.getColumnIndex(DBHelper.Resources.ID));
             uri = Uri.parse(c.getString(c.getColumnIndex(DBHelper.Resources.URI)));
             last_refresh = c.getLong(c.getColumnIndex(DBHelper.Resources.URI));
         }
        public LocalCollection(String id, Uri uri){
             this.id = Integer.parseInt(id);
             this.uri = uri;
         }
    }

    private class LocalCollectionPage {
        int id;
        int size;
        String nextHref;
        String etag;

        public LocalCollectionPage(Cursor c){
            id = c.getInt(c.getColumnIndex(DBHelper.ResourcePages.ID));
            size = c.getInt(c.getColumnIndex(DBHelper.ResourcePages.SIZE));
            nextHref = c.getString(c.getColumnIndex(DBHelper.ResourcePages.NEXT_HREF));
            etag = c.getString(c.getColumnIndex(DBHelper.ResourcePages.ETAG));
        }

        public void applyEtag(Request request) {
            if (!TextUtils.isEmpty(etag)) request.ifNoneMatch(etag);
        }
    }


}
