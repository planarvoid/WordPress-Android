package com.soundcloud.android.likes;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.RefreshRecyclerViewAdapterSubscriber;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriber;
import com.soundcloud.android.utils.CollapsingScrollHelper;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.PrependItemToListSubscriber;
import com.soundcloud.android.view.adapters.RemoveEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

class TrackLikesPresenter extends RecyclerViewPresenter<List<PropertySet>, TrackLikesItem> {

    private static final int EXTRA_LIST_ITEMS = 1;
    @LightCycle final CollapsingScrollHelper scrollHelper;
    @LightCycle final TrackLikesHeaderPresenter headerPresenter;

    private final TrackLikeOperations likeOperations;
    private final PlaybackInitiator playbackOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final TrackLikesAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;

    private Subscription collectionSubscription = RxUtils.invalidSubscription();
    private Subscription entityStateChangedSubscription = RxUtils.invalidSubscription();

    private static final Func1<List<PropertySet>, ? extends Iterable<TrackLikesItem>> TO_TRACK_LIKES_ITEM = new Func1<List<PropertySet>, Iterable<TrackLikesItem>>() {
        @Override
        public Iterable<TrackLikesItem> call(List<PropertySet> propertySets) {
            List<TrackLikesItem> trackLikesItems = new ArrayList<>(propertySets.size() + EXTRA_LIST_ITEMS);
            for (PropertySet propertySet : propertySets) {
                trackLikesItems.add(new TrackLikesTrackItem(TrackItem.from(propertySet)));
            }
            return trackLikesItems;
        }
    };
    private static final Func1<? super PropertySet, TrackLikesTrackItem> TO_TRACK_LIKES_TRACK_ITEM = new Func1<PropertySet, TrackLikesTrackItem>() {
        @Override
        public TrackLikesTrackItem call(PropertySet track) {
            return new TrackLikesTrackItem(TrackItem.from(track));
        }
    };

    @Inject
    TrackLikesPresenter(TrackLikeOperations likeOperations,
                        PlaybackInitiator playbackInitiator,
                        OfflineContentOperations offlineContentOperations,
                        TrackLikesAdapter adapter,
                        TrackLikesHeaderPresenter headerPresenter,
                        Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider, EventBus eventBus,
                        SwipeRefreshAttacher swipeRefreshAttacher,
                        CollapsingScrollHelper scrollHelper) {
        super(swipeRefreshAttacher);
        this.likeOperations = likeOperations;
        this.playbackOperations = playbackInitiator;
        this.offlineContentOperations = offlineContentOperations;
        this.adapter = adapter;
        this.headerPresenter = headerPresenter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
        this.scrollHelper = scrollHelper;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<List<PropertySet>, TrackLikesItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(likeOperations.likedTracks(), TO_TRACK_LIKES_ITEM)
                                .withAdapter(adapter)
                                .withPager(likeOperations.pagingFunction())
                                .build();
    }

    @Override
    protected CollectionBinding<List<PropertySet>, TrackLikesItem> onRefreshBinding() {
        return CollectionBinding.from(
                likeOperations.updatedLikedTracks(), TO_TRACK_LIKES_ITEM)
                                .withAdapter(adapter)
                                .withPager(likeOperations.pagingFunction())
                                .build();
    }

    @Override
    protected void onSubscribeBinding(CollectionBinding<List<PropertySet>, TrackLikesItem> collectionBinding,
                                      CompositeSubscription viewLifeCycle) {
        Observable<List<Urn>> allLikedTrackUrns = collectionBinding.items()
                                                                   .first()
                                                                   .flatMap(RxUtils.continueWith(likeOperations.likedTrackUrns()))
                                                                   .observeOn(AndroidSchedulers.mainThread())
                                                                   .cache();
        collectionSubscription = allLikedTrackUrns.subscribe(new AllLikedTracksSubscriber());
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        // remove the blinking whenever we notifyItemChanged
        ((DefaultItemAnimator) getRecyclerView().getItemAnimator()).setSupportsChangeAnimations(false);

        getEmptyView().setImage(R.drawable.empty_like);
        getEmptyView().setMessageText(R.string.list_empty_you_likes_message);
        getEmptyView().setBackgroundResource(R.color.page_background);

        viewLifeCycle = new CompositeSubscription(

                eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM,
                                   new UpdatePlayableAdapterSubscriber(adapter)),

                eventBus.queue(OFFLINE_CONTENT_CHANGED)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new UpdateCurrentDownloadSubscriber(adapter)),

                eventBus.subscribe(ENTITY_STATE_CHANGED,
                                   new UpdateEntityListSubscriber(adapter)),

                likeOperations.onTrackLiked()
                              .map(TO_TRACK_LIKES_TRACK_ITEM)
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new PrependItemToListSubscriber<>(adapter)),

                likeOperations.onTrackUnliked()
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new RemoveEntityListSubscriber(adapter)),

                offlineContentOperations.getOfflineContentOrOfflineLikesStatusChanges()
                                        .subscribe(new RefreshRecyclerViewAdapterSubscriber(adapter))
        );

        entityStateChangedSubscription = eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                                                 .filter(EntityStateChangedEvent.IS_TRACK_LIKE_EVENT_FILTER)
                                                 .flatMap(RxUtils.continueWith(likeOperations.likedTrackUrns()))
                                                 .observeOn(AndroidSchedulers.mainThread())
                                                 .subscribe(new AllLikedTracksSubscriber());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        // TODO create subscription light cycle
        entityStateChangedSubscription.unsubscribe();
        collectionSubscription.unsubscribe();
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onItemClicked(View view, int position) {
        // here we assume that the list you are looking at is up to date with the database, which is not necessarily the case
        // a sync may have happened in the background. This is def. an edge case, but worth handling maybe??
        final TrackLikesItem item = adapter.getItem(position);

        if (item == null) {
            String exceptionMessage = "Adapter item is null on item click, with adapter: " + adapter + ", on position " + position;
            ErrorUtils.handleSilentException(new IllegalStateException(exceptionMessage));
        } else {
            TrackItem trackItem = ((TrackLikesTrackItem) item).getTrackItem();
            Urn initialTrack = trackItem.getUrn();
            PlaySessionSource playSessionSource = new PlaySessionSource(Screen.LIKES);
            playbackOperations
                    .playTracks(likeOperations.likedTrackUrns(), initialTrack, position, playSessionSource)
                    .subscribe(expandPlayerSubscriberProvider.get());
        }
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private class AllLikedTracksSubscriber extends DefaultSubscriber<List<Urn>> {
        @Override
        public void onNext(List<Urn> allLikedTracks) {
            headerPresenter.updateTrackCount(allLikedTracks.size());
        }
    }

}
