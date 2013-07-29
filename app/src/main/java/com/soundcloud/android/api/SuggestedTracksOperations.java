package com.soundcloud.android.api;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.http.json.JacksonJsonTransformer;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.SuggestedTrack;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import com.soundcloud.android.utils.IOUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import java.io.InputStream;

public class SuggestedTracksOperations extends ScheduledOperations {

    /**
     * Temporary until we use RxHttpClient with real endpoints
     */
    public SuggestedTracksOperations() {
        super(ScSchedulers.API_SCHEDULER);
    }

    public Observable<SuggestedTrack> getPopMusic() {
        return schedule(Observable.create(new Func1<Observer<SuggestedTrack>, Subscription>() {
            @Override
            public Subscription call(Observer<SuggestedTrack> suggestedTrackObserver) {
                try {

                    Thread.sleep(3000);

                    final InputStream dummyEndpointStream = SoundCloudApplication.instance.getAssets().open("suggested_tracks.json");
                    CollectionHolder<SuggestedTrack> suggestedTrackCollectionHolder = new JacksonJsonTransformer().fromJson(
                            IOUtils.readInputStream(dummyEndpointStream),
                            new TypeToken<CollectionHolder<SuggestedTrack>>() {
                            }
                    );

                    RxUtils.emitIterable(suggestedTrackObserver, suggestedTrackCollectionHolder.collection);

                    suggestedTrackObserver.onCompleted();

                } catch (Exception e) {
                    e.printStackTrace();
                    suggestedTrackObserver.onError(e);
                }
                return Subscriptions.empty();

            }
        }));
    }
}
