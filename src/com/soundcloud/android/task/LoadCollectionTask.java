package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A background task that will be run when there is a need to append more
 * data. Mostly, this code delegates to the subclass, to append the data in
 * the background thread and rebind the pending view once that is done.
 */
public class LoadCollectionTask extends AsyncTask<String, List<? super Parcelable>, Boolean> {
    private SoundCloudApplication mApp;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;
    /* package */ List<Parcelable> mNewItems = new ArrayList<Parcelable>();

    protected String mNextHref;
    protected int mResponseCode;

    public Class<?> loadModel;

    public Uri contentUri;
    public int pageIndex;


    public Request request;
    public boolean refresh;
    public boolean keepGoing;

    public int pageSize;

    public LoadCollectionTask(SoundCloudApplication app, Class<?> loadModel) {
        mApp = app;
        this.loadModel = loadModel;
    }

    /**
     * Set the activity and adapter that this task now belong to. This will
     * be set as new context is destroyed and created in response to
     * orientation changes
     */
    public void setAdapter(LazyEndlessAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
        if (lazyEndlessAdapter != null) {
            loadModel = lazyEndlessAdapter.getLoadModel(false);
        }
    }

    @Override
    protected void onProgressUpdate(List<? super Parcelable>... values) {

    }

    @Override
    protected Boolean doInBackground(String... params) {

        int localPageSize, localResourceId = -1, localPageId = -1;
        String localPageEtag = "";
        Cursor c = null;

        if (contentUri != null){
            // get the current Uri data
            c = mApp.getContentResolver().query(ScContentProvider.Content.RESOURCES, null, "uri = ?", new String[]{contentUri.toString()}, null);
            if (c != null && c.moveToFirst()) {
                localResourceId = c.getInt(c.getColumnIndex(DBHelper.Resources.ID));
            }
            if (c != null) c.close();

            if (localResourceId == -1) {
                // insert if not there
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.Resources.URI, contentUri.toString());
                Uri inserted = mApp.getContentResolver().insert(ScContentProvider.Content.RESOURCES, cv);
                if (inserted != null) localResourceId = Integer.parseInt(inserted.getLastPathSegment());
            } else {

                // get the entry for the requested page
                c = mApp.getContentResolver().query(ScContentProvider.Content.RESOURCE_PAGES, null,
                        DBHelper.ResourcePages.RESOURCE_ID + " = ? AND " + DBHelper.ResourcePages.PAGE_INDEX + " = ?",
                        new String[]{String.valueOf(localResourceId), String.valueOf(pageIndex)}, null);


                if (c != null && c.moveToFirst()) {
                    localPageEtag = c.getString(c.getColumnIndex(DBHelper.ResourcePages.ETAG));
                    localPageId = c.getInt(c.getColumnIndex(DBHelper.ResourcePages.ID));
                    localPageSize = c.getInt(c.getColumnIndex(DBHelper.ResourcePages.SIZE));
                    mNextHref = c.getString(c.getColumnIndex(DBHelper.ResourcePages.NEXT_HREF));
                }
            }
        }

