package com.soundcloud.android.cache;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.android.task.LoadFollowingsTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

// TODO replace with db lookups
@Deprecated
public class FollowStatus implements Parcelable {
    private static final Request ENDPOINT = Request.to(Endpoints.MY_FOLLOWINGS + "/ids");
    private static final int MAX_AGE = 5 * 60 * 1000;

    private final Set<Long> followings = Collections.synchronizedSet(new HashSet<Long>());
    private long lastUpdate;
    private static FollowStatus sInstance;
    private LoadFollowingsTask mFollowingsTask;
    private WeakHashMap<Listener, Listener> listeners = new WeakHashMap<Listener, Listener>();

    public synchronized static FollowStatus get() {
        if (sInstance == null) {
            sInstance = new FollowStatus();
        }
        return sInstance;
    }

    public synchronized static void set(FollowStatus status) {
        sInstance = status;
    }

    /* package */ FollowStatus() {
    }

    private FollowStatus(Parcel parcel) {
        lastUpdate = parcel.readLong();
        int size = parcel.readInt();
        while (size-- > 0) followings.add(parcel.readLong());
    }

    public boolean isFollowing(long id) {
        return followings.contains(id);
    }

    public boolean isFollowing(User user) {
        return user != null && isFollowing(user.id);
    }

    public synchronized void requestUserFollowings(AndroidCloudAPI api, final Listener listener, boolean force) {
        // add this listener with a weak reference
        listeners.put(listener, null);

        if (AndroidUtils.isTaskFinished(mFollowingsTask) &&
                (force || System.currentTimeMillis() - lastUpdate >= MAX_AGE)) {
            mFollowingsTask = new LoadFollowingsTask(api) {
                @Override
                protected void onPostExecute(List<Long> ids) {
                    lastUpdate = System.currentTimeMillis();
                    if (ids != null) {
                        synchronized (followings) {
                            followings.clear();
                            followings.addAll(ids);
                        }
                    }
                    for (Listener l : listeners.keySet()) {
                        l.onChange(ids != null, FollowStatus.this);
                    }
                }
            };
            mFollowingsTask.execute(ENDPOINT);
        }
    }

    public void addListener(Listener l) {
        listeners.put(l, l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public AsyncTask<Long,Void,Boolean> toggleFollowing(final long userid,
                                final AndroidCloudAPI api,
                                final Handler handler) {
        final boolean addFollowing = toggleFollowing(userid);
        return new AsyncApiTask<Long,Void,Boolean>(api) {
            @Override
            protected Boolean doInBackground(Long... params) {
                Long id = params[0];
                final Request request = Request.to(Endpoints.MY_FOLLOWING, id);
                try {
                    final int status = (addFollowing ? api.put(request) : api.delete(request))
                                      .getStatusLine().getStatusCode();
                    final boolean success;
                    if (addFollowing) {
                        success = status == HttpStatus.SC_CREATED;
                    } else {
                        success = status == HttpStatus.SC_OK || status == HttpStatus.SC_NOT_FOUND;
                    }
                    if (!success) {
                        Log.w(TAG, "error changing following status, resp="+status);
                    }
                    return success;
                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    for (Listener l : listeners.keySet()) {
                        l.onChange(true, FollowStatus.this);
                    }
                } else {
                    updateFollowing(userid, !addFollowing); // revert state change
                }

                if (handler != null) {
                    Message.obtain(handler, success ? 1 : 0).sendToTarget();
                }
            }
        }.execute(userid);
    }

  /* package */ void updateFollowing(long userId, boolean follow) {
        if (follow) {
            followings.add(userId);
        } else {
            followings.remove(userId);
        }
    }

    /* package */ boolean toggleFollowing(long userId) {
        synchronized (followings) {
            if (followings.contains(userId)) {
                followings.remove(userId);
                return false;
            } else {
                followings.add(userId);
                return true;
            }
        }
    }


    public interface Listener {
        void onChange(boolean success, FollowStatus status);
    }

    @Override
    public String toString() {
        return "FollowStatus{" +
                "followingsSet=" + followings +
                ", lastUpdate=" + lastUpdate +
                '}';
    }

    static String getFilename(long userId) {
        return "follow-status-cache-"+userId;
    }

    // Google recommends not to use the filesystem to save parcelables (portability issues)
    // since this is not important information we're going to do it anyway
    // - it's fast (~ 10ms cache writes on a N1).
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

    public static synchronized void initialize(final Context context, long userId) {
        final String statusCache = getFilename(userId);
        try {
            FollowStatus status = fromInputStream(context.openFileInput(statusCache));
            if (status != null) {
                set(status);
            } else {
                context.deleteFile(statusCache);
            }


        } catch (FileNotFoundException ignored) {
            // ignored
        } catch (IOException ignored) {
            Log.w(TAG, "error initializing FollowStatus", ignored);
        }

        get().addListener(new FollowStatus.Listener() {
            @Override
            public void onChange(boolean success, FollowStatus status) {
                if (success) {
                    synchronized (FollowStatus.class) {
                        try {
                            // TODO : StrictMode policy violation; ~duration=39 ms: android.os.StrictMode$StrictModeDiskReadViolation: policy=23 violation=2
                            // already ported to DB in another branch (cache-followstatus-rework)

                            FileOutputStream fos = context.openFileOutput(statusCache, 0);
                            status.toFilesStream(fos);
                            fos.close();
                        } catch (IOException ignored) {
                            Log.w(TAG, "error initializing FollowStatus", ignored);
                        }
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
        dest.writeInt(followings.size());
        for (Long id : followings) dest.writeLong(id);
    }
}
