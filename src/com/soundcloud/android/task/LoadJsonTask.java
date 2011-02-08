package com.soundcloud.android.task;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.mapper.CloudDateFormat;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.objects.Model;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public abstract class LoadJsonTask<T> extends AsyncTask<String, Parcelable, List<T>> {
    protected WeakReference<CloudAPI> mApi;


    public LoadJsonTask(CloudAPI api) {
        this.mApi = new WeakReference<CloudAPI>(api);
    }

    protected InputStream httpGet(String path) throws IOException {
        return mApi.get().executeRequest(path);
    }

    protected ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().setDateFormat(CloudDateFormat.INSTANCE);
        return mapper;
    }

    <T> List<T> list(String path, Class<T> type) {
        try {
            final InputStream is = httpGet(path);
            if (is == null) throw new NullPointerException();
            return getMapper().readValue(is, TypeFactory.collectionType(ArrayList.class, type));
        } catch (IOException e) {
            Log.w(TAG, "error fetching JSON", e);
            return Collections.emptyList();
        }
    }
}
