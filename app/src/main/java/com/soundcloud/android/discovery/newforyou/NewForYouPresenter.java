package com.soundcloud.android.discovery.newforyou;

import static com.soundcloud.android.discovery.newforyou.NewForYouItem.Kind.TRACK;
import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.R;
import com.soundcloud.android.discovery.newforyou.NewForYouItem.NewForYouHeaderItem;
import com.soundcloud.android.discovery.newforyou.NewForYouItem.NewForYouTrackItem;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class NewForYouPresenter extends RecyclerViewPresenter<NewForYou, NewForYouItem> implements TrackItemRenderer.Listener, NewForYouHeaderRenderer.Listener {
    private static final int NUM_EXTRA_ITEMS = 1;

    private final NewForYouOperations operations;
    private final NewForYouAdapter adapter;
    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final Resources resources;
    private final EventBus eventBus;

    private final CompositeSubscription subscription = new CompositeSubscription();

    @Inject
    NewForYouPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                       NewForYouOperations operations,
                       NewForYouAdapterFactory adapterFactory,
                       PlaybackInitiator playbackInitiator,
                       Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                       Resources resources,
                       EventBus eventBus) {
        super(swipeRefreshAttacher, Options.list().build());

        this.operations = operations;
        this.adapter = adapterFactory.create(this, this);
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.resources = resources;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onDestroy(Fragment fragment) {
        subscription.unsubscribe();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        subscription.add(eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter)));
    }

    @Override
    protected CollectionBinding<NewForYou, NewForYouItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(operations.newForYou().observeOn(AndroidSchedulers.mainThread()), toNewForYouItems())
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    protected CollectionBinding<NewForYou, NewForYouItem> onRefreshBinding() {
        return CollectionBinding.from(operations.refreshNewForYou().observeOn(AndroidSchedulers.mainThread()), toNewForYouItems())
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private Func1<NewForYou, ? extends Iterable<NewForYouItem>> toNewForYouItems() {
        return newForYou -> {
            final List<NewForYouItem> items = new ArrayList<>(newForYou.tracks().size() + NUM_EXTRA_ITEMS);

            items.add(NewForYouHeaderItem.create(newForYou,
                                                 formatDuration(newForYou.tracks()),
                                                 ScTextUtils.formatTimeElapsedSince(resources, newForYou.lastUpdate().getTime(), true),
                                                 transform(newForYou.tracks(), TrackItem::from)));

            for (Track track : newForYou.tracks()) {
                items.add(NewForYouTrackItem.create(newForYou, TrackItem.from(track)));
            }

            return items;
        };
    }

    private String formatDuration(List<Track> tracks) {
        long duration = 0;

        for (Track track : tracks) {
            duration += track.fullDuration();
        }

        return resources.getString(R.string.new_for_you_duration, tracks.size(), ScTextUtils.formatTimestamp(duration, TimeUnit.MILLISECONDS));
    }

    @Override
    public void trackItemClicked(Urn urn, int adapterPosition) {
        startPlayback(adapterPosition, adapterPosition - NUM_EXTRA_ITEMS);
    }

    @Override
    public void playClicked() {
        startPlayback(0, 0);
    }

    private void startPlayback(int adapterPosition, int finalPosition) {
        subscription.add(playbackInitiator.playTracks(getTrackUrns(),
                                                    finalPosition,
                                                    getPlaySessionSource(adapterPosition, finalPosition))
                                        .subscribe(expandPlayerSubscriberProvider.get()));
    }

    private List<Urn> getTrackUrns() {
        final List<Urn> urns = new ArrayList<>(adapter.getItemCount() - NUM_EXTRA_ITEMS);

        for (NewForYouItem newForYouItem : adapter.getItems()) {
            if (newForYouItem.kind() == TRACK) {
                urns.add(((NewForYouTrackItem) newForYouItem).track().getUrn());
            }
        }

        return urns;
    }

    private PlaySessionSource getPlaySessionSource(int adapterPosition, int playbackPosition) {
        return PlaySessionSource.forNewForYou(Screen.NEW_FOR_YOU.get(),
                                              playbackPosition,
                                              adapter.getItem(adapterPosition).newForYou().queryUrn());
    }
}
