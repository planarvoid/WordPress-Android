package com.soundcloud.android.playlists;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.lightcycle.LightCycleSupportFragment;
import com.soundcloud.android.likes.PlaylistLikeOperations;
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
public class PlaylistLikesFragment extends LightCycleSupportFragment
        implements RefreshableListComponent<ConnectableObservable<List<PropertySet>>> {

    @Inject PlaylistLikesAdapter adapter;
    @Inject PlaylistLikeOperations likeOperations;
    @Inject ListViewController listViewController;
    @Inject PullToRefreshController pullToRefreshController;

    private ConnectableObservable<List<PropertySet>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();

    public PlaylistLikesFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);

        listViewController.setAdapter(adapter, likeOperations.likedPlaylistsPager());
        pullToRefreshController.setRefreshListener(this, adapter);

        addLifeCycleComponent(listViewController);
        addLifeCycleComponent(pullToRefreshController);
        addLifeCycleComponent(adapter.getLifeCycleHandler());
    }

    @VisibleForTesting
    PlaylistLikesFragment(PlaylistLikesAdapter adapter,
                          PlaylistLikeOperations likeOperations,
                          ListViewController listViewController,
                          PullToRefreshController pullToRefreshController) {
        this.adapter = adapter;
        this.likeOperations = likeOperations;
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

        listViewController.getEmptyView().setImage(R.drawable.empty_like);
        listViewController.getEmptyView().setMessageText(R.string.list_empty_liked_playlists_message);

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
        return pagedObservable(likeOperations.likedPlaylists());
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<PropertySet>> observable) {
        this.observable = observable;
        return observable.connect();
    }

    @Override
    public ConnectableObservable<List<PropertySet>> refreshObservable() {
        return pagedObservable(likeOperations.updatedLikedPlaylists());
    }

    private ConnectableObservable<List<PropertySet>> pagedObservable(Observable<List<PropertySet>> source) {
        final ConnectableObservable<List<PropertySet>> observable =
                likeOperations.likedPlaylistsPager().page(source).observeOn(mainThread()).replay();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Urn playlistUrn = adapter.getItem(position).get(PlaylistProperty.URN);
        PlaylistDetailActivity.start(getActivity(), playlistUrn, Screen.SIDE_MENU_PLAYLISTS);
    }
}
