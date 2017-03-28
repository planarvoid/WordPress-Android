package com.soundcloud.android.playlists;


import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.reverse;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.BaseIntegrationTest;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.hamcrest.TestAsyncState;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.Supplier;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NewPlaylistDetailsPresenterIntegrationTest extends BaseIntegrationTest {

    public NewPlaylistDetailsPresenterIntegrationTest() {
        super(TestUser.playlistUser, Flag.NEW_PLAYLIST_SCREEN);
    }

    @Test
    public void presenterDoesNotEmitWhenNotConnected() {
        noNetwork();

        final NewPlaylistDetailsPresenter presenter = createPresenter(Urn.forPlaylist(123L));
        final Screen screen = new Screen(presenter);

        screen.assertState(empty());
    }

    @Test
    public void presenterStartsWithEmptyModel() {
        unrespondingNetwork();

        final NewPlaylistDetailsPresenter presenter = createPresenter(Urn.forPlaylist(123L));
        final Screen screen = new Screen(presenter);

        presenter.connect();

        screen.assertState(contains(AsyncViewModel.create(Optional.absent(), true, false, Optional.absent())));
    }

    @Test
    public void showNetworkError() throws InterruptedException {
        noNetwork();

        final NewPlaylistDetailsPresenter presenter = createPresenter(Urn.forPlaylist(123L));
        final Screen screen = new Screen(presenter);

        presenter.connect();

        screen.assertState(contains(AsyncViewModel.create(Optional.absent(), true, false, Optional.absent()),
                                    AsyncViewModel.create(Optional.absent(), false, false, Optional.of(ViewError.CONNECTION_ERROR))));
    }

    @Test
    public void showPlaylistWithTracks() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        final NewPlaylistDetailsPresenter presenter = createPresenter(playlistUrn);
        final Screen screen = new Screen(presenter);

        presenter.connect();

        screen.assertLastState(this::lastPlaylistUrn, is(playlistUrn));
        screen.assertLastState(AsyncViewModel::isRefreshing, is(false));
        screen.assertLastState(state -> state.data().get().tracks(), not(empty()));
    }

    @Test
    public void editTrackList() {
        final Urn playlistWith2Tracks = Urn.forPlaylist(116114846L);
        final NewPlaylistDetailsPresenter presenter = createPresenter(playlistWith2Tracks);
        final Screen screen = new Screen(presenter);

        presenter.connect();

        screen.assertLastState(this::lastPlaylistUrn, is(playlistWith2Tracks));
        screen.assertLastState(state -> state.data().get().tracks().size(), greaterThan(1));

        final List<PlaylistDetailTrackItem> tracks = screen.currentState().data().get().tracks();
        final List<PlaylistDetailTrackItem> updatedTrackList = reverse(newArrayList(tracks));

        presenter.actionUpdateTrackList(updatedTrackList);

        screen.assertLastState(state -> state.data().get().tracks(), is(updatedTrackList));
    }

    static class Screen extends TestAsyncState<AsyncViewModel<PlaylistDetailsViewModel>> {

        final List<AsyncViewModel<PlaylistDetailsViewModel>> models = new ArrayList<>();

        Screen(NewPlaylistDetailsPresenter presenter) {
            presenter.viewModel().subscribe(this::updateModel);
        }

        public AsyncViewModel<PlaylistDetailsViewModel> currentState() {
            return Iterables.getLast(models);
        }

        public void assertContentLoaded() {
            assertLastState(state -> !state.isRefreshing() && hasTracks(state.data()), is(true));
        }

        public void resetState() {
            models.clear();
        }

        private boolean hasTracks(Optional<PlaylistDetailsViewModel> data) {
            return data.isPresent() && !data.get().tracks().isEmpty();
        }

        private void updateModel(AsyncViewModel<PlaylistDetailsViewModel> update) {
            models.add(update);
        }

        @Override
        public Supplier<List<AsyncViewModel<PlaylistDetailsViewModel>>> states() {
            return () -> models;
        }
    }

    private NewPlaylistDetailsPresenter createPresenter(Urn playlistUrn) {
        final String screen = "fake-screen";
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("query"), "query");
        final PromotedSourceInfo promotedSourceInfo = new PromotedSourceInfo("add urn", new Urn("promoted item"), Optional.absent(), emptyList());

        NewPlaylistDetailsPresenterFactory presenterFactory = SoundCloudApplication.getObjectGraph().newPlaylistDetailsPresenterFactory();
        return presenterFactory.create(playlistUrn, screen, searchQuerySourceInfo, promotedSourceInfo);
    }

    private Urn lastPlaylistUrn(AsyncViewModel<PlaylistDetailsViewModel> state) {
        final Optional<PlaylistDetailsViewModel> data = state.data();
        if (data.isPresent()) {
            return data.get().metadata().urn();
        } else {
            return Urn.NOT_SET;
        }
    }
}
