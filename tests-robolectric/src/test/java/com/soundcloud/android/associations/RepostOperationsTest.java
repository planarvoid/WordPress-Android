package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static com.soundcloud.android.testsupport.fixtures.TestStorageResults.successfulChange;
import static com.soundcloud.android.testsupport.fixtures.TestStorageResults.successfulInsert;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestApiResponses;
import com.soundcloud.android.testsupport.fixtures.TestStorageResults;
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
    @Mock private ApiScheduler apiScheduler;
    @Mock private WriteResult writeResult;

    @Before
    public void setUp() throws Exception {
        operations = new RepostOperations(repostStorage, apiScheduler, eventBus);
    }

    @Test
    public void shouldStoreTrackRepostAndPushToApi() throws Exception {
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(Observable.just(successfulInsert()));
        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(Observable.<ChangeResult>never());
        when(apiScheduler.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        expect(testObserver.getOnNextEvents()).toContainExactly(PropertySet.from(
                PlayableProperty.URN.bind(TRACK_URN),
                PlayableProperty.IS_REPOSTED.bind(true)
        ));
    }

    @Test
    public void shouldStorePlaylistRepostAndPushToApi() throws Exception {
        when(repostStorage.addRepost(PLAYLIST_URN)).thenReturn(Observable.just(successfulInsert()));
        when(repostStorage.removeRepost(PLAYLIST_URN)).thenReturn(Observable.<ChangeResult>never());
        when(apiScheduler.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/playlist_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(PLAYLIST_URN, true).subscribe(testObserver);

        expect(testObserver.getOnNextEvents()).toContainExactly(PropertySet.from(
                PlayableProperty.URN.bind(PLAYLIST_URN),
                PlayableProperty.IS_REPOSTED.bind(true)
        ));
    }

    @Test
    public void shouldPublishEntityChangedEventAfterSuccesfulPushToApi() throws Exception {
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(Observable.just(successfulInsert()));
        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(Observable.<ChangeResult>never());
        when(apiScheduler.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getNextUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        PlayableProperty.URN.bind(TRACK_URN),
                        PlayableProperty.IS_REPOSTED.bind(true)
                )
        );
    }

    @Test
    public void shouldNotPushRepostToApiIfStorageCallFailed() throws Exception {
        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(Observable.<ChangeResult>never());
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(Observable.<InsertResult>error(mock(PropellerWriteException.class)));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        verifyZeroInteractions(apiScheduler);
    }

    @Test
    public void shouldPublishNotRepostedEventIfInsertFailed() throws Exception {
        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(Observable.just(successfulChange()));
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(Observable.<InsertResult>error(mock(PropellerWriteException.class)));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getNextUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        TrackProperty.URN.bind(TRACK_URN),
                        TrackProperty.IS_REPOSTED.bind(false)
                )
        );
    }

    @Test
    public void shouldRollbackStoredRepostAfterFailedApiCall() throws Exception {
        PublishSubject<ChangeResult> repostRemoval = PublishSubject.create();

        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(repostRemoval);
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(Observable.just(successfulInsert()));
        when(apiScheduler.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        expect(repostRemoval.hasObservers()).toBeTrue();
    }

    @Test
    public void shouldPublishNotRepostedEventAfterFailedApiCall() throws Exception {
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(Observable.just(successfulInsert()));
        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(Observable.just(successfulChange()));
        when(apiScheduler.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getNextUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        TrackProperty.URN.bind(TRACK_URN),
                        TrackProperty.IS_REPOSTED.bind(false)
                )
        );
    }

    @Test
    public void shouldStoreTrackUnpostAndPushToApi() throws Exception {
        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(Observable.just(successfulChange()));
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(Observable.<InsertResult>never());
        when(apiScheduler.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        expect(testObserver.getOnNextEvents()).toContainExactly(PropertySet.from(
                PlayableProperty.URN.bind(TRACK_URN),
                PlayableProperty.IS_REPOSTED.bind(false)
        ));
    }

    @Test
    public void shouldStorePlaylistUnpostAndPushToApi() throws Exception {
        when(repostStorage.removeRepost(PLAYLIST_URN)).thenReturn(Observable.just(successfulChange()));
        when(repostStorage.addRepost(PLAYLIST_URN)).thenReturn(Observable.<InsertResult>never());
        when(apiScheduler.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/playlist_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(PLAYLIST_URN, false).subscribe(testObserver);

        expect(testObserver.getOnNextEvents()).toContainExactly(PropertySet.from(
                PlayableProperty.URN.bind(PLAYLIST_URN),
                PlayableProperty.IS_REPOSTED.bind(false)
        ));
    }

    @Test
    public void shouldPublishEntityChangedEventForUnpostAfterSuccesfulPushToApi() throws Exception {
        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(Observable.just(successfulChange()));
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(Observable.<InsertResult>never());

        when(apiScheduler.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getNextUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        PlayableProperty.URN.bind(TRACK_URN),
                        PlayableProperty.IS_REPOSTED.bind(false)
                )
        );
    }

    @Test
    public void shouldNotDeleteRepostFromApiIfStorageCallFailed() throws Exception {
        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(Observable.<ChangeResult>error(mock(PropellerWriteException.class)));
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(Observable.<InsertResult>never());

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        verifyZeroInteractions(apiScheduler);
    }

    @Test
    public void shouldPublishRepostedEventIfRepostRemovalFailed() throws Exception {
        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(Observable.<ChangeResult>error(mock(PropellerWriteException.class)));
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(Observable.just(TestStorageResults.successfulInsert()));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getNextUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        TrackProperty.URN.bind(TRACK_URN),
                        TrackProperty.IS_REPOSTED.bind(true)
                )
        );
    }

    @Test
    public void shouldRollbackRepostRemovalAfterFailedRemoteRemoval() throws Exception {
        PublishSubject<InsertResult> repostInsertion = PublishSubject.create();

        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(Observable.just(successfulChange()));
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(repostInsertion);
        when(apiScheduler.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        expect(repostInsertion.hasObservers()).toBeTrue();
    }

    @Test
    public void shouldPublishRepostedEventAfterFailedRemoteRemoval() throws Exception {
        when(repostStorage.addRepost(TRACK_URN)).thenReturn(Observable.just(successfulInsert()));
        when(repostStorage.removeRepost(TRACK_URN)).thenReturn(Observable.just(successfulChange()));
        when(apiScheduler.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.REPOST);
        expect(event.getNextUrn()).toEqual(TRACK_URN);
        expect(event.getChangeMap().get(TRACK_URN)).toEqual(
                PropertySet.from(
                        TrackProperty.URN.bind(TRACK_URN),
                        TrackProperty.IS_REPOSTED.bind(true)
                )
        );
    }


}