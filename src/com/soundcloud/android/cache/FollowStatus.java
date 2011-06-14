package com.soundcloud.android.cache;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.LoadFollowingsTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.accounts.Account;
import android.content.Context;
import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class FollowStatus implements Parcelable {
    private static final Request ENDPOINT = Request.to(Endpoints.MY_FOLLOWINGS + "/ids");
    private static final int MAX_AGE = 5 * 60 * 1000;

    private Set<Long> followingsSet;
    private long lastUpdate;
    private static FollowStatus sInstance;
    private LoadFollowingsTask mFollowingsTask;
    private WeakHashMap<Listener, Listener> listeners = new WeakHashMap<Listener,Listener>();

    public synchronized static FollowStatus get() {
        if (sInstance == null) {
            sInstance = new FollowStatus();
        }
        return sInstance;
    }

    public synchronized static void set(FollowStatus status) {
        sInstance = status;
    }

    private FollowStatus() {
    }

    private FollowStatus(Parcel parcel) {
        lastUpdate = parcel.readLong();
        int size = parcel.readInt();
        if (size != -1) {
            followingsSet = new HashSet<Long>(size);
            while (size-- != 0) followingsSet.add(parcel.readLong());
        }
    }


    public boolean following(User user) {
        return followingsSet != null && followingsSet.contains(user.id);
    }

    public synchronized void requestUserFollowings(AndroidCloudAPI api, final Listener listener, boolean force) {
        addListener(listener);
        if (CloudUtils.isTaskFinished(mFollowingsTask) &&
                (force || System.currentTimeMillis() - lastUpdate >= MAX_AGE)) {
            mFollowingsTask = new LoadFollowingsTask(api) {
                @Override
                protected void onPostExecute(List<Long> ids) {
                    lastUpdate = System.currentTimeMillis();
                    if (ids != null) {
                        followingsSet = new HashSet<Long>(ids);
                    }
                    for (Listener l : listeners.keySet()) {
                        l.onFollowings(ids != null, FollowStatus.this);
                    }
                }
            };
            mFollowingsTask.execute(ENDPOINT);
        }
    }

    public void addListener(Listener l) {
        listeners.put(l, l);
    }

    public void updateFollowing(long userId, boolean follow) {
        if (follow) {
            followingsSet.add(userId);
        } else {
            followingsSet.remove(userId);
        }
    }

    public boolean toggleFollowing(long userId){
        if (followingsSet.contains(userId)) {
            followingsSet.remove(userId);
            return false;
        } else {
            followingsSet.add(userId);
            return true;
        }
    }

    public interface Listener {
        void onFollowings(boolean success, FollowStatus status);
    }

    @Override
    public String toString() {
        return "FollowStatus{" +
                "followingsSet=" + followingsSet +
                ", lastUpdate=" + lastUpdate +
                '}';
    }

    static String getFilename(Account account) {
        return "follow-status-cache-" + account.name;
    }


    // Google recommends not to use the filesystem to save parcelables
    // since  this is not important information we're going to do it anyway - it's fast.
    static FollowStatus fromInputStream(FileInputStream is) {
        try {
            byte[] b = new byte[8192];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int n;
            while ((n = is.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            bos.close();
            Parcel parcel = Parcel.obtain();
            byte[] result = bos.toByteArray();

            parcel.unmarshall(result, 0, result.length);
            parcel.setDataPosition(0);
            FollowStatus status = CREATOR.createFromParcel(parcel);
            parcel.recycle();
            return status;
        } catch (ParcelFormatException ignored) {
            Log.w(TAG, "error", ignored);
            return null;
        } catch (IOException ignored) {
            Log.w(TAG, "error", ignored);
            return null;
        }
    }

    public static synchronized void initialize(final Context context, Account account) {
        final String statusCache = getFilename(account);
        try {
            FollowStatus status = fromInputStream(context.openFileInput(statusCache));
            if (status != null) {
                Log.d(TAG, "loaded from cache: " + status);
                set(status);
            } else {
                context.deleteFile(statusCache);
            }
        } catch (IOException ignored) {
            Log.w(TAG, "error initializing FollowStatus", ignored);
        }

        get().addListener(new FollowStatus.Listener() {
            @Override
            public void onFollowings(boolean success, FollowStatus status) {
                if (success) {
                    try {
                        Log.d(TAG, "wrote " + status + " to " + statusCache);
                        FileOutputStream fos = context.openFileOutput(statusCache, 0);
                        status.toFilesStream(fos);
                        fos.close();
                    } catch (IOException ignored) {
                        Log.w(TAG, "error initializing FollowStatus", ignored);
                    }
                }
            }
        });
    }

    public static final Parcelable.Creator<FollowStatus> CREATOR = new Parcelable.Creator<FollowStatus>() {
        public FollowStatus createFromParcel(Parcel in) {
            return new FollowStatus(in);
        }

        public FollowStatus[] newArray(int size) {
            return new FollowStatus[size];
        }
    };

    public void toFilesStream(OutputStream os) {
        Parcel parcel = Parcel.obtain();
        writeToParcel(parcel, 0);
        try {
            os.write(parcel.marshall());
        } catch (IOException ignored) {
            Log.w(TAG, "error", ignored);
        }
        parcel.recycle();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(lastUpdate);
        dest.writeInt(followingsSet == null ? -1 : followingsSet.size());
        if (followingsSet != null) for (Long id : followingsSet) dest.writeLong(id);
    }
}
