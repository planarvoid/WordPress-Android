package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.UnexpectedResponseException;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.storage.TrackStorage;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.util.Log;

import java.io.IOException;
import java.util.Collection;

@Deprecated
public class RemoteCollectionLoader<T extends PublicApiResource> implements CollectionLoader<T> {

    private final TrackStorage trackStorage;

    public RemoteCollectionLoader() {
        this(new TrackStorage());
    }

    public RemoteCollectionLoader(TrackStorage trackStorage) {
        this.trackStorage = trackStorage;
    }

    @Override
    public ReturnData<T> load(PublicCloudAPI app, CollectionParams<T> params) {
        try {
            CollectionHolder<T> holder = app.readCollection(params.getRequest());

            // suppress unknown resources
            holder.removeUnknownResources();

            storeTracks(holder);

            return new ReturnData<>(holder.getCollection(),
                    params,
                    holder.getNextHref(),
                    HttpStatus.SC_OK,
                    holder.moreResourcesExist(),
                    true);

        } catch (UnexpectedResponseException e){
            Log.e(TAG, "error", e);
            return new ReturnData.Error<>(params, e.getStatusCode());
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return new ReturnData.Error<>(params);
        }
    }

    /**
     * Store all tracks as they are playback candidates and will be expected to be in the database
     */
    private void storeTracks(CollectionHolder<T> holder) {
        final Collection<T> trackHolders = Collections2.filter(holder.getCollection(), new Predicate<T>() {
            @Override
            public boolean apply(@Nullable T input) {
                return input instanceof PlayableHolder && ((PlayableHolder) input).getPlayable() instanceof PublicApiTrack;
            }
        });

        if (!trackHolders.isEmpty()) {
            final Collection<PublicApiTrack> tracks = Collections2.transform(trackHolders, new Function<T, PublicApiTrack>() {
                @Override
                public PublicApiTrack apply(T input) {
                    return (PublicApiTrack) ((PlayableHolder) input).getPlayable();
                }
            });

            fireAndForget(trackStorage.storeCollectionAsync(tracks));
        }
    }
}
