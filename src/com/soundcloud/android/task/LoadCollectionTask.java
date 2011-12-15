package com.soundcloud.android.task;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.*;
import com.soundcloud.android.utils.CloudUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public abstract class LoadCollectionTask extends AsyncTask<Boolean, List<? super Parcelable>, Boolean> {

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

    public LoadCollectionTask(SoundCloudApplication app, LazyEndlessAdapter lazyEndlessAdapter) {
        mApp = app;
        setAdapter(lazyEndlessAdapter);
        mParams = new CollectionParams();
        if (lazyEndlessAdapter != null) {
           mParams.loadModel = lazyEndlessAdapter.getLoadModel();
           mParams.contentUri = lazyEndlessAdapter.getContentUri(false);
           mParams.pageIndex = lazyEndlessAdapter.getPageIndex();
           mParams.refresh = lazyEndlessAdapter.isRefreshing();
        }
    }

    public void setAdapter(LazyEndlessAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
    }

    protected List<? extends Parcelable> loadLocalContent(){
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
