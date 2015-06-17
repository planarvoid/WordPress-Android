package com.soundcloud.android.playlists;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import java.util.List;

@SuppressLint("ValidFragment")
public class PlaylistPostsFragment extends LightCycleSupportFragment
        implements RefreshableListComponent<ConnectableObservable<List<PlaylistItem>>> {

    @Inject PlaylistPostsAdapter adapter;
    @Inject PlaylistPostOperations playlistPostOperations;
    @Inject @LightCycle ListViewController listViewController;
    @Inject @LightCycle PullToRefreshController pullToRefreshController;

    private ConnectableObservable<List<PlaylistItem>> observable;
    private Subscription connectionSubscription = RxUtils.invalidSubscription();

    public PlaylistPostsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);

        listViewController.setAdapter(adapter, playlistPostOperations.postedPlaylistsPager(), PlaylistItem.fromPropertySets());
        pullToRefreshController.setRefreshListener(this, adapter);

        bind(adapter.getLifeCycleHandler());
    }

    @VisibleForTesting
    PlaylistPostsFragment(PlaylistPostsAdapter adapter,
                          PlaylistPostOperations playlistPostOperations,
                          ListViewController listViewController,
                          PullToRefreshController pullToRefreshController) {
        this.adapter = adapter;
        this.playlistPostOperations = playlistPostOperations;
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
    public ConnectableObservable<List<PlaylistItem>> buildObservable() {
        return pagedObservable(playlistPostOperations.postedPlaylists());
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<PlaylistItem>> observable) {
        this.observable = observable;
        return observable.connect();
    }

    @Override
    public ConnectableObservable<List<PlaylistItem>> refreshObservable() {
        return pagedObservable(playlistPostOperations.updatedPostedPlaylists());
    }

    private ConnectableObservable<List<PlaylistItem>> pagedObservable(Observable<List<PropertySet>> source) {
        final ConnectableObservable<List<PlaylistItem>> observable =
                playlistPostOperations.postedPlaylistsPager()
                        .page(source)
                        .map(PlaylistItem.fromPropertySets())
                        .observeOn(mainThread())
                        .replay();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Urn playlistUrn = adapter.getItem(position).getEntityUrn();
        PlaylistDetailActivity.start(getActivity(), playlistUrn, Screen.SIDE_MENU_PLAYLISTS);
    }
}
