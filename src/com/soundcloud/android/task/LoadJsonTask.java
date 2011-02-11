package com.soundcloud.android.task;

import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.CloudAPI;
import org.codehaus.jackson.map.type.TypeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public abstract class LoadJsonTask<T> extends AsyncApiTask<String, Parcelable, List<T>> {
    public LoadJsonTask(CloudAPI api) {
        super(api);
    }

    List<T> list(String path, Class<T> type) {
        return list(path, type, false);
    }

    List<T> list(String path, Class<T> type, boolean failFast) {
        try {

            final InputStream is = api().executeRequest(path);
            if (is == null) throw new NullPointerException();

            return api().getMapper().readValue(is, TypeFactory.collectionType(ArrayList.class, type));
        } catch (IOException e) {
            Log.w(TAG, "error fetching JSON", e);

            if (failFast) {
                throw new RuntimeException(e);
            } else {
                return null;
            }
        }
    }
}
