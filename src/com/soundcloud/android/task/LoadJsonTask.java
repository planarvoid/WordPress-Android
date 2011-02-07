package com.soundcloud.android.task;

import android.os.AsyncTask;
import android.os.Parcelable;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.mapper.CloudDateFormat;
import com.soundcloud.android.objects.Track;
import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.StdDateFormat;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
            throw new RuntimeException(e);
        }
    }
}