        boolean remoteLoad = (contentUri == null || localPageId == -1);
        if (remoteLoad || refresh) {
            Log.i("asdf","SETTING ETAG TO " + localPageEtag);
            if (!TextUtils.isEmpty(localPageEtag)) request.ifNoneMatch(localPageEtag);
            try {
                HttpResponse resp = mApp.get(request);
                mResponseCode = resp.getStatusLine().getStatusCode();
                if (mResponseCode != HttpStatus.SC_OK && mResponseCode != HttpStatus.SC_NOT_MODIFIED) {
                    throw new IOException("Invalid response: " + resp.getStatusLine());
                }
                if (mResponseCode == HttpStatus.SC_OK) {

                    // we have new content. wipe out everything for now (or it gets tricky)
                    mApp.getContentResolver().delete(ScContentProvider.Content.RESOURCE_PAGES,
                            DBHelper.ResourcePages.RESOURCE_ID + " = ? AND " + DBHelper.ResourcePages.PAGE_INDEX + " > ?",
                            new String[]{String.valueOf(localResourceId), String.valueOf(pageIndex)});

                    InputStream is = resp.getEntity().getContent();

                    CollectionHolder holder = getCollection(is, mNewItems);
                    mNextHref = holder == null || TextUtils.isEmpty(holder.next_href) ? null : holder.next_href;
                    keepGoing = !TextUtils.isEmpty(mNextHref);

                    for (Parcelable p : mNewItems) {
                        ((ModelBase) p).resolve(mApp);
                    }
                    publishProgress(mNewItems);


                    // store items if we have items and a content uri
                    if (contentUri != null && mNewItems != null) {

                        ContentValues cv = new ContentValues();
                        cv.put(DBHelper.ResourcePages.RESOURCE_ID, localResourceId);
                        cv.put(DBHelper.ResourcePages.PAGE_INDEX, pageIndex);
                        cv.put(DBHelper.ResourcePages.ETAG, Http.etag(resp));
                        cv.put(DBHelper.ResourcePages.SIZE, mNewItems.size());
                        if (mNextHref != null) {
                            cv.put(DBHelper.ResourcePages.NEXT_HREF, mNextHref);
                        }

                        // insert new page
                        Uri uri = mApp.getContentResolver().insert(ScContentProvider.Content.RESOURCE_PAGES, cv);

                        // insert/update the items
                        // TODO, use Bulk Insert for everything

                        int i = 0;
                        ContentValues[] bulkValues = new ContentValues[mNewItems.size()];
                        for (Parcelable p : mNewItems) {
                            Uri row = ((ModelBase) p).assertInDb(mApp);

                            ContentValues itemCv = new ContentValues();
                            itemCv.put(DBHelper.ResourceItems.RESOURCE_PAGE_ID,uri.getLastPathSegment());
                            itemCv.put(DBHelper.ResourceItems.USER_ID, getCollectionOwner());
                            itemCv.put(DBHelper.ResourceItems.RESOURCE_PAGE_INDEX,i);
                            itemCv.put(DBHelper.ResourceItems.ITEM_ID,((ModelBase) p).id);
                            bulkValues[i] = itemCv;
                            i++;
                        }
                        int inserted = mApp.getContentResolver().bulkInsert(contentUri,bulkValues);
                        return true;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "error", e);
            }
        } else {

        }

        // if we get this far, we either failed, or our etags matched up.

        keepGoing = !TextUtils.isEmpty(mNextHref);

        if (contentUri != null){
            Cursor itemsCursor = mApp.getContentResolver().query(contentUri, null,
                DBHelper.ResourceItems.RESOURCE_PAGE_ID + " = ?",
                new String[]{String.valueOf(localPageId)}, DBHelper.ResourceItems.RESOURCE_PAGE_INDEX);

            // wipe it out and remote load ?? if (c.getCount() == localPageSize){ }
            mNewItems = new ArrayList<Parcelable>();
            if (itemsCursor != null && itemsCursor.moveToFirst()) {
                do {
                    if (Track.class.equals(loadModel)) {
                        mNewItems.add(new Track(itemsCursor));
                    } else if (User.class.equals(loadModel)) {
                        mNewItems.add(new User(itemsCursor));
                    } else if (Event.class.equals(loadModel)) {
                        mNewItems.add(new Event(itemsCursor));
                    }
                } while (itemsCursor.moveToNext());
            }
            publishProgress(mNewItems);
            if (itemsCursor != null) itemsCursor.close();
        }
        return true;
    }



    /* package */ CollectionHolder getCollection(InputStream is, List<? super Parcelable> items) throws IOException {
        CollectionHolder holder = null;
        if (Track.class.equals(loadModel)) {
            holder = mApp.getMapper().readValue(is, TracklistItemHolder.class);
            for (TracklistItem t : (TracklistItemHolder) holder) {
                items.add(new Track(t));
            }
        } else if (User.class.equals(loadModel)) {
            holder = mApp.getMapper().readValue(is, UserlistItemHolder.class);
            for (UserlistItem u : (UserlistItemHolder) holder) {
                items.add(new User(u));
            }
        } else if (Event.class.equals(loadModel)) {
            holder = mApp.getMapper().readValue(is, EventsHolder.class);
            for (Event e : (EventsHolder) holder) {
                items.add(e);
            }
        } else if (Friend.class.equals(loadModel)) {
            holder = mApp.getMapper().readValue(is, FriendHolder.class);
            for (Friend f : (FriendHolder) holder) {
                items.add(f);
            }
        } else if (Comment.class.equals(loadModel)) {
            holder = mApp.getMapper().readValue(is, CommentHolder.class);
            for (Comment f : (CommentHolder) holder) {
                items.add(f);
            }
        }
        return holder;
    }




    public static class EventsHolder extends CollectionHolder<Event> {}
    public static class TracklistItemHolder extends CollectionHolder<TracklistItem> {}
    public static class UserlistItemHolder extends CollectionHolder<UserlistItem> {}
    public static class FriendHolder extends CollectionHolder<Friend> {}
    public static class CommentHolder extends CollectionHolder<Comment> {}

    private long getCollectionOwner(){
        final int uriCode = mApp.getContentUriMatcher().match(contentUri);
        if (uriCode < 200){ // mine
            return mApp.getCurrentUserId();
        } else if (contentUri.getPathSegments().size() > 2){
            return Long.parseLong(contentUri.getPathSegments().get(1));
        } else{
            return -1;
        }
    }
}
