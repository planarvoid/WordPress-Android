package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Request;

import org.apache.http.HttpResponse;
import org.codehaus.jackson.map.type.TypeFactory;

import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class LoadJsonTask<T> extends AsyncApiTask<Request, Parcelable, List<T>> {
    public LoadJsonTask(AndroidCloudAPI api) {
        super(api);
    }

    protected List<T> list(Request request, Class<T> type) {
        return list(request, type, false);
    }

    protected List<T> list(Request path, Class<T> type, boolean failFast) {
        try {

            HttpResponse response = api().get(path);

            Log.i("aaa","return:"+CloudUtils.readInputStream(response.getEntity().getContent()));
            response = api().get(path);

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
