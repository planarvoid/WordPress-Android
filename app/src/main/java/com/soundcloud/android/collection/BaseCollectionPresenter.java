package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PullToRefreshEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCollectionPresenter extends RecyclerViewPresenter<MyCollection, CollectionItem>
        implements OnboardingItemCellRenderer.Listener {

    public static final String TAG = "CollectionPresenter";
    private final Func1<Object, Boolean> isNotRefreshing = new Func1<Object, Boolean>() {
        @Override
        public Boolean call(Object event) {
            return !swipeRefreshAttacher.isRefreshing();
        }
    };

    private final Action1<Object> logCollectionChanged = new Action1<Object>() {
        @Override
        public void call(Object o) {
            Log.d(TAG, "OnCollectionChanged [event=" + o + ", isNotRefreshing=" + isNotRefreshing + "]");
        }
    };

    private final Action1<MyCollection> clearOnNext = new Action1<MyCollection>() {
        @Override
        public void call(MyCollection myCollection) {
            adapter.clear();
        }
    };

    @VisibleForTesting
    public final Func1<MyCollection, Iterable<CollectionItem>> toCollectionItems =
            new Func1<MyCollection, Iterable<CollectionItem>>() {
                @Override
                public List<CollectionItem> call(MyCollection myCollection) {
                    List<CollectionItem> collectionItems = buildCollectionItems(myCollection);
                    if (showOnboarding() && collectionOptionsStorage.isOnboardingEnabled()) {
                        return collectionWithOnboarding(collectionItems);
                    } else {
                        return collectionItems;
                    }
                }
            };

    private final SwipeRefreshAttacher swipeRefreshAttacher;
    private final EventBus eventBus;
    private final CollectionAdapter adapter;
    private final Resources resources;
    private final CollectionOptionsStorage collectionOptionsStorage;

    private CompositeSubscription eventSubscriptions = new CompositeSubscription();

    public BaseCollectionPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                            EventBus eventBus,
                            CollectionAdapter adapter,
                            Resources resources,
                            CollectionOptionsStorage collectionOptionsStorage) {
        super(swipeRefreshAttacher, Options.defaults());
        this.swipeRefreshAttacher = swipeRefreshAttacher;
        this.eventBus = eventBus;
        this.adapter = adapter;
        this.resources = resources;
        this.collectionOptionsStorage = collectionOptionsStorage;
        adapter.setOnboardingListener(this);
    }

    protected abstract Observable<MyCollection> myCollection();

    protected abstract Observable<MyCollection> updatedMyCollection();

    protected abstract Observable<Object> onCollectionChanged();

    protected abstract List<CollectionItem> buildCollectionItems(MyCollection myCollection);

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        final int spanCount = resources.getInteger(R.integer.collection_grid_span_count);
        final GridLayoutManager layoutManager = new GridLayoutManager(view.getContext(), spanCount);
        layoutManager.setSpanSizeLookup(createSpanSizeLookup(spanCount));
        getRecyclerView().setLayoutManager(layoutManager);
    }

    @Override
    public void onDestroy(Fragment fragment) {
        eventSubscriptions.unsubscribe();
        super.onDestroy(fragment);
    }

    @Override
    protected CollectionBinding<MyCollection, CollectionItem> onBuildBinding(Bundle bundle) {
        final Observable<MyCollection> collections = myCollection().observeOn(AndroidSchedulers.mainThread());
        return CollectionBinding.from(collections.doOnNext(new OnCollectionLoadedAction()), toCollectionItems)
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    protected CollectionBinding<MyCollection, CollectionItem> onRefreshBinding() {
        final Observable<MyCollection> collections =
                updatedMyCollection()
                        .doOnSubscribe(eventBus.publishAction0(EventQueue.TRACKING,
                                                               new PullToRefreshEvent(Screen.COLLECTIONS)))
                        .observeOn(AndroidSchedulers.mainThread());
        return CollectionBinding.from(collections.doOnError(new OnErrorAction()).doOnNext(clearOnNext),
                                      toCollectionItems)
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    public void onCollectionsOnboardingItemClosed(int position) {
        collectionOptionsStorage.disableOnboarding();
        removeItem(position);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    void refreshCollections() {
        final Observable<MyCollection> source = myCollection()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(clearOnNext);
        retryWith(CollectionBinding
                          .from(source, toCollectionItems)
                          .withAdapter(adapter).build());
    }

    protected boolean showOnboarding() {
        return true;
    }

    private GridLayoutManager.SpanSizeLookup createSpanSizeLookup(final int spanCount) {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < adapter.getItemCount() && adapter.getItem(position).isSingleSpan()) {
                    return 1;
                } else {
                    return spanCount;
                }
            }
        };
    }

    private void removeItem(int position) {
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }

    private void showError() {
        Toast.makeText(getRecyclerView().getContext(),
                       R.string.collections_loading_error,
                       Toast.LENGTH_LONG).show();
    }

    private void subscribeForUpdates() {
        eventSubscriptions.unsubscribe();
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.OFFLINE_CONTENT_CHANGED, new UpdateCollectionDownloadSubscriber(adapter)),
                onCollectionChanged()
                        .doOnNext(logCollectionChanged)
                        .filter(isNotRefreshing)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RefreshCollectionsSubscriber())
        );
    }

    private class RefreshCollectionsSubscriber extends DefaultSubscriber<Object> {
        @Override
        public void onNext(Object ignored) {
            refreshCollections();
        }
    }

    private class OnCollectionLoadedAction implements Action1<MyCollection> {
        @Override
        public void call(MyCollection myCollection) {
            adapter.clear();
            subscribeForUpdates();
            if (myCollection.hasError()) {
                showError();
            }
        }
    }

    private class OnErrorAction implements Action1<Throwable> {
        @Override
        public void call(Throwable ignored) {
            showError();
        }
    }

    private List<CollectionItem> collectionWithOnboarding(List<CollectionItem> collectionItems) {
        List<CollectionItem> collectionItemsWithOnboarding = new ArrayList<>(collectionItems.size() + 1);
        collectionItemsWithOnboarding.add(OnboardingCollectionItem.create());
        collectionItemsWithOnboarding.addAll(collectionItems);
        return collectionItemsWithOnboarding;
    }

}
