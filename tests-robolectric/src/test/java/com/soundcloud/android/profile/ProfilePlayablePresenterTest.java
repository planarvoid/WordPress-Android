package com.soundcloud.android.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.RecyclerViewPauseOnScrollListener;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.Pager;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedPlayableItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerViewAdapter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
// TODO : Extend New List Presenter test
public class ProfilePlayablePresenterTest {

    public static final String EMPTY_TEXT = "empty-text";
    private ProfilePlayablePresenter presenter;

    @Mock private PlaybackOperations playbackOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private PullToRefreshWrapper pullToRefreshWrapper;
    @Mock private ProfileOperations profileOperations;
    @Mock private MixedPlayableRecyclerViewAdapter adapter;
    @Mock private MixedPlayableItemClickListener.Factory mixedClickListenerFactory;
    @Mock private MixedPlayableItemClickListener itemClickListener;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;
    @Mock private Fragment fragment;
    @Mock private View fragmentView;
    @Mock private View itemView;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;
    @Mock private TrackItemRenderer trackRenderer;
    @Mock private PlayableListUpdater.Factory playableListUpdaterFactory;
    @Mock private PlayableListUpdater playableListUpdater;
    @Mock private RecyclerViewPauseOnScrollListener pauseOnScrollListener;
    @Mock private Resources resources;
    @Mock private Drawable divider;

    private final Bundle arguments = new Bundle();
    private final Screen screen = Screen.USER_POSTS;
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L));
    private final Urn user = Urn.forUser(123L);

    @Before
    public void setUp() throws Exception {
        when(fragmentView.findViewById(R.id.recycler_view)).thenReturn(recyclerView);
        when(fragmentView.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(fragmentView.getResources()).thenReturn(resources);
        when(resources.getDrawable(R.drawable.divider_list_grey)).thenReturn(divider);
        when(fragment.getArguments()).thenReturn(arguments);
        when(mixedClickListenerFactory.create(screen, searchQuerySourceInfo)).thenReturn(itemClickListener);
        when(adapter.getTrackRenderer()).thenReturn(trackRenderer);
        when(playableListUpdaterFactory.create(adapter, trackRenderer)).thenReturn(playableListUpdater);

        arguments.putParcelable(UserPostsFragment.USER_URN_KEY, user);
        arguments.putSerializable(UserPostsFragment.SCREEN_KEY, screen);
        arguments.putParcelable(UserPostsFragment.SEARCH_QUERY_SOURCE_INFO_KEY, searchQuerySourceInfo);
        createPresenter();
    }

    @Test
    public void presenterUsesMixedPlayableClickListener() throws Exception {
        List<PlayableItem> items = Collections.emptyList();
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);

        presenter.onItemClicked(itemView, 1);

        verify(itemClickListener).onItemClick(items, itemView, 1);
    }

    @Test
    public void configuresEmptyViewInOnViewCreated() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);

        verify(emptyView).setMessageText(EMPTY_TEXT);
    }

    private void createPresenter() {
        presenter = new ProfilePlayablePresenter(pullToRefreshWrapper, pauseOnScrollListener, adapter, mixedClickListenerFactory, playableListUpdaterFactory, profileOperations) {
            @Override
            protected int handleError(Throwable error) {
                return 0;
            }

            @Override
            protected Pager.PagingFunction<PagedRemoteCollection> getPagingFunction() {
                return null;
            }

            @Override
            protected Observable<PagedRemoteCollection> getPagedObservable(Urn userUrn) {
                return Observable.just(new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), "next-href"));
            }

            @Override
            protected void configureEmptyView(EmptyView emptyView) {
                emptyView.setMessageText(EMPTY_TEXT);
            }
        };
    }
}