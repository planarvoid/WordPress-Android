package com.soundcloud.android.likes;

import static com.soundcloud.android.events.EventQueue.CURRENT_DOWNLOAD;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.PLAY_QUEUE_TRACK;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListPresenter;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.RefreshAdapterSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.adapters.PrependItemToListSubscriber;
import com.soundcloud.android.view.adapters.RemoveEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.lightcycle.LightCycle;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;
import javax.inject.Provider;

class TrackLikesPresenter extends ListPresenter<TrackItem> {

    final @LightCycle TrackLikesActionMenuController actionMenuController;
    final @LightCycle TrackLikesHeaderPresenter headerPresenter;

    private final TrackLikeOperations likeOperations;
    private final OfflinePlaybackOperations playbackOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final PagedTracksAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;

    @Inject
    TrackLikesPresenter(TrackLikeOperations likeOperations,
                        OfflinePlaybackOperations playbackOperations,
                        OfflineContentOperations offlineContentOperations, PagedTracksAdapter adapter,
                        TrackLikesActionMenuController actionMenuController,
                        TrackLikesHeaderPresenter headerPresenter,
                        Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider, EventBus eventBus,
                        ImageOperations imageOperations, PullToRefreshWrapper pullToRefreshWrapper) {
        super(imageOperations, pullToRefreshWrapper);
        this.likeOperations = likeOperations;
        this.playbackOperations = playbackOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.adapter = adapter;
        this.actionMenuController = actionMenuController;
        this.headerPresenter = headerPresenter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;

        setHeaderPresenter(headerPresenter);
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<TrackItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(likeOperations.likedTracks(), TrackItem.fromPropertySets())
                .withAdapter(adapter)
                .withPager(likeOperations.pagingFunction())
                .build();
    }

    @Override
    protected CollectionBinding<TrackItem> onRefreshBinding() {
        return CollectionBinding.from(likeOperations.updatedLikedTracks(), TrackItem.fromPropertySets())
                .withAdapter(adapter)
                .withPager(likeOperations.pagingFunction())
                .build();
    }

    @Override
    protected void onSubscribeBinding(CollectionBinding<TrackItem> collectionBinding, CompositeSubscription viewLifeCycle) {
        headerPresenter.onSubscribeListObservers(collectionBinding);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        getEmptyView().setImage(R.drawable.empty_like);
        getEmptyView().setMessageText(R.string.list_empty_user_likes_message);

        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(PLAY_QUEUE_TRACK,
                        new UpdatePlayingTrackSubscriber(adapter, adapter.getTrackRenderer())),
                eventBus.queue(CURRENT_DOWNLOAD)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new UpdateCurrentDownloadSubscriber(adapter)),
                eventBus.subscribe(ENTITY_STATE_CHANGED,
                        new UpdateEntityListSubscriber(adapter)),

                likeOperations.onTrackLiked()
                        .map(TrackItem.fromPropertySet())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new PrependItemToListSubscriber<>(adapter)),
                likeOperations.onTrackUnliked()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RemoveEntityListSubscriber(adapter)),

                offlineContentOperations.getOfflineContentOrLikesStatus()
                        .subscribe(new RefreshAdapterSubscriber(adapter))
        );
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
    public void onItemClicked(View view, int position) {
        // here we assume that the list you are looking at is up to date with the database, which is not necessarily the case
        // a sync may have happened in the background. This is def. an edge case, but worth handling maybe??
        final int realPosition = position - ((ListView) getListView()).getHeaderViewsCount();
        TrackItem item = adapter.getItem(realPosition);
        if (item == null) {
            String exceptionMessage = "Adapter item is null on item click, with adapter: " + adapter + ", on position " + realPosition;
            ErrorUtils.handleSilentException(new IllegalStateException(exceptionMessage));
        } else {
            Urn initialTrack = item.getEntityUrn();
            PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SIDE_MENU_LIKES);
            playbackOperations
                    .playLikes(initialTrack, realPosition, playSessionSource)
                    .subscribe(expandPlayerSubscriberProvider.get());
        }
    }

    @Override
    protected int handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

}
