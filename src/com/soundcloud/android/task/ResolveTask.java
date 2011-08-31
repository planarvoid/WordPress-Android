package com.soundcloud.android.task;

import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.lang.ref.WeakReference;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class ResolveTask extends AsyncApiTask<Uri, Void, HttpResponse>  {

    private WeakReference<ResolveListener> mListenerWeakReference;

    public ResolveTask(AndroidCloudAPI api) {
        super(api);
    }

    @Override
    protected HttpResponse doInBackground(Uri... params) {

        final Uri uri = params[0];

        try {
            return mApi.get(Request.to(Endpoints.RESOLVE).add("url",uri.toString()));
        } catch (IOException e) {
            warn("error resolving url", e);
            return null;
        }
    }

    public void setListener(ResolveListener listener){
        mListenerWeakReference = new WeakReference<ResolveListener>(listener);
    }

    @Override
    protected void onPostExecute(HttpResponse response) {
        ResolveListener listener = mListenerWeakReference != null ? mListenerWeakReference.get() : null;
        if (listener == null) return;

        if (response != null) {
            final Header location = response.getFirstHeader("Location");
            if (location != null && location.getValue() != null) {
                listener.onUrlResolved(Uri.parse(location.getValue()));
            } else {
                listener.onUrlError();
            }
        } else {
            listener.onUrlError();
        }
    }

    // Define our custom Listener interface
    public interface ResolveListener {
        void onUrlResolved(Uri uri);
        void onUrlError();
    }
}
