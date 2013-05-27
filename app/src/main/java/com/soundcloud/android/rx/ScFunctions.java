package com.soundcloud.android.rx;

import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

import java.util.List;

public class ScFunctions {

    public static AlwaysTrue alwaysTrue() {
        return AlwaysTrue.INSTANCE;
    }

    private enum AlwaysTrue implements Func0<Boolean> {
        INSTANCE;

        @Override
        public Boolean call() {
            return true;
        }
    }

    /**
     * Turns an <pre>Observable<Playlist></pre> into an <pre>Observable<Track></pre> by emitting individual tracks
     * from the playlist to the given Track observer.
     */
    public static final Func1<Observable<Playlist>, Observable<Track>> PLAYLIST_OBS_TO_TRACKS_OBS =
            new Func1<Observable<Playlist>, Observable<Track>>() {
                @Override
                public Observable<Track> call(final Observable<Playlist> observable) {
                    return Observable.create(new Func1<Observer<Track>, Subscription>() {
                        @Override
                        public Subscription call(final Observer<Track> trackObserver) {
                            return observable.subscribe(new Action1<Playlist>() {
                                @Override
                                public void call(Playlist playlist) {
                                    for (Track track : playlist.tracks) {
                                        trackObserver.onNext(track);
                                    }
                                    trackObserver.onCompleted();
                                }
                            });
                        }
                    });
                }
            };

}
