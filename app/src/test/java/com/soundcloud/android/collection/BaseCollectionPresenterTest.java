package com.soundcloud.android.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.content.res.Resources;
import android.support.v4.app.Fragment;

import java.util.Collections;
import java.util.List;

public class BaseCollectionPresenterTest extends AndroidUnitTest {

    private static final MyCollection MY_COLLECTION = mock(MyCollection.class);

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private CollectionOperations collectionOperations;
    @Mock private CollectionOptionsStorage collectionOptionsStorage;
    @Mock private CollectionAdapter adapter;
    @Mock private Fragment fragment;

    private TestEventBus eventBus = new TestEventBus();

    private BaseCollectionPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(collectionOperations.collections(null)).thenReturn(Observable.<MyCollection>empty());
        when(collectionOperations.updatedCollections(null)).thenReturn(Observable.<MyCollection>empty());
        when(collectionOperations.onCollectionChanged()).thenReturn(Observable.empty());
        presenter = new TestCollectionPresenter(swipeRefreshAttacher,
                                                eventBus,
                                                adapter,
                                                resources(),
                                                collectionOptionsStorage);
    }

    @Test
    public void onCollectionChangedShouldNotRefreshUntilAfterFirstLoad() {
        final PublishSubject<Object> collectionSyncedBus = PublishSubject.create();
        setupOnCollectionChanged(collectionSyncedBus, Observable.<MyCollection>empty());

        collectionSyncedBus.onNext(null);

        verify(collectionOperations, never()).collections(null);
    }

    @Test
    public void onCollectionChangedShouldRefresh() {
        final PublishSubject<Object> collectionSyncedBus = PublishSubject.create();
        setupOnCollectionChanged(collectionSyncedBus, Observable.just(MY_COLLECTION));

        collectionSyncedBus.onNext(null);

        verify(collectionOperations).collections(null);
    }

    @Test
    public void onCollectionChangedShouldNotRefreshWhenAlreadyRefreshing() {
        final PublishSubject<Object> collectionSyncedBus = PublishSubject.create();
        setupOnCollectionChanged(collectionSyncedBus, Observable.just(MY_COLLECTION));
        when(swipeRefreshAttacher.isRefreshing()).thenReturn(true);

        collectionSyncedBus.onNext(null);

        verify(collectionOperations, never()).collections(null);
    }

    @Test
    public void shouldAddOnboardingWhenEnabled() {
        when(collectionOptionsStorage.isOnboardingEnabled()).thenReturn(true);

        presenter.onCreate(fragment, null);

        assertThat(presenter.toCollectionItems.call(MY_COLLECTION)).containsExactly(
                OnboardingCollectionItem.create()
        );
    }

    @Test
    public void shouldDisableOnboardingWhenClosed() {
        presenter.onCollectionsOnboardingItemClosed(0);

        verify(collectionOptionsStorage).disableOnboarding();
    }

    @Test
    public void shouldRemoveOnboardingWhenClosed() {
        presenter.onCollectionsOnboardingItemClosed(1);

        verify(adapter).removeItem(1);
    }

    private void setupOnCollectionChanged(PublishSubject<Object> subject, Observable<MyCollection> initial) {
        when(collectionOperations.onCollectionChanged()).thenReturn(subject);
        when(collectionOperations.collections(null)).thenReturn(initial);

        presenter.onCreate(fragment, null);
        reset(collectionOperations);

        when(collectionOperations.collections(null)).thenReturn(Observable.just(MY_COLLECTION));
    }

    /* A test CollectionPresenter that does nothing by itself */
    private class TestCollectionPresenter extends BaseCollectionPresenter {
        TestCollectionPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                EventBus eventBus,
                                CollectionAdapter adapter,
                                Resources resources,
                                CollectionOptionsStorage collectionOptionsStorage) {
            super(swipeRefreshAttacher, eventBus, adapter, resources, collectionOptionsStorage);
        }

        @Override
        public Observable<MyCollection> myCollection() {
            return collectionOperations.collections(null);
        }

        @Override
        public Observable<MyCollection> updatedMyCollection() {
            return collectionOperations.updatedCollections(null);
        }

        @Override
        public Observable<Object> onCollectionChanged() {
            return collectionOperations.onCollectionChanged();
        }

        @Override
        public List<CollectionItem> buildCollectionItems(MyCollection myCollection) {
            return Collections.emptyList();
        }
    }
}
