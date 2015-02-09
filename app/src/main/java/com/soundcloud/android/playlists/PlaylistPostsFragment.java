package com.soundcloud.android.playlists;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.lightcycle.LightCycleFragment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
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
import java.util.List;

@SuppressLint("ValidFragment")
public class PlaylistPostsFragment extends LightCycleFragment
        implements RefreshableListComponent<ConnectableObservable<List<PropertySet>>> {

    @Inject PlaylistPostsAdapter adapter;
    @Inject PlaylistOperations playlistOperations;
    @Inject ListViewController listViewController;
    @Inject PullToRefreshController pullToRefreshController;

    private ConnectableObservable<List<PropertySet>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();

    public PlaylistPostsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);

        listViewController.setAdapter(adapter, playlistOperations.postedPlaylistsPager());
        pullToRefreshController.setRefreshListener(this, adapter);

        addLifeCycleComponent(listViewController);
        addLifeCycleComponent(pullToRefreshController);
        addLifeCycleComponent(adapter.getLifeCycleHandler());
    }

    @VisibleForTesting
    PlaylistPostsFragment(PlaylistPostsAdapter adapter,
                          PlaylistOperations playlistOperations,
                          ListViewController listViewController,
                          PullToRefreshController pullToRefreshController) {
        this.adapter = adapter;
        this.playlistOperations = playlistOperations;
        this.listViewController = listViewController;
        this.pullToRefreshController = pullToRefreshController;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectionSubscription = connectObservable(buildObservable());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_list_with_refresh, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewController.getEmptyView().setImage(R.drawable.empty_playlists);
        listViewController.getEmptyView().setMessageText(R.string.list_empty_you_playlists_message);

        listViewController.connect(this, observable);
        pullToRefreshController.connect(observable, adapter);
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public ConnectableObservable<List<PropertySet>> buildObservable() {
        return pagedObservable(playlistOperations.postedPlaylists());
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<PropertySet>> observable) {
        this.observable = observable;
        return observable.connect();
    }

    @Override
    public ConnectableObservable<List<PropertySet>> refreshObservable() {
        return pagedObservable(playlistOperations.updatedPostedPlaylists());
    }

    private ConnectableObservable<List<PropertySet>> pagedObservable(Observable<List<PropertySet>> source) {
        final ConnectableObservable<List<PropertySet>> observable =
                playlistOperations.postedPlaylistsPager().page(source).observeOn(mainThread()).replay();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Urn playlistUrn = adapter.getItem(position).get(PlaylistProperty.URN);
        PlaylistDetailActivity.start(getActivity(), playlistUrn, Screen.SIDE_MENU_PLAYLISTS);
    }
}
