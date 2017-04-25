package com.soundcloud.android.discovery;


import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.EmptyView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observer;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.List;

public class DiscoveryPresenterTest extends AndroidUnitTest {

    @Mock private Fragment fragment;
    @Mock private Bundle bundle;
    @Mock private Navigator navigator;
    @Mock private DiscoveryAdapter adapter;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private Observer<Iterable<DiscoveryCard>> itemObserver;
    @Mock private Activity activity;

    private DiscoveryPresenter presenter;

    @Before
    public void setUp() {
        presenter = new DiscoveryPresenter(swipeRefreshAttacher, adapter, navigator);
    }

    @Test
    public void onCreateSetsSearchListener() {
        presenter.onCreate(fragment, bundle);
        verify(adapter).setSearchListener(presenter);
    }

    @Test
    public void onCreateAddsSearchItemToAdapter() {
        CollectionBinding<List<DiscoveryCard>, DiscoveryCard> binding = presenter.onBuildBinding(bundle);
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(singletonList(DiscoveryCard.forSearchItem()));
    }

    @Test
    public void onRefreshAddsSearchItemToAdapter() {
        CollectionBinding<List<DiscoveryCard>, DiscoveryCard> binding = presenter.onRefreshBinding();
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(singletonList(DiscoveryCard.forSearchItem()));
    }

    @Test
    public void navigatesToSearchWhenClicked() {
        presenter.onSearchClicked(activity);

        verify(navigator).openSearch(activity);
    }

    @Test
    public void handlesError() {
        final Exception exception = new RuntimeException();
        final EmptyView.Status result = presenter.handleError(exception);

        assertThat(result).isEqualTo(EmptyView.Status.ERROR);
    }
}
