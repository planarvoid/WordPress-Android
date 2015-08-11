package com.soundcloud.android.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Collections;
import java.util.List;

public class UserPostsPresenterTest extends AndroidUnitTest {

    private UserPostsPresenter presenter;

    @Mock private PlaybackOperations playbackOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private UserProfileOperations profileOperations;
    @Mock private MixedPlayableRecyclerItemAdapter adapter;
    @Mock private MixedItemClickListener.Factory mixedClickListenerFactory;
    @Mock private MixedItemClickListener itemClickListener;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;
    @Mock private Fragment fragment;
    @Mock private View fragmentView;
    @Mock private View itemView;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;
    @Mock private TrackItemRenderer trackRenderer;
    @Mock private PlayableListUpdater.Factory playableListUpdaterFactory;
    @Mock private PlayableListUpdater playableListUpdater;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;

    private final Bundle arguments = new Bundle();
    private final Screen screen = Screen.USER_POSTS;
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L));
    private final Urn user = Urn.forUser(123L);

    @Before
    public void setUp() throws Exception {
        when(fragmentView.findViewById(R.id.ak_recycler_view)).thenReturn(recyclerView);
        when(fragmentView.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(fragment.getArguments()).thenReturn(arguments);
        when(mixedClickListenerFactory.create(screen, searchQuerySourceInfo)).thenReturn(itemClickListener);
        when(profileOperations.pagedPostItems(user)).thenReturn(Observable.just(new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), "next-href")));
        when(adapter.getTrackRenderer()).thenReturn(trackRenderer);
        when(playableListUpdaterFactory.create(adapter, trackRenderer)).thenReturn(playableListUpdater);
        when(fragmentView.getResources()).thenReturn(context().getResources());
        when(recyclerView.getResources()).thenReturn(context().getResources());

        arguments.putParcelable(UserPostsFragment.USER_URN_KEY, user);
        arguments.putSerializable(UserPostsFragment.SCREEN_KEY, screen);
        arguments.putParcelable(UserPostsFragment.SEARCH_QUERY_SOURCE_INFO_KEY, searchQuerySourceInfo);
        presenter = new UserPostsPresenter(swipeRefreshAttacher, imagePauseOnScrollListener,
                adapter, mixedClickListenerFactory, playableListUpdaterFactory, profileOperations);
    }

    @Test
    public void presenterUsesMixedPlayableClickListenerToPlayPosts() throws Exception {
        List<PlayableItem> adapterItems = Collections.emptyList();
        when(adapter.getItems()).thenReturn(adapterItems);

        final TrackItem clickedItem = TrackItem.from(TestPropertySets.fromApiTrack());
        when(adapter.getItem(1)).thenReturn(clickedItem);

        final Observable<List<PropertySet>> playbackItems = Observable.just(Collections.<PropertySet>emptyList());
        when(profileOperations.postsForPlayback(adapterItems)).thenReturn(playbackItems);

        presenter.onCreate(fragment, null);

        presenter.onItemClicked(itemView, 1);

        verify(itemClickListener).onPostClick(playbackItems, itemView, 1, clickedItem);
    }

}