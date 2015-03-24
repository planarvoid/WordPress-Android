package com.soundcloud.android.stream;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.lightcycle.LightCycleSupportFragment;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("ValidFragment")
public class SoundStreamFragment extends LightCycleSupportFragment
        implements RefreshableListComponent<ConnectableObservable<List<PlayableItem>>> {

    @VisibleForTesting
    static final String ONBOARDING_RESULT_EXTRA = "onboarding.result";

    private static final Func1<List<PropertySet>, List<PlayableItem>> PAGE_TRANSFORMER =
            new Func1<List<PropertySet>, List<PlayableItem>>() {
                @Override
                public List<PlayableItem> call(List<PropertySet> bindings) {
                    final List<PlayableItem> items = new ArrayList<>(bindings.size());
                    for (PropertySet source : bindings) {
                        final Urn urn = source.get(EntityProperty.URN);
                        if (urn.isTrack()) {
                            items.add(TrackItem.from(source));
                        } else if (urn.isPlaylist()) {
                            items.add(PlaylistItem.from(source));
                        }
                    }
                    return items;
                }
            };


    @Inject SoundStreamOperations operations;
    @Inject SoundStreamAdapter adapter;
    @Inject ListViewController listViewController;
    @Inject PullToRefreshController pullToRefreshController;
    @Inject PlaybackOperations playbackOperations;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;

    private ConnectableObservable<List<PlayableItem>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();

    public static SoundStreamFragment create(boolean onboardingSucceeded) {
        final Bundle args = new Bundle();
        args.putBoolean(ONBOARDING_RESULT_EXTRA, onboardingSucceeded);
        SoundStreamFragment fragment = new SoundStreamFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public SoundStreamFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponents();
    }

    @VisibleForTesting
    SoundStreamFragment(SoundStreamOperations operations,
                        SoundStreamAdapter adapter,
                        ListViewController listViewController,
                        PullToRefreshController pullToRefreshController,
                        PlaybackOperations playbackOperations,
                        Provider<ExpandPlayerSubscriber> subscriberProvider) {
        this.operations = operations;
        this.adapter = adapter;
        this.listViewController = listViewController;
        this.pullToRefreshController = pullToRefreshController;
        this.playbackOperations = playbackOperations;
        this.subscriberProvider = subscriberProvider;
        addLifeCycleComponents();
    }

    private void addLifeCycleComponents() {
        listViewController.setAdapter(adapter, operations.pager(), PAGE_TRANSFORMER);
        pullToRefreshController.setRefreshListener(this, adapter);

        attachLightCycle(listViewController);
        attachLightCycle(pullToRefreshController);
        attachLightCycle(adapter);
    }

    @Override
    public ConnectableObservable<List<PlayableItem>> buildObservable() {
        return buildPagedObservable(operations.existingStreamItems());
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<PlayableItem>> observable) {
        this.observable = observable;
        this.connectionSubscription = observable.connect();
        return connectionSubscription;
    }

    @Override
    public ConnectableObservable<List<PlayableItem>> refreshObservable() {
        return buildPagedObservable(operations.updatedStreamItems());
    }

    private ConnectableObservable<List<PlayableItem>> buildPagedObservable(Observable<List<PropertySet>> source) {
        final ConnectableObservable<List<PlayableItem>> observable =
                operations.pager().page(source)
                        .map(PAGE_TRANSFORMER)
                        .observeOn(mainThread())
                        .replay();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectObservable(buildObservable());
    }

    @Override
    public void onResume() {
        super.onResume();
        operations.updateLastSeen();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_list_with_refresh, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final EmptyView emptyView = listViewController.getEmptyView();
        emptyView.setImage(R.drawable.empty_stream);
        if (getArguments().getBoolean(ONBOARDING_RESULT_EXTRA)) {
            emptyView.setMessageText(R.string.list_empty_stream_message);
            emptyView.setActionText(R.string.list_empty_stream_action);
            emptyView.setButtonActions(new Intent(Actions.WHO_TO_FOLLOW));
        } else {
            emptyView.setMessageText(R.string.error_onboarding_fail);
        }

        listViewController.connect(this, observable);
        pullToRefreshController.connect(observable, adapter);
    }


    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ListItem item = adapter.getItem(position);
        final Urn playableUrn = item.getEntityUrn();
        if (playableUrn.isTrack()) {
            playbackOperations
                    .playTracks(operations.trackUrnsForPlayback(),
                            playableUrn,
                            position,
                            new PlaySessionSource(Screen.SIDE_MENU_STREAM))
                    .subscribe(subscriberProvider.get());
        } else if (playableUrn.isPlaylist()) {
            PlaylistDetailActivity.start(getActivity(), playableUrn, Screen.SIDE_MENU_STREAM);
        }
    }
}
