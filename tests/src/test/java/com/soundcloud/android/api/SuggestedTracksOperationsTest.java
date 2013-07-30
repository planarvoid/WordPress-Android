package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.SuggestedTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.observers.ScObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;
import rx.observables.BlockingObservable;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedTracksOperationsTest {

    private SuggestedTracksOperations suggestedTracksOperations;

    @Before
    public void setUp(){
        suggestedTracksOperations = new SuggestedTracksOperations();
    }

    @Test
    public void shouldReturnDummySuggestedTracks(){
        final BlockingObservable<Observable<SuggestedTrack>> observableBlockingObservable = suggestedTracksOperations.getPopMusic().toBlockingObservable();
        List<SuggestedTrack> trackList = Lists.newArrayList(observableBlockingObservable.last().toBlockingObservable().toIterable());
        expect(trackList.size()).toBe(8);
        expect(trackList.get(0).getTitle()).toEqual("Evolution of Get Lucky [Daft Punk chronologic cover]");
        expect(trackList.get(0).getStreamUrl()).toEqual("https://api.soundcloud.com/tracks/96017719/stream");
    }

    @Test
    public void shouldReturnDummyTracksObservable(){
        Observable<Observable<SuggestedTrack>> suggestedTrackPageObservable = suggestedTracksOperations.getPopMusic();
        suggestedTrackPageObservable.subscribe(new ScObserver<Observable<SuggestedTrack>>() {
            @Override
            public void onCompleted() {
                super.onCompleted();
            }

            @Override
            public void onError(Exception e) {
                super.onError(e);
            }

            @Override
            public void onNext(Observable<SuggestedTrack> args) {
                super.onNext(args);
            }
        });


    }
}
