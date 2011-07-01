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
import java.util.HashMap;
import java.util.List;

public abstract class ParcelCache<T extends Parcelable> implements Parcelable {
    private static final int MAX_AGE = 5 * 60 * 1000;

    private final List<T> objects;

    private AsyncTask<?, ?, List<T>> mTask;
    // XXX needs to be WeakHashMap
    private HashMap<Listener<T>, Boolean> listeners = new HashMap<Listener<T>, Boolean>();
    private boolean mLoadedFromCache;
    private long lastUpdate;

    ParcelCache() {
        objects = new ArrayList<T>();
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

            lastUpdate = parcel.readLong();
            int num = parcel.readInt();
            objects = new ArrayList<T>(num);
            while (num-- > 0) {
                objects.add(parcel.<T>readParcelable(getClass().getClassLoader()));
            }

            parcel.recycle();

            Log.d(TAG, "loaded from cache: "+objects);
            mLoadedFromCache = true;
        } catch (ParcelFormatException e) {
            throw new IOException(e);
        }
    }


    public void addListener(Listener<T> l) {
        listeners.put(l, true);
    }

    public void removeListener(Listener<T> l) {
        listeners.remove(l);
    }

    public synchronized void requestUpdate(AndroidCloudAPI api, final Listener<T> listener, boolean force) {
        addListener(listener);

        if (mLoadedFromCache) {
            for (Listener<T> l : listeners.keySet()) {
                l.onChanged(getObjects(), this);
            }
        }

        if (CloudUtils.isTaskFinished(mTask) &&
            (force || System.currentTimeMillis() - lastUpdate >= MAX_AGE)) {
            mTask = executeTask(api, new Listener<T>() {
                @Override
                public void onChanged(List<T> objs, ParcelCache<T> cache) {
                    lastUpdate = System.currentTimeMillis();
                    if (objs != null) {
                        synchronized (objects) {
                            objects.clear();
                            objects.addAll(objs);
                        }
                    }
                    for (Listener<T> l : listeners.keySet()) {
                        l.onChanged(objs == null ? null : getObjects(), ParcelCache.this);
                    }
                }
            });
        }
    }

    public List<T> getObjects() {
        return objects == null ? null : new ArrayList<T>(objects);
    }

    public interface Listener<T extends Parcelable> {
        void onChanged(List<T> objects, ParcelCache<T> cache);
    }

    protected abstract AsyncTask<?, ?, List<T>> executeTask(AndroidCloudAPI api, Listener<T> listener);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "objects=" + objects +
                ", lastUpdate=" + lastUpdate +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(lastUpdate);
        dest.writeInt(objects.size());
        for (T obj : objects) {
            dest.writeParcelable(obj, flags);
        }
    }

    public synchronized void toFilesStream(OutputStream os) {
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
