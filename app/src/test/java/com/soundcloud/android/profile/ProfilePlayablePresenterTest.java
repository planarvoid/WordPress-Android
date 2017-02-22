package com.soundcloud.android.profile;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlayableWithReposter;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;

import java.util.List;

public class ProfilePlayablePresenterTest extends AndroidUnitTest {

    private ProfilePlayablePresenter presenter;

    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private ImageOperations imageOperations;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private MixedPlayableRecyclerItemAdapter adapter;
    @Mock private MixedItemClickListener.Factory mixedClickListenerFactory;
    @Mock private MixedItemClickListener itemClickListener;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;
    @Mock private Fragment fragment;
    @Mock private View fragmentView;
    @Mock private View itemView;
    @Mock private RecyclerView recyclerView;
    @Mock private SimpleItemAnimator itemAnimator;
    @Mock private EmptyView emptyView;
    @Mock private TrackItemRenderer trackRenderer;
    @Mock private PlayableListUpdater.Factory playableListUpdaterFactory;
    @Mock private PlayableListUpdater playableListUpdater;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private Resources resources;
    @Mock private Drawable divider;
    @Mock private EmptyViewBuilder emptyViewBuilder;
    @Captor private ArgumentCaptor<Observable<List<PlayableItem>>> playableItemArgumentCaptor;
    @Captor private ArgumentCaptor<Observable<List<PlayableWithReposter>>> playableWithReposterArgumentCaptor;

    private final Bundle arguments = new Bundle();
    private final Screen screen = Screen.USER_POSTS;
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
    private final Urn user = Urn.forUser(123L);

    @Before
    public void setUp() throws Exception {
        when(fragmentView.findViewById(R.id.ak_recycler_view)).thenReturn(recyclerView);
        when(fragmentView.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(fragmentView.getResources()).thenReturn(resources);
        when(resources.getDrawable(R.drawable.ak_list_divider_item)).thenReturn(divider);
        when(recyclerView.getResources()).thenReturn(resources);
        when(recyclerView.getItemAnimator()).thenReturn(itemAnimator);
        when(recyclerView.getAdapter()).thenReturn(adapter);
        when(fragment.getArguments()).thenReturn(arguments);
        when(mixedClickListenerFactory.create(screen, searchQuerySourceInfo)).thenReturn(itemClickListener);
        when(adapter.getTrackRenderer()).thenReturn(trackRenderer);
        when(playableListUpdaterFactory.create(adapter, trackRenderer)).thenReturn(playableListUpdater);

        arguments.putParcelable(UserFollowingsFragment.USER_URN_KEY, user);
        arguments.putSerializable(UserFollowingsFragment.SCREEN_KEY, screen);
        arguments.putParcelable(UserFollowingsFragment.SEARCH_QUERY_SOURCE_INFO_KEY, searchQuerySourceInfo);
        createPresenter();
        presenter.onCreate(fragment, null);
    }

    @Test
    public void presenterUsesMixedPlayableClickListenerForProfilePost() throws Exception {
        TrackItem trackItem = PlayableFixtures.expectedTrackForListItem(Urn.forTrack(123L));
        when(adapter.getItem(1)).thenReturn(trackItem);

        presenter.onItemClicked(itemView, 1);

        verify(itemClickListener).onProfilePostClick(playableItemArgumentCaptor.capture(), same(itemView), eq(1),
                                                     same((ListItem) trackItem), same(trackItem.creatorUrn()));
    }

    @Test
    public void presenterUsesMixedPlayableClickListenerForPlaylistPost() throws Exception {
        PlaylistItem playlistItem = PlayableFixtures.expectedPostedPlaylistForPostsScreen();
        when(adapter.getItem(1)).thenReturn(playlistItem);

        presenter.onItemClicked(itemView, 1);

        verify(itemClickListener).legacyOnPostClick(playableWithReposterArgumentCaptor.capture(), same(itemView), eq(1),
                                                    same((ListItem) playlistItem));
    }

    @Test
    public void configuresEmptyViewInOnViewCreated() throws Exception {
        presenter.onViewCreated(fragment, fragmentView, null);

        verify(emptyViewBuilder).configure(emptyView);
    }

    @Test
    public void resumesImageLoadingOnViewDestroy() {
        presenter.onViewCreated(fragment, fragmentView, null);

        presenter.onDestroyView(fragment);

        verify(imagePauseOnScrollListener).resume();
    }

    private void createPresenter() {
        presenter = new ProfilePlayablePresenter(swipeRefreshAttacher, imagePauseOnScrollListener,
                                                 adapter, mixedClickListenerFactory, playableListUpdaterFactory) {

            @Override
            public void bind(Object o) {

            }

            @Override
            protected void configureEmptyView(EmptyView emptyView) {
                emptyViewBuilder.configure(emptyView);
            }

            @Override
            protected CollectionBinding onBuildBinding(Bundle bundle) {
                return CollectionBinding.from(Observable.empty())
                                        .withAdapter((ItemAdapter) adapter).build();
            }

            @Override
            protected EmptyView.Status handleError(Throwable error) {
                return EmptyView.Status.OK;
            }
        };
    }
}
