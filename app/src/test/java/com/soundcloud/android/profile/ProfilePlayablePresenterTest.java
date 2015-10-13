package com.soundcloud.android.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import org.junit.Before;
import org.junit.Test;
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
    @Mock private EmptyView emptyView;
    @Mock private TrackItemRenderer trackRenderer;
    @Mock private PlayableListUpdater.Factory playableListUpdaterFactory;
    @Mock private PlayableListUpdater playableListUpdater;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private Resources resources;
    @Mock private Drawable divider;
    @Mock private EmptyViewBuilder emptyViewBuilder;

    private final Bundle arguments = new Bundle();
    private final Screen screen = Screen.USER_POSTS;
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L));
    private final Urn user = Urn.forUser(123L);

    @Before
    public void setUp() throws Exception {
        when(fragmentView.findViewById(R.id.ak_recycler_view)).thenReturn(recyclerView);
        when(fragmentView.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(fragmentView.getResources()).thenReturn(resources);
        when(resources.getDrawable(R.drawable.divider_list_grey)).thenReturn(divider);
        when(recyclerView.getResources()).thenReturn(resources);
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

        verify(emptyViewBuilder).configure(emptyView);
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
                return CollectionBinding.from(Observable.empty(), pageTransformer)
                        .withAdapter(adapter).build();
            }

            @Override
            protected EmptyView.Status handleError(Throwable error) {
                return EmptyView.Status.OK;
            }
        };
    }
}
