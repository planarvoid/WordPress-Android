package com.soundcloud.android.likes;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.ListBinding;
import com.soundcloud.android.presentation.ListPresenter;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.internal.util.UtilityFunctions;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

class TrackLikesPresenter extends ListPresenter<PropertySet, PropertySet>
        implements AdapterView.OnItemClickListener {

    private final LikeOperations likeOperations;
    private final PlaybackOperations playbackOperations;
    private final TrackLikesAdapter adapter;
    private final TrackLikesActionMenuController actionMenuController;
    private final TrackLikesHeaderPresenter headerPresenter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;

    private Subscription syncQueueUpdatedSubscription = Subscriptions.empty();

    @Inject
    TrackLikesPresenter(LikeOperations likeOperations,
                        PlaybackOperations playbackOperations,
                        TrackLikesAdapter adapter,
                        TrackLikesActionMenuController actionMenuController,
                        TrackLikesHeaderPresenter headerPresenter,
                        Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider, EventBus eventBus,
                        ImageOperations imageOperations, PullToRefreshWrapper pullToRefreshWrapper) {
        super(imageOperations, pullToRefreshWrapper);
        this.likeOperations = likeOperations;
        this.playbackOperations = playbackOperations;
        this.adapter = adapter;
        this.actionMenuController = actionMenuController;
        this.headerPresenter = headerPresenter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
    }


    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        syncQueueUpdatedSubscription = eventBus.queue(EventQueue.OFFLINE_CONTENT).subscribe(new OfflineSyncQueueUpdated());

        getListBinding().connect();
    }

    @Override
    protected ListBinding<PropertySet, PropertySet> onBuildListBinding() {
        return ListBinding.pagedList(
                likeOperations.likedTracks(),
                adapter,
                likeOperations.likedTracksPager(),
                UtilityFunctions.<List<PropertySet>>identity());
    }

    @Override
    protected ListBinding<PropertySet, PropertySet> onBuildRefreshBinding() {
        return ListBinding.pagedList(
                likeOperations.updatedLikedTracks(),
                adapter,
                likeOperations.likedTracksPager(),
                UtilityFunctions.<List<PropertySet>>identity());
    }

    @Override
    protected void onSubscribeListBinding(ListBinding<PropertySet, PropertySet> listBinding) {
        headerPresenter.onSubscribeListObservers(listBinding);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        final ListView listView = (ListView) getListView();
        headerPresenter.onViewCreated(view, listView);

        getEmptyView().setImage(R.drawable.empty_like);
        getEmptyView().setMessageText(R.string.list_empty_user_likes_message);

        listView.setOnItemClickListener(this);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        actionMenuController.onCreateOptionsMenu(menu, inflater);
    }

    public boolean onOptionsItemSelected(Fragment fragment, MenuItem item) {
        return actionMenuController.onOptionsItemSelected(fragment, item);
    }

    @Override
    public void onResume(Fragment fragment) {
        super.onResume(fragment);
        actionMenuController.onResume(fragment);
        headerPresenter.onResume();
    }

    @Override
    public void onPause(Fragment fragment) {
        actionMenuController.onPause();
        headerPresenter.onPause();
        super.onPause(fragment);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        headerPresenter.onDestroyView();
        super.onDestroyView(fragment);
    }

    @Override
    public void onDestroy(Fragment fragment) {
        syncQueueUpdatedSubscription.unsubscribe();
        super.onDestroy(fragment);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        // here we assume that the list you are looking at is up to date with the database, which is not necessarily the case
        // a sync may have happened in the background. This is def. an edge case, but worth handling maybe??
        Urn initialTrack = ((PropertySet) adapterView.getItemAtPosition(position)).get(TrackProperty.URN);
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SIDE_MENU_LIKES);
        playbackOperations
                .playTracks(likeOperations.likedTrackUrns(), initialTrack, position, playSessionSource)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    private class OfflineSyncQueueUpdated extends DefaultSubscriber<OfflineContentEvent> {
        @Override
        public void onNext(OfflineContentEvent event) {
            if (event.getKind() == OfflineContentEvent.QUEUE_UPDATED) {
                adapter.clear();
                rebuildListBinding().connect();
            }
        }
    }
}
