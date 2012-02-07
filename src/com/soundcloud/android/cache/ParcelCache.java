package com.soundcloud.android.cache;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.utils.CloudUtils;

import android.os.AsyncTask;
import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ParcelCache<T extends Parcelable> implements Parcelable {
    private static final int MAX_AGE = 5 * 60 * 1000;

    private final List<T> mObjects;
    private AsyncTask<?, ?, List<T>> mTask;

    private Set<Listener<T>> mListeners = new HashSet<Listener<T>>();

    private boolean mLoadedFromCache;
    private long mLastUpdate;

    ParcelCache() {
        mObjects = Collections.synchronizedList(new ArrayList<T>());
    }

    public ParcelCache(InputStream is) throws IOException {
        byte[] b = new byte[8192];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int n;
        while ((n = is.read(b)) != -1) {
            bos.write(b, 0, n);
        }
        bos.close();
        Parcel parcel = Parcel.obtain();
        byte[] result = bos.toByteArray();

        try {
            parcel.unmarshall(result, 0, result.length);
            parcel.setDataPosition(0);

            mLastUpdate = parcel.readLong();
            int num = parcel.readInt();
            mObjects = Collections.synchronizedList(new ArrayList<T>(num));
            while (num-- > 0) {
                mObjects.add(parcel.<T>readParcelable(getClass().getClassLoader()));
            }

            parcel.recycle();

            Log.d(TAG, "loaded from cache: "+ mObjects);
            mLoadedFromCache = true;
        } catch (ParcelFormatException e) {
            throw new IOException(e);
        }
    }


    public void addListener(Listener<T> l) {
        mListeners.add(l);
    }

    public void removeListener(Listener<T> l) {
        mListeners.remove(l);
    }

    public synchronized void requestUpdate(AndroidCloudAPI api, boolean force, final Listener<T> listener) {
        if (CloudUtils.isTaskFinished(mTask) &&
            (force || System.currentTimeMillis() - mLastUpdate >= MAX_AGE)) {
            mTask = executeTask(api, new Listener<T>() {
                @Override
                public void onChanged(List<T> objs, ParcelCache<T> cache) {
                    mLastUpdate = System.currentTimeMillis();
                    if (objs != null) {
                        synchronized (mObjects) {
                            mObjects.clear();
                            mObjects.addAll(objs);
                        }
                    }

                    if (listener != null) {
                        listener.onChanged(objs == null ? null : getObjects(), ParcelCache.this);
                    }

                    for (Listener<T> l : mListeners) {
                        l.onChanged(objs == null ? null : getObjects(), ParcelCache.this);
                    }
                }
            });
        }
    }

    public List<T> getObjects() {
        return mObjects == null ? null : new ArrayList<T>(mObjects);
    }

    public List<T> getObjectsOrNull() {
        return isCached() ? getObjects() : null;
    }

    public boolean isCached() {
        return mLoadedFromCache;
    }

    public interface Listener<T extends Parcelable> {
        /**
         * @param objects list of objects retrieved, or null if failed
         * @param cache the cache object
         */
        void onChanged(List<T> objects, ParcelCache<T> cache);
    }

    protected abstract AsyncTask<?, ?, List<T>> executeTask(AndroidCloudAPI api, Listener<T> listener);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "objects=" + mObjects +
                ", lastUpdate=" + mLastUpdate +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mLastUpdate);
        dest.writeInt(mObjects.size());
        for (T obj : mObjects) {
            dest.writeParcelable(obj, flags);
        }
    }

    public synchronized void toFilesStream(OutputStream os) {
        // TODO StrictMode policy violation; ~duration=29 ms: android.os.StrictMode$StrictModeDiskWriteViolation: policy=23 violation=1
        try {
            Parcel parcel = Parcel.obtain();
            writeToParcel(parcel, 0);
            try {
                os.write(parcel.marshall());
            } catch (IOException ignored) {
                Log.w(TAG, "error", ignored);
            }
            parcel.recycle();
            os.close();
        } catch (IOException e) {
            Log.w(TAG, "error initializing saving " + this, e);
        }
    }
}
