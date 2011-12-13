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
import com.soundcloud.android.adapter.SyncedCollectionAdapter;
import com.soundcloud.android.model.*;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public abstract class LoadCollectionTask extends AsyncTask<String, List<? super Parcelable>, Boolean> {

    protected SoundCloudApplication mApp;
    protected CollectionParams mParams;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;
    public boolean keepGoing;
    /* package */ List<Parcelable> mNewItems = new ArrayList<Parcelable>();

    public static class CollectionParams {
        public Class<?> loadModel;
        public Uri contentUri;
        public int pageIndex;
        public boolean refresh;
    }

    public LoadCollectionTask(SoundCloudApplication app, CollectionParams params) {
        mApp = app;
        mParams = params;
    }

    public void setAdapter(LazyEndlessAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
        if (lazyEndlessAdapter != null) {
            mParams.loadModel = lazyEndlessAdapter.getLoadModel(false);
        }
    }

    protected List<? super Parcelable> loadLocalContent(){
        Cursor itemsCursor = mApp.getContentResolver().query(CloudUtils.getPagedUri(mParams.contentUri, mParams.pageIndex), null, null, null, null);
            List<Parcelable> items = new ArrayList<Parcelable>();
            if (itemsCursor != null && itemsCursor.moveToFirst()) {
                do {
                    if (Track.class.equals(mParams.loadModel)) {
                        items.add(new Track(itemsCursor));
                    } else if (User.class.equals(mParams.loadModel)) {
                        items.add(new User(itemsCursor));
                    } else if (Friend.class.equals(mParams.loadModel)) {
                        items.add(new User(itemsCursor));
                    }
                } while (itemsCursor.moveToNext());
            }
        if (itemsCursor != null) itemsCursor.close();
        return items;
    }


}
