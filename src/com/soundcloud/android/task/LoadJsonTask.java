package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;

import org.apache.http.HttpResponse;
import org.codehaus.jackson.map.type.TypeFactory;

import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class LoadJsonTask<T> extends AsyncApiTask<String, Parcelable, List<T>> {
    public LoadJsonTask(AndroidCloudAPI api) {
        super(api);
    }

    protected List<T> list(String path, Class<T> type) {
        return list(path, type, false);
    }

    protected List<T> list(String path, Class<T> type, boolean failFast) {
        try {

            HttpResponse response = api().getContent(path);
            InputStream is = response.getEntity().getContent();
            //Log.i(TAG,"Json returned from " + path + " : " + CloudUtils.readInputStream(is));
            if (response.getStatusLine().getStatusCode() == SC_OK) {
                return api().getMapper().readValue(is,
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
