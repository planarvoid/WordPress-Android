package com.soundcloud.android.task.collection;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.api.Http;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RemoteCollectionLoader<T extends ScResource> extends CollectionLoader<T> {

    @Override
    public ReturnData<T> load(AndroidCloudAPI app, CollectionParams<T> params) {
        try {
            HttpResponse resp = app.get(params.request);
            final int responseCode = resp.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }

            // process new items and publish them
            CollectionHolder<T> holder = SoundCloudApplication.MODEL_MANAGER.getCollectionFromStream(resp.getEntity().getContent());

            final String nextHref = TextUtils.isEmpty(holder.next_href) ? null : holder.next_href;
            holder.resolve(app.getContext());

            // suppress playlists for now
            List<ScResource> toRemove = new ArrayList<ScResource>();
            for (ScResource resource : holder){
                if (resource instanceof Playlist || resource instanceof UnknownResource) toRemove.add(resource);
            }
            holder.collection.removeAll(toRemove);

            return new ReturnData<T>(holder, params, nextHref, responseCode, !TextUtils.isEmpty(nextHref), true);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }

        return new ReturnData<T>(params) {
            {
                success = false;
                keepGoing = false;
                responseCode = EmptyListView.Status.CONNECTION_ERROR;
            }
        };
    }
}
