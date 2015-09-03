package com.soundcloud.android.collections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.support.v4.app.Fragment;


public class CollectionsPresenterTest extends AndroidUnitTest {

    CollectionsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private CollectionsOperations collectionsOperations;
    @Mock private CollectionsPlaylistOptionsPresenter optionsPresenter;
    @Mock private CollectionsAdapter adapter;
    @Mock private Fragment fragment;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        presenter = new CollectionsPresenter(swipeRefreshAttacher, collectionsOperations, adapter, optionsPresenter, resources(), eventBus);
        when(collectionsOperations.collections(any(CollectionsOptions.class))).thenReturn(Observable.<MyCollections>empty());
    }

    @Test
    public void unsubscribesFromEventBusInOnDestroy() {
        presenter.onCreate(fragment, null);
        presenter.onDestroy(fragment);
        eventBus.verifyUnsubscribed();
    }
}
