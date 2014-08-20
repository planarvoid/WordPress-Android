package com.soundcloud.android.actionbar;

import static rx.android.OperatorPaged.Page;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import rx.Subscription;
import rx.functions.Func1;
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

    private final Func1<PlayerUIEvent, Boolean> isPlayerCollapsed = new Func1<PlayerUIEvent, Boolean>() {
        @Override
        public Boolean call(PlayerUIEvent event) {
            return event.getKind() != PlayerUIEvent.PLAYER_EXPANDED
                    && event.getKind() != PlayerUIEvent.PLAYER_EXPANDING;
        }
    };

    @Inject
    public PullToRefreshController(EventBus eventBus, PullToRefreshWrapper wrapper) {
        this.eventBus = eventBus;
        this.wrapper = wrapper;
    }

    @Deprecated // this will become a private method once ScListFragment gets removed
    public void onViewCreated(FragmentActivity activity, PullToRefreshLayout pullToRefreshLayout, OnRefreshListener listener) {
        wrapper.attach(activity, pullToRefreshLayout, listener);
        wrapper.setRefreshing(wasRefreshing);
        playerExpandedSubscription = eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerExpandingSubscriber());
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
    void onViewCreated(final FragmentT fragment, OT activeObservable, final PagingItemAdapter<T> adapter) {
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
        eventBus.queue(EventQueue.PLAYER_UI)
                .first()
                .filter(isPlayerCollapsed)
                .subscribe(new PlayerCollapsedSubscriber());
    }

    public void stopRefreshing() {
        wrapper.setRefreshing(false);
    }

    private final class PlayerCollapsedSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            wrapper.setRefreshing(true);
        }
    }

    private final class PlayerExpandingSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (event.getKind() == PlayerUIEvent.PLAYER_EXPANDING) {
                stopRefreshing();
            }
        }
    }

    private final class PageSubscriber<T extends Parcelable> extends DefaultSubscriber<Page<? extends Iterable<T>>> {

        private final PagingItemAdapter<T> adapter;

        public PageSubscriber(PagingItemAdapter<T> adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onNext(Page<? extends Iterable<T>> page) {
            adapter.clear();
            adapter.onNext(page);
        }

        @Override
        public void onCompleted() {
            adapter.onCompleted();
            stopRefreshing();
        }

        @Override
        public void onError(Throwable error) {
            stopRefreshing();
            super.onError(error);
        }
    }
}
