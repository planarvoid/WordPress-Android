package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static com.soundcloud.android.testsupport.fixtures.TestStorageResults.successfulChange;
import static com.soundcloud.android.testsupport.fixtures.TestStorageResults.successfulInsert;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class RepostOperationsTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    private RepostOperations operations;
    private TestObserver<PropertySet> testObserver = new TestObserver<>();
    private TestEventBus eventBus = new TestEventBus();

    @Mock private RepostStorage repostStorage;
    @Mock private Command<Urn, InsertResult> addRepost;
    @Mock private Command<Urn, ChangeResult> removeRepost;
    @Mock private ApiClientRx apiClientRx;
    @Mock private WriteResult writeResult;

    @Before
    public void setUp() throws Exception {
        operations = new RepostOperations(repostStorage, apiClientRx, Schedulers.immediate(), eventBus);
        when(repostStorage.addRepost()).thenReturn(addRepost);
        when(repostStorage.removeRepost()).thenReturn(removeRepost);
    }

    @Test
    public void shouldStoreTrackRepostAndPushToApi() throws Exception {
        when(addRepost.toObservable(TRACK_URN)).thenReturn(Observable.just(successfulInsert()));
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        expect(testObserver.getOnNextEvents()).toContainExactly(PropertySet.from(
                PlayableProperty.URN.bind(TRACK_URN),
                PlayableProperty.IS_REPOSTED.bind(true)
        ));
    }

    @Test
    public void shouldStorePlaylistRepostAndPushToApi() throws Exception {
        when(addRepost.toObservable(PLAYLIST_URN)).thenReturn(Observable.just(successfulInsert()));
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/playlist_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(PLAYLIST_URN, true).subscribe(testObserver);

        expect(testObserver.getOnNextEvents()).toContainExactly(PropertySet.from(
                PlayableProperty.URN.bind(PLAYLIST_URN),
                PlayableProperty.IS_REPOSTED.bind(true)
        ));
    }

    @Test
    public void shouldPublishEntityChangedEventAfterSuccesfulPushToApi() throws Exception {
        when(addRepost.toObservable(TRACK_URN)).thenReturn(Observable.just(successfulInsert()));
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getFirstUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        PlayableProperty.URN.bind(TRACK_URN),
                        PlayableProperty.IS_REPOSTED.bind(true)
                )
        );
    }

    @Test
    public void shouldNotPushRepostToApiIfStorageCallFailed() throws Exception {
        when(addRepost.toObservable(TRACK_URN)).thenReturn(Observable.<InsertResult>error(mock(PropellerWriteException.class)));
        when(removeRepost.call(TRACK_URN)).thenReturn(successfulChange());

        PublishSubject<ApiResponse> subject = PublishSubject.create();
        when(apiClientRx.response(any(ApiRequest.class))).thenReturn(subject);

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);
        subject.onNext(TestApiResponses.ok()); // this must not propagate

        expect(testObserver.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void shouldPublishNotRepostedEventIfInsertFailed() throws Exception {
        when(addRepost.toObservable(TRACK_URN)).thenReturn(Observable.<InsertResult>error(mock(PropellerWriteException.class)));
        when(removeRepost.call(TRACK_URN)).thenReturn(successfulChange());

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getFirstUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        TrackProperty.URN.bind(TRACK_URN),
                        TrackProperty.IS_REPOSTED.bind(false)
                )
        );
    }

    @Test
    public void shouldRollbackStoredRepostAfterFailedApiCall() throws Exception {
        when(addRepost.toObservable(TRACK_URN)).thenReturn(Observable.just(successfulInsert()));
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        verify(removeRepost).call(TRACK_URN);
    }

    @Test
    public void shouldPublishNotRepostedEventAfterFailedApiCall() throws Exception {
        when(addRepost.toObservable(TRACK_URN)).thenReturn(Observable.just(successfulInsert()));
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getFirstUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        TrackProperty.URN.bind(TRACK_URN),
                        TrackProperty.IS_REPOSTED.bind(false)
                )
        );
    }

    @Test
    public void shouldStoreTrackUnpostAndPushToApi() throws Exception {
        when(removeRepost.toObservable(TRACK_URN)).thenReturn(Observable.just(successfulChange()));
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        expect(testObserver.getOnNextEvents()).toContainExactly(PropertySet.from(
                PlayableProperty.URN.bind(TRACK_URN),
                PlayableProperty.IS_REPOSTED.bind(false)
        ));
    }

    @Test
    public void shouldStorePlaylistUnpostAndPushToApi() throws Exception {
        when(removeRepost.toObservable(PLAYLIST_URN)).thenReturn(Observable.just(successfulChange()));
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/playlist_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(PLAYLIST_URN, false).subscribe(testObserver);

        expect(testObserver.getOnNextEvents()).toContainExactly(PropertySet.from(
                PlayableProperty.URN.bind(PLAYLIST_URN),
                PlayableProperty.IS_REPOSTED.bind(false)
        ));
    }

    @Test
    public void shouldPublishEntityChangedEventForUnpostAfterSuccesfulPushToApi() throws Exception {
        when(removeRepost.toObservable(TRACK_URN)).thenReturn(Observable.just(successfulChange()));

        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getFirstUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        PlayableProperty.URN.bind(TRACK_URN),
                        PlayableProperty.IS_REPOSTED.bind(false)
                )
        );
    }

    @Test
    public void shouldNotDeleteRepostFromApiIfStorageCallFailed() throws Exception {
        when(removeRepost.toObservable(TRACK_URN)).thenReturn(Observable.<ChangeResult>error(mock(PropellerWriteException.class)));

        PublishSubject<ApiResponse> subject = PublishSubject.create();
        when(apiClientRx.response(any(ApiRequest.class))).thenReturn(subject);

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);
        subject.onNext(TestApiResponses.ok()); // this must not propagate

        expect(testObserver.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void shouldPublishRepostedEventIfRepostRemovalFailed() throws Exception {
        when(removeRepost.toObservable(TRACK_URN)).thenReturn(Observable.<ChangeResult>error(mock(PropellerWriteException.class)));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getFirstUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        TrackProperty.URN.bind(TRACK_URN),
                        TrackProperty.IS_REPOSTED.bind(true)
                )
        );
    }

    @Test
    public void shouldRollbackRepostRemovalAfterFailedRemoteRemoval() throws Exception {
        when(removeRepost.toObservable(TRACK_URN)).thenReturn(Observable.just(successfulChange()));
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        verify(addRepost).call(TRACK_URN);
    }

    @Test
    public void shouldPublishRepostedEventAfterFailedRemoteRemoval() throws Exception {
        when(removeRepost.toObservable(TRACK_URN)).thenReturn(Observable.just(successfulChange()));
        when(removeRepost.call(TRACK_URN)).thenReturn(successfulChange());
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getFirstUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        TrackProperty.URN.bind(TRACK_URN),
                        TrackProperty.IS_REPOSTED.bind(true)
                )
        );
    }


}