
package com.soundcloud.android.profile;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.DividerItemDecoration;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class ProfileRecyclerViewPresenterTest {

    private ProfileRecyclerViewPresenter profileRecyclerViewPresenter;

    @Mock private MultiSwipeRefreshLayout swipeRefreshLayout;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private ProfileRecyclerViewScroller recyclerViewScroller;
    @Mock private DividerItemDecoration dividerItemDecoration;
    @Mock private CollectionBinding collectionBinding;
    @Mock private RecyclerItemAdapter adapter;
    @Mock private Fragment fragment;
    @Mock private View fragmentView;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;
    @Mock private Resources resources;
    @Mock private Drawable divider;
    @Captor private ArgumentCaptor<SwipeRefreshLayout.OnRefreshListener> refreshListenerCaptor;

    @Before
    public void setUp() throws Exception {
        profileRecyclerViewPresenter = buildPresenter();
        when(collectionBinding.items()).thenReturn(Observable.empty());
        when(fragmentView.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(fragmentView.findViewById(R.id.ak_recycler_view)).thenReturn(recyclerView);
        when(fragmentView.getResources()).thenReturn(resources);
        when(resources.getDrawable(R.drawable.divider_list_grey)).thenReturn(divider);
        when(collectionBinding.adapter()).thenReturn(adapter);
        when(recyclerView.getAdapter()).thenReturn(adapter);
    }

    @Test
    public void attachingRefreshLayoutBeforeResumedAttachesAfterResuming() throws Exception {
        profileRecyclerViewPresenter.attachRefreshLayout(swipeRefreshLayout);
        verifyZeroInteractions(swipeRefreshAttacher);
        profileRecyclerViewPresenter.onResume(fragment);
        swipeToRefresh();
    }

    @Test
    public void detachingRefreshLayoutBeforeResumedPreventsAttachingAfterResuming() throws Exception {
        final MultiSwipeRefreshLayout refreshLayout = mock(MultiSwipeRefreshLayout.class);
        profileRecyclerViewPresenter.attachRefreshLayout(refreshLayout);

        profileRecyclerViewPresenter.detachRefreshLayout();
        profileRecyclerViewPresenter.onResume(fragment);

        verify(swipeRefreshAttacher, never()).attach(
                any(SwipeRefreshLayout.OnRefreshListener.class),
                any(MultiSwipeRefreshLayout.class), same(recyclerView), same(emptyView));
    }

    @Test
    public void onViewCreatedSetsViewsOnScroller() throws Exception {
        profileRecyclerViewPresenter.onCreate(fragment, null);
        profileRecyclerViewPresenter.onViewCreated(fragment, fragmentView, null);

        verify(recyclerViewScroller).setViews(recyclerView, emptyView);
    }

    @Test
    public void onDestroyViewClearsViewsOnScroller() throws Exception {
        profileRecyclerViewPresenter.onCreate(fragment, null);
        profileRecyclerViewPresenter.onViewCreated(fragment, fragmentView, null);
        profileRecyclerViewPresenter.onDestroyView(fragment);

        verify(recyclerViewScroller).clearViews();
    }

    private void swipeToRefresh() {
        verify(swipeRefreshAttacher).attach(
                refreshListenerCaptor.capture(), same(swipeRefreshLayout), same(recyclerView), same(emptyView));
        refreshListenerCaptor.getValue().onRefresh();
    }

    private ProfileRecyclerViewPresenter buildPresenter() {
        return new ProfileRecyclerViewPresenter(swipeRefreshAttacher, imagePauseOnScrollListener, recyclerViewScroller) {
            @Override
            protected CollectionBinding onBuildBinding(Bundle fragmentArgs) {
                return collectionBinding;
            }

            @Override
            protected void onItemClicked(View view, int position) {

            }

            @Override
            protected EmptyView.Status handleError(Throwable error) {
                return EmptyView.Status.OK;
            }

            @Override
            public RecyclerView getRecyclerView() {
                return recyclerView;
            }

            @Override
            public EmptyView getEmptyView() {
                return emptyView;
            }
        };
    }
}