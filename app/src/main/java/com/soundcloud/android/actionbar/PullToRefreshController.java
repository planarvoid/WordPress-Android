package com.soundcloud.android.actionbar;

import static rx.android.OperatorPaged.Page;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.main.DefaultFragmentLifeCycle;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import javax.inject.Inject;

public class PullToRefreshController extends DefaultFragmentLifeCycle<Fragment> {

    private final EventBus eventBus;
    private final PullToRefreshWrapper wrapper;

    private Subscription playerExpandedSubscription = Subscriptions.empty();
    private Subscription refreshSubscription = Subscriptions.empty();
    private boolean wasRefreshing;

    private OnRefreshListener refreshListener;
    private Fragment fragment;
    private PagingItemAdapter adapter;

    private final Func1<PlayerUIEvent, Boolean> playerIsNotExpanded = new Func1<PlayerUIEvent, Boolean>() {
        @Override
        public Boolean call(PlayerUIEvent event) {
            return event.getKind() != PlayerUIEvent.PLAYER_EXPANDED;
        }
    };

    @Inject
    public PullToRefreshController(EventBus eventBus, PullToRefreshWrapper wrapper) {
        this.eventBus = eventBus;
        this.wrapper = wrapper;
    }

    public void setAdapter(PagingItemAdapter adapter) {
        this.adapter = adapter;
    }

    public void setRefreshListener(OnRefreshListener refreshListener) {
        this.refreshListener = refreshListener;
    }

    @Override
    public void onBind(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        PullToRefreshLayout pullToRefreshLayout;
        if (view instanceof PullToRefreshLayout) {
            // this is only used for ScListFragment right now
            pullToRefreshLayout = (PullToRefreshLayout) view;
        } else {
            pullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
        }
        if (refreshListener == null && fragment instanceof RefreshableListComponent) {
            refreshListener = new OnRefreshListener() {
                @Override
                public void onRefreshStarted(View view) {
                    RefreshableListComponent component = (RefreshableListComponent) fragment;
                    final Observable refreshObservable = component.refreshObservable();
                    refreshSubscription = refreshObservable.subscribe(new PageSubscriber(adapter));
                    component.connectObservable(refreshObservable);
                }
            };
        }

        wrapper.attach(resolveFragmentActivity(), pullToRefreshLayout, refreshListener);
        wrapper.setRefreshing(wasRefreshing);
        playerExpandedSubscription = eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerExpandedSubscriber());
    }

    /**
     * Use this overload for paged list fragments, as it will take care of managing all PTR state.
     */
    public <T extends Parcelable, OT extends Observable<? extends Page<? extends Iterable<T>>>>
    void connect(OT activeObservable, final PagingItemAdapter<T> adapter) {
        if (wasRefreshing) {
            refreshSubscription = activeObservable.subscribe(new PageSubscriber(adapter));
        }
    }

    private FragmentActivity resolveFragmentActivity() {
        return fragment.getParentFragment() == null
                ? fragment.getActivity()
                : fragment.getParentFragment().getActivity();
    }

    @Override
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
                .filter(playerIsNotExpanded)
                .subscribe(new StartRefreshSubscriber());
    }

    public void stopRefreshing() {
        if (isAttached()) {
            wrapper.setRefreshing(false);
        }
    }

    private final class StartRefreshSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (isAttached()) {
                wrapper.setRefreshing(true);
            }
        }
    }

    private final class PlayerExpandedSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (event.getKind() == PlayerUIEvent.PLAYER_EXPANDED) {
                stopRefreshing();
            }
        }
    }

    private final class PageSubscriber extends DefaultSubscriber<Page> {

        private final PagingItemAdapter adapter;

        public PageSubscriber(PagingItemAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onNext(Page page) {
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
