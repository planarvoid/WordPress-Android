package com.soundcloud.android.task;

import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.CloudAPI;
import org.apache.http.HttpResponse;
import org.codehaus.jackson.map.type.TypeFactory;

import java.io.IOException;
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

            HttpResponse response = api().getContent(path);

            if (response.getStatusLine().getStatusCode() == SC_OK) {
                return api().getMapper().readValue(response.getEntity().getContent(),
                        TypeFactory.collectionType(ArrayList.class, type));
            } else {
                Log.w(TAG, "invalid response code " + response.getStatusLine());
                return null;
            }


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
