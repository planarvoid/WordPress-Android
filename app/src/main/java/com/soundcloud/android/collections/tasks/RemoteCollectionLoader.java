package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.behavior.PlayableHolder;
import org.apache.http.HttpStatus;

import android.util.Log;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;

@Deprecated
public class RemoteCollectionLoader<T extends ScResource> implements CollectionLoader<T> {

    private final TrackStorage mTrackStorage;

    public RemoteCollectionLoader() {
        this(new TrackStorage());
    }

    public RemoteCollectionLoader(TrackStorage trackStorage) {
        mTrackStorage = trackStorage;
    }

    @Override
    public ReturnData<T> load(PublicCloudAPI app, CollectionParams<T> params) {
        try {
            CollectionHolder<T> holder = app.readCollection(params.getRequest());

            // suppress unknown resources
            holder.removeUnknownResources();

            storeTracks(holder);

            return new ReturnData<T>(holder.getCollection(),
                    params,
                    holder.getNextHref(),
                    HttpStatus.SC_OK,
                    holder.moreResourcesExist(),
                    true);

        } catch (PublicCloudAPI.UnexpectedResponseException e){
            Log.e(TAG, "error", e);
            return new ReturnData.Error<T>(params, e.getStatusCode());
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return new ReturnData.Error<T>(params);
        }
    }

    /**
     * Store all tracks as they are playback candidates and will be expected to be in the database
     */
    private void storeTracks(CollectionHolder<T> holder) {
        final Collection<T> trackHolders = Collections2.filter(holder.getCollection(), new Predicate<T>() {
            @Override
            public boolean apply(@Nullable T input) {
                return input instanceof PlayableHolder && ((PlayableHolder) input).getPlayable() instanceof Track;
            }
        });

        if (!trackHolders.isEmpty()) {
            final Collection<Track> tracks = Collections2.transform(trackHolders, new Function<T, Track>() {
                @Override
                public Track apply(T input) {
                    return (Track) ((PlayableHolder) input).getPlayable();
                }
            });

            fireAndForget(mTrackStorage.storeCollectionAsync(tracks));
        }
    }
}
