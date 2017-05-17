package com.soundcloud.android.playlists;


import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.reverse;
import static com.soundcloud.java.optional.Optional.absent;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.BaseIntegrationTest;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.hamcrest.TestAsyncState;
import com.soundcloud.android.model.Urn;
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
public class PlaylistDetailsPresenterIntegrationTest extends BaseIntegrationTest {

    public PlaylistDetailsPresenterIntegrationTest() {
        super(TestUser.playlistUser);
    }

    @Test
    public void presenterDoesNotEmitWhenNotConnected() {
        noNetwork();

        final PlaylistDetailsPresenter presenter = createPresenter();
        final Screen screen = new Screen(presenter);

        screen.assertState(empty());
    }

    @Test
    public void presenterStartsWithEmptyModel() {
        unrespondingNetwork();

        final PlaylistDetailsPresenter presenter = createPresenter();
        final Screen screen = new Screen(presenter);

        presenter.connect(PlaylistDetailsInputs.create(), Urn.forPlaylist(123L));

        screen.assertState(contains(AsyncViewModel.<Object>builder()
                                            .data(Optional.absent())
                                            .isLoadingNextPage(true)
                                            .isRefreshing(false)
                                            .error(Optional.absent())
                                            .refreshError(absent())
                                            .build()));
    }

    @Test
    public void showNetworkError() throws InterruptedException {
        noNetwork();

        final PlaylistDetailsPresenter presenter = createPresenter();
        final Screen screen = new Screen(presenter);

        presenter.connect(PlaylistDetailsInputs.create(), Urn.forPlaylist(123L));

        screen.assertState(contains(AsyncViewModel.<Object>builder()
                                            .data(Optional.absent())
                                            .isLoadingNextPage(true)
                                            .isRefreshing(false)
                                            .error(Optional.absent())
                                            .refreshError(absent())
                                            .build(),
                                    AsyncViewModel.<Object>builder()
                                            .data(Optional.absent())
                                            .isLoadingNextPage(false)
                                            .isRefreshing(false)
                                            .error(Optional.of(ViewError.CONNECTION_ERROR))
                                            .refreshError(absent())
                                            .build()));
    }

    @Test
    public void showPlaylistWithTracks() {
        final Urn playlistUrn = Urn.forPlaylist(123L);
        final PlaylistDetailsPresenter presenter = createPresenter();
        final Screen screen = new Screen(presenter);

        presenter.connect(PlaylistDetailsInputs.create(), playlistUrn);

        screen.assertLastState(this::lastPlaylistUrn, equalTo(playlistUrn));
        screen.assertLastState(AsyncViewModel::isRefreshing, equalTo(false));
        screen.assertLastState(state -> state.data().get().tracks(), not(empty()));
    }

    @Test
    @Ignore
    public void editTrackList() {
        final Urn playlistWith2Tracks = Urn.forPlaylist(116114846L);
        final PlaylistDetailsPresenter presenter = createPresenter();
        final Screen screen = new Screen(presenter);

        final PlaylistDetailsInputs inputs = PlaylistDetailsInputs.create();
        presenter.connect(inputs, playlistWith2Tracks);

        screen.assertLastState(this::lastPlaylistUrn, equalTo(playlistWith2Tracks));
        screen.assertLastState(state -> state.data().get().tracks().size(), greaterThan(1));

        final List<PlaylistDetailTrackItem> tracks = screen.currentState().data().get().tracks();
        final List<PlaylistDetailTrackItem> updatedTrackList = reverse(newArrayList(tracks));

        inputs.actionUpdateTrackList(updatedTrackList);

        screen.assertLastState(state -> state.data().get().tracks(), equalTo(updatedTrackList));
    }

    private PlaylistDetailsPresenter createPresenter() {
        final String screen = "fake-screen";
        final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("query"), "query");
        final PromotedSourceInfo promotedSourceInfo = new PromotedSourceInfo("add urn", new Urn("promoted item"), Optional.absent(), emptyList());

        PlaylistDetailsPresenterFactory presenterFactory = SoundCloudApplication.getObjectGraph().playlistDetailsPresenterFactory();
        return presenterFactory.create(screen, searchQuerySourceInfo, promotedSourceInfo);
    }

    private Urn lastPlaylistUrn(AsyncViewModel<PlaylistDetailsViewModel> state) {
        final Optional<PlaylistDetailsViewModel> data = state.data();
        if (data.isPresent()) {
            return data.get().metadata().urn();
        } else {
            return Urn.NOT_SET;
        }
    }

    static class Screen extends TestAsyncState<AsyncViewModel<PlaylistDetailsViewModel>> {

        final List<AsyncViewModel<PlaylistDetailsViewModel>> models = new ArrayList<>();

        Screen(PlaylistDetailsPresenter presenter) {
            presenter.viewModel().subscribe(this::updateModel);
        }

        public AsyncViewModel<PlaylistDetailsViewModel> currentState() {
            return Iterables.getLast(models);
        }

        public void assertContentLoaded() {
            assertLastState(state -> !state.isRefreshing() && hasTracks(state.data()), equalTo(true));
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
}

