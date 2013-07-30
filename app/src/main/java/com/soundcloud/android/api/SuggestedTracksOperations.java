package com.soundcloud.android.api;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.http.json.JacksonJsonTransformer;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.SuggestedTrack;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.IOUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.text.TextUtils;

import java.io.InputStream;

public class SuggestedTracksOperations extends ScheduledOperations {

    int page = 0;

    /**
     * Temporary until we use RxHttpClient with real endpoints
     */
    public SuggestedTracksOperations() {
        super(ScSchedulers.API_SCHEDULER);
    }

    public Observable<Observable<SuggestedTrack>> getPopMusic() {
        return Observable.create(new Func1<Observer<Observable<SuggestedTrack>>, Subscription>() {
            @Override
            public Subscription call(final Observer<Observable<SuggestedTrack>> observableObserver) {
                observableObserver.onNext(getPopMusicPage(observableObserver, null));
                return Subscriptions.empty();
            }
        });
    }

    public Observable<SuggestedTrack> getPopMusicPage(final Observer<Observable<SuggestedTrack>> observableObserver, String next_href) {
        return schedule(Observable.create(new Func1<Observer<SuggestedTrack>, Subscription>() {
            @Override
            public Subscription call(Observer<SuggestedTrack> suggestedTrackObserver) {
                try {
                    Thread.sleep(1000);
                    CollectionHolder<SuggestedTrack> suggestedTrackCollectionHolder = getDummySuggestedTracks(page > 3);

                    // emit items
                    RxUtils.emitIterable(suggestedTrackObserver, suggestedTrackCollectionHolder);
                    suggestedTrackObserver.onCompleted();

                    // emit next page or done
                    if (!TextUtils.isEmpty(suggestedTrackCollectionHolder.next_href)){
                        final Observable<SuggestedTrack> popMusicPage = getPopMusicPage(observableObserver, suggestedTrackCollectionHolder.next_href);
                        observableObserver.onNext(popMusicPage);
                        page++;
                    } else {
                        observableObserver.onCompleted();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    suggestedTrackObserver.onError(e);
                }
                return Subscriptions.empty();

            }
        }));
    }

    private CollectionHolder<SuggestedTrack> getDummySuggestedTracks(boolean lastPage) throws Exception {
        final InputStream dummyEndpointStream = SoundCloudApplication.instance.getAssets().open(lastPage ? "suggested_tracks_2.json" : "suggested_tracks.json");
        return new JacksonJsonTransformer().fromJson(
                IOUtils.readInputStream(dummyEndpointStream),
                new TypeToken<CollectionHolder<SuggestedTrack>>() { }
        );
    }
}
