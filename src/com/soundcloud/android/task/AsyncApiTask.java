package com.soundcloud.android.task;

import android.os.AsyncTask;
import android.util.Log;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public abstract class AsyncApiTask<Params, Progress, Result>
        extends AsyncTask<Params, Progress, Result>
        implements CloudAPI.Enddpoints {

    protected WeakReference<CloudAPI> mApi;

    public AsyncApiTask(CloudAPI api) {
        this.mApi = new WeakReference<CloudAPI>(api);
    }

    protected CloudAPI api() {
        return mApi.get();
    }

    public static List<NameValuePair> params(Object... args) {
        if (args == null) return null;
        if (args.length % 2 != 0) throw new IllegalArgumentException("need even number of arguments");

        List<NameValuePair> pairs = new ArrayList<NameValuePair>(args.length / 2);
        for (int i = 0; i < args.length; i += 2) {
            pairs.add(new BasicNameValuePair(args[i].toString(), args[i + 1].toString()));
        }
        return pairs;
    }

    public static String queryString(Object... args) {
        return URLEncodedUtils.format(params(args), "UTF-8");
    }

    public void warn(String s, HttpResponse response) {
        Log.w(TAG, s + ": " + response.getStatusLine().toString());
    }

    protected void warn(String s, Throwable throwable) {
        Log.w(TAG, s, throwable);
    }
}
