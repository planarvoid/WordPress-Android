package com.soundcloud.android.search;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.main.DefaultFragment;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.ReactiveListComponent;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

@SuppressLint("ValidFragment")
public class SearchResultsFragment extends DefaultFragment
        implements ReactiveListComponent<ConnectableObservable<List<PropertySet>>> {

    static final String EXTRA_QUERY = "query";
    static final String EXTRA_TYPE = "type";

    @Inject SearchOperations searchOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject ListViewController listViewController;
    @Inject EventBus eventBus;
    @Inject SearchResultsAdapter adapter;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;

    private int searchType;
    private ConnectableObservable<List<PropertySet>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();
    private SearchOperations.SearchResultPager pager;


    public static SearchResultsFragment newInstance(int type, String query) {
        SearchResultsFragment fragment = new SearchResultsFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_TYPE, type);
        bundle.putString(EXTRA_QUERY, query);
        fragment.setArguments(bundle);
        return fragment;
    }

    public SearchResultsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponents();
    }

    @VisibleForTesting
    SearchResultsFragment(SearchOperations operations, ListViewController listViewController) {
        this.searchOperations = operations;
        this.listViewController = listViewController;
    }

    private void addLifeCycleComponents() {
        addLifeCycleComponent(listViewController);
        addLifeCycleComponent(adapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchType = getArguments().getInt(EXTRA_TYPE);
        pager = searchOperations.pager(searchType);
        listViewController.setAdapter(adapter, pager, SearchOperations.TO_PROPERTY_SET);

        connectObservable(buildObservable());
    }

    @Override
    public ConnectableObservable<List<PropertySet>> buildObservable() {
        final String query = getArguments().getString(EXTRA_QUERY);
        final Observable<ModelCollection<PropertySetSource>> observable = searchOperations.getSearchResult(query, searchType);
        return pager
                .page(observable).map(SearchOperations.TO_PROPERTY_SET)
                .observeOn(mainThread()).replay();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<PropertySet>> observable) {
        this.observable = observable;
        observable.subscribe(adapter);
        connectionSubscription = observable.connect();
        return connectionSubscription;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewController.connect(this, observable);
        new EmptyViewBuilder().configureForSearch(listViewController.getEmptyView());
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO: bring it back!
    }
}
