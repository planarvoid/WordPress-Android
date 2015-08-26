package com.soundcloud.android.search;

import static com.soundcloud.java.checks.Preconditions.checkArgument;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.EmptyViewController;
import com.soundcloud.android.view.ListenableScrollView;
import com.soundcloud.android.view.ReactiveComponent;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class PlaylistTagsFragment extends LightCycleSupportFragment implements ListenableScrollView.OnScrollListener,
        ReactiveComponent<ConnectableObservable<List<String>>> {

    @Inject PlaylistDiscoveryOperations operations;
    @Inject PlaylistTagsPresenter presenter;
    @Inject EventBus eventBus;
    @Inject @LightCycle EmptyViewController emptyViewController;

    private Subscription connectionSubscription = RxUtils.invalidSubscription();
    private CompositeSubscription viewSubscriptions;
    private ConnectableObservable<List<String>> allTagsObservable;
    private Observable<List<String>> recentTagsObservable;

    public interface PlaylistTagsFragmentListener extends PlaylistTagsPresenter.Listener {
        void onTagsScrolled();
    }

    public PlaylistTagsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        checkArgument(activity instanceof PlaylistTagsFragmentListener, "Host activity must be a " + PlaylistTagsFragmentListener.class);
        presenter.setListener((PlaylistTagsPresenter.Listener) activity);
    }

    @Override
    public void onDetach() {
        presenter.setListener(null);
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recentTagsObservable = operations.recentPlaylistTags().observeOn(mainThread());
        connectObservable(buildAllTagsObservable());
    }

    @Override
    public ConnectableObservable<List<String>> buildObservable() {
        ConnectableObservable<List<String>> observable = buildAllTagsObservable();
        viewSubscriptions.add(observable.subscribe(new TagsSubscriber()));
        return observable;
    }

    private ConnectableObservable<List<String>> buildAllTagsObservable() {
        return operations.popularPlaylistTags().observeOn(mainThread()).replay();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<String>> observable) {
        allTagsObservable = observable;
        connectionSubscription = allTagsObservable.connect();
        return connectionSubscription;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_tags_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        emptyViewController.connect(allTagsObservable);

        ListenableScrollView scrollView = (ListenableScrollView) view.findViewById(R.id.playlist_tags_scroll_container);
        scrollView.setOnScrollListener(this);

        viewSubscriptions = new CompositeSubscription();
        viewSubscriptions.add(allTagsObservable.subscribe(new TagsSubscriber()));
        viewSubscriptions.add(recentTagsObservable.subscribe(new RecentsSubscriber()));
    }

    @Override
    public void onDestroyView() {
        viewSubscriptions.unsubscribe();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onScroll(int top, int oldTop) {
        ((PlaylistTagsFragmentListener) getActivity()).onTagsScrolled();
    }

    private final class TagsSubscriber extends DefaultSubscriber<List<String>> {

        @Override
        public void onNext(List<String> tags) {
            presenter.displayPopularTags(getView(), tags);
        }

    }

    private final class RecentsSubscriber extends DefaultSubscriber<List<String>> {

        @Override
        public void onNext(List<String> tags) {
            if (!tags.isEmpty()) {
                presenter.displayRecentTags(getView(), tags);
            }
        }
    }

}
