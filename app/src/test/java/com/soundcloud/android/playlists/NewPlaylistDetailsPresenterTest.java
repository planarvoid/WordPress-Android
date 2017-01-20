package com.soundcloud.android.playlists;

import static com.soundcloud.android.view.AsyncViewModel.fromIdle;
import static com.soundcloud.android.view.AsyncViewModel.fromRefreshing;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.likes.LikedStatuses;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.AssertableSubscriber;
import rx.subjects.PublishSubject;

import java.util.HashSet;
import java.util.List;

public class NewPlaylistDetailsPresenterTest extends AndroidUnitTest {

    private final TrackItem track1 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
    private final TrackItem track2 = TrackItem.from(ModelFixtures.create(ApiTrack.class));
    private final List<TrackItem> trackItems = asList(track1, track2);

    private final Playlist initialPlaylist = ModelFixtures.playlistBuilder().build();
    private final Playlist updatedPlaylist = initialPlaylist.toBuilder().urn(initialPlaylist.urn()).title("new-title").build();
    private PlaylistDetailsViewModel initialModel;
    private PlaylistDetailsViewModel updatedModel;

    @Mock private PlaylistOperations playlistOperations;
    @Mock private LikesStateProvider likesStateProvider;
    @Mock private SyncInitiator syncInitiator;
    @Mock private TrackRepository trackRepository;
    private EventBus eventBus = new TestEventBus();

    private Urn playlistUrn;
    private NewPlaylistDetailsPresenter newPlaylistPresenter;
    private final PublishSubject<LikedStatuses> likeStates = PublishSubject.create();
    private PublishSubject<SyncJobResult> updateSubject = PublishSubject.create();

    @Before
    public void setUp() throws Exception {
        playlistUrn = initialPlaylist.urn();
        initialModel = PlaylistDetailsViewModel.from(initialPlaylist, trackItems, false, resources());
        updatedModel = initialModel.toBuilder()
                                   .header(PlaylistDetailHeaderItem.from(updatedPlaylist, trackItems, false, resources()))
                                   .build();

        newPlaylistPresenter = new NewPlaylistDetailsPresenter(playlistOperations,
                                                               likesStateProvider,
                                                               syncInitiator,
                                                               eventBus,
                                                               trackRepository,
                                                               resources(),
                                                               playlistUrn);
        when(playlistOperations.playlist(playlistUrn)).thenReturn(just(initialPlaylist), just(updatedPlaylist));
        when(likesStateProvider.likedStatuses()).thenReturn(likeStates);
        when(syncInitiator.syncPlaylist(playlistUrn)).thenReturn(updateSubject);

        when(trackRepository.forPlaylist(playlistUrn)).thenReturn(just(trackItems));
        newPlaylistPresenter.connect();
        emitLikedEntities();
    }

    @Test
    public void emitsPlaylistFromStorage() {
        newPlaylistPresenter.viewModel()
                            .test()
                            .assertValue(fromIdle(PlaylistDetailsViewModel.from(initialPlaylist, trackItems, false, resources())));
    }

    @Test
    public void emitsUpdatedPlaylistAfterLike() {
        final PlaylistDetailHeaderItem likedHeader = initialModel.header().toBuilder().isLikedByUser(true).build();
        final PlaylistDetailsViewModel likedPlaylist = initialModel.toBuilder().header(likedHeader).build();

        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();
        modelUpdates.assertValues(fromIdle(initialModel));

        emitLikedEntities(Urn.forTrack(123L));
        modelUpdates.assertValues(fromIdle(initialModel));

        emitLikedEntities(Urn.forTrack(123L), playlistUrn);
        modelUpdates.assertValues(fromIdle(initialModel), fromIdle(likedPlaylist));
    }

    @Test
    public void emitsUpdatedPlaylistAfterRefresh() {
        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        modelUpdates.assertValues(fromIdle(initialModel));

        newPlaylistPresenter.refresh();
        modelUpdates.assertValues(fromIdle(initialModel), fromRefreshing(initialModel));

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.forUpdate(singleton(updatedPlaylist)));
        updateSubject.onCompleted();
        modelUpdates.assertValues(fromIdle(initialModel),
                                  fromRefreshing(initialModel),
                                  fromRefreshing(updatedModel),
                                  fromIdle(updatedModel));
    }

    @Test
    public void emitsUpdatedModelWhenPlaylistUpdated() {
        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();

        modelUpdates.assertValues(fromIdle(initialModel));

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.forUpdate(singleton(updatedPlaylist)));
        modelUpdates.assertValues(fromIdle(initialModel), fromIdle(updatedModel));
    }

    @Test
    public void ignoredUnrelatedPlaylistsUpdates() {
        final AssertableSubscriber<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates = newPlaylistPresenter.viewModel().test();
        final Playlist unrelatedPlaylist = ModelFixtures.playlist();

        modelUpdates.assertValues(fromIdle(initialModel));

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.forUpdate(singleton(unrelatedPlaylist)));
        modelUpdates.assertValues(fromIdle(initialModel));
    }

    private void emitLikedEntities(Urn... urns) {
        final HashSet<Urn> likedEntities = new HashSet<>(asList(urns));
        likeStates.onNext(new LikedStatuses(likedEntities));
    }

}
