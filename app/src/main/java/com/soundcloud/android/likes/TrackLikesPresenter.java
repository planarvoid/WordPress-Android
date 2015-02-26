package com.soundcloud.android.likes;

import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT;
import static com.soundcloud.android.events.EventQueue.PLAY_QUEUE_TRACK;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.ListBinding;
import com.soundcloud.android.presentation.ListPresenter;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.view.adapters.PrependItemToListSubscriber;
import com.soundcloud.android.view.adapters.RemoveEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

class TrackLikesPresenter extends ListPresenter<PropertySet, PropertySet>
        implements AdapterView.OnItemClickListener {

    private final TrackLikeOperations likeOperations;
    private final TrackOperations trackOperations;
    private final OfflinePlaybackOperations playbackOperations;
    private final PagedTracksAdapter adapter;
    private final TrackLikesActionMenuController actionMenuController;
    private final TrackLikesHeaderPresenter headerPresenter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;

    private Subscription creationLifeCycle = Subscriptions.empty();
    private CompositeSubscription viewLifeCycle;

    private Func1<EntityStateChangedEvent, Observable<PropertySet>> loadTrack = new Func1<EntityStateChangedEvent, Observable<PropertySet>>() {
        @Override
        public Observable<PropertySet> call(EntityStateChangedEvent entityStateChangedEvent) {
            // This could actually just load the liked track cell representation, instead of the full representation
            return trackOperations.track(entityStateChangedEvent.getNextUrn());
        }
    };

    @Inject
    TrackLikesPresenter(TrackLikeOperations likeOperations,
                        TrackOperations trackOperations,
                        OfflinePlaybackOperations playbackOperations,
                        PagedTracksAdapter adapter,
                        TrackLikesActionMenuController actionMenuController,
                        TrackLikesHeaderPresenter headerPresenter,
                        Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider, EventBus eventBus,
                        ImageOperations imageOperations, PullToRefreshWrapper pullToRefreshWrapper) {
        super(imageOperations, pullToRefreshWrapper);
        this.likeOperations = likeOperations;
        this.trackOperations = trackOperations;
        this.playbackOperations = playbackOperations;
        this.adapter = adapter;
        this.actionMenuController = actionMenuController;
        this.headerPresenter = headerPresenter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;

        attach(actionMenuController);
        attach(headerPresenter);
        setHeaderPresenter(headerPresenter);
    }


    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getListBinding().connect();
        creationLifeCycle = eventBus.queue(OFFLINE_CONTENT).subscribe(new OfflineSyncQueueUpdated());
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

        getEmptyView().setImage(R.drawable.empty_like);
        getEmptyView().setMessageText(R.string.list_empty_user_likes_message);

        listView.setOnItemClickListener(this);

        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(PLAY_QUEUE_TRACK, new UpdatePlayingTrackSubscriber(adapter, adapter.getTrackPresenter())),
                eventBus.subscribe(OFFLINE_CONTENT, new OfflineSyncStopped()),
                eventBus.subscribe(ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)),
                // TODO: Extract how the prependTrackOnLike and removeTrackOnUnlike sequence is created into an operations class for this screen
                prependTrackOnLike(),
                removeTrackOnUnlike()
        );
    }

    private Subscription prependTrackOnLike() {
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_TRACK_LIKED_FILTER)
                .flatMap(loadTrack)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PrependItemToListSubscriber(adapter));
    }

    private Subscription removeTrackOnUnlike() {
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_TRACK_UNLIKED_FILTER)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RemoveEntityListSubscriber(adapter));
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        actionMenuController.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        // TODO create subscription light cycle
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onDestroy(Fragment fragment) {
        creationLifeCycle.unsubscribe();
        super.onDestroy(fragment);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        // here we assume that the list you are looking at is up to date with the database, which is not necessarily the case
        // a sync may have happened in the background. This is def. an edge case, but worth handling maybe??
        Urn initialTrack = ((PropertySet) adapterView.getItemAtPosition(position)).get(TrackProperty.URN);
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SIDE_MENU_LIKES);
        playbackOperations
                .playLikes(initialTrack, position, playSessionSource)
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

    private class OfflineSyncStopped extends DefaultSubscriber<OfflineContentEvent> {
        @Override
        public void onNext(OfflineContentEvent event) {
            if (event.getKind() == OfflineContentEvent.STOP) {
                adapter.notifyDataSetChanged();
            }
        }
    }

}
