package com.soundcloud.android.cache;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.task.LoadConnectionsTask;
import com.soundcloud.api.Request;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Connections extends ParcelCache<Connection> {
    private static Connections sInstance;
    private  Connections() {}

    public Connections(InputStream is) throws IOException {
        super(is);
    }

    public static synchronized Connections get() {
        if (sInstance == null) {
            sInstance = new Connections();
        }
        return sInstance;
    }

    public static synchronized void set(Connections connections) {
        sInstance = connections;
    }

    @Override
    protected AsyncTask<?, ?, List<Connection>> executeTask(AndroidCloudAPI api, final Listener<Connection> listener) {
        return new LoadConnectionsTask(api) {
            @Override
            protected void onPostExecute(List<Connection> connections) {
                listener.onChanged(connections, null);
            }
        }.execute((Request)null);
    }

    public static synchronized void initialize(final Context context, final String filename) {
        try {
            set(new Connections(context.openFileInput(filename)));
        } catch (FileNotFoundException ignored) {
            // ignored
        } catch (IOException ignored) {
            Log.w(TAG, "error initializing Connections", ignored);
            context.deleteFile(filename);
        }

        get().addListener(new Listener<Connection>() {
            @Override
            public void onChanged(List<Connection> objects, ParcelCache<Connection> cache) {
                if (objects != null) {
                    try {
                        Log.d(TAG, "saving connections cache");
                        cache.toFilesStream(context.openFileOutput(filename, 0));
                    } catch (FileNotFoundException ignored) {
                    }
                }
            }
        });
    }
}
