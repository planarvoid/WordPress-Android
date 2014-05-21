package com.soundcloud.android.actionbar;

import static rx.android.OperatorPaged.Page;

import com.soundcloud.android.R;
import com.soundcloud.android.collections.EndlessPagingAdapter;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.RefreshableListComponent;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import javax.inject.Inject;

public class PullToRefreshController {

    private final EventBus eventBus;
    private final PullToRefreshWrapper wrapper;

    private Subscription playerExpandedSubscription = Subscriptions.empty();
    private Subscription refreshSubscription = Subscriptions.empty();
    private boolean wasRefreshing;

    @Inject
    public PullToRefreshController(EventBus eventBus, PullToRefreshWrapper wrapper) {
        this.eventBus = eventBus;
        this.wrapper = wrapper;
    }

    @Deprecated
    public PullToRefreshController(EventBus eventBus) {
        this(eventBus, new PullToRefreshWrapper());
    }

    @Deprecated // this will become a private method once ScListFragment gets removed
    public void onViewCreated(FragmentActivity activity, PullToRefreshLayout pullToRefreshLayout, OnRefreshListener listener) {
        wrapper.attach(activity, pullToRefreshLayout, listener);
        wrapper.setRefreshing(wasRefreshing);
        playerExpandedSubscription = eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerExpandedSubscriber());
    }

    /**
     * Use this overload for fragments that wish to handle refresh logic individually.
     */
    public void onViewCreated(Fragment fragment, OnRefreshListener listener) {
        PullToRefreshLayout pullToRefreshLayout = (PullToRefreshLayout) fragment.getView().findViewById(R.id.ptr_layout);
        onViewCreated(resolveFragmentActivity(fragment), pullToRefreshLayout, listener);
    }

    /**
     * Use this overload for paged list fragments, as it will take care of managing all PTR state.
     */
    public <T extends Parcelable,
            OT extends ConnectableObservable<? extends Page<? extends Iterable<T>>>,
            FragmentT extends Fragment & RefreshableListComponent<OT>>
    void onViewCreated(final FragmentT fragment, OT activeObservable, final EndlessPagingAdapter<T> adapter) {
        this.onViewCreated(fragment, new OnRefreshListener() {
            @Override
            public void onRefreshStarted(View view) {
                OT refreshObservable = fragment.refreshObservable();
                refreshSubscription = refreshObservable.subscribe(new PageSubscriber<T>(adapter));
                fragment.connectObservable(refreshObservable);
            }
        });

        if (wasRefreshing) {
            refreshSubscription = activeObservable.subscribe(new PageSubscriber<T>(adapter));
        }
    }

    private FragmentActivity resolveFragmentActivity(Fragment fragment) {
        return fragment.getParentFragment() == null ? fragment.getActivity() : fragment.getParentFragment().getActivity();
    }

    public void onDestroyView() {
        this.wasRefreshing = isRefreshing();
        playerExpandedSubscription.unsubscribe();
        refreshSubscription.unsubscribe();
        wrapper.detach();
    }

    public boolean isAttached() {
        return wrapper.isAttached();
    }

    public boolean isRefreshing() {
        return wrapper.isRefreshing();
    }

    public void startRefreshing() {
        wrapper.setRefreshing(true);
    }

    public void stopRefreshing() {
        wrapper.setRefreshing(false);
    }

    private final class PlayerExpandedSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (event.getKind() == PlayerUIEvent.PLAYER_EXPANDED) {
                stopRefreshing();
            }
        }
    }

    private final class PageSubscriber<T extends Parcelable> extends DefaultSubscriber<Page<? extends Iterable<T>>> {

        private final EndlessPagingAdapter<T> adapter;

        public PageSubscriber(EndlessPagingAdapter<T> adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onNext(Page<? extends Iterable<T>> page) {
            //TODO: should we signal to the user that no items have been added?
            if (page.getPagedCollection().iterator().hasNext()) {
                adapter.clear();
                adapter.onNext(page);
            }
        }

        @Override
        public void onCompleted() {
            adapter.onCompleted();
            stopRefreshing();
        }

        @Override
        public void onError(Throwable error) {
            //TODO: should we signal to the user that an error occurred?
            stopRefreshing();
            super.onError(error);
        }
    }
}
