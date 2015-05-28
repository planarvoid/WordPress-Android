package com.soundcloud.android.stream;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackEvent;
import com.soundcloud.android.image.RecyclerViewPauseOnScrollListener;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.DividerItemDecoration;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.PromotedTrackProperty;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerViewAdapter;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class SoundStreamPresenter extends RecyclerViewPresenter<PlayableItem> {

    @VisibleForTesting
    static final Func1<List<PropertySet>, List<PlayableItem>> PAGE_TRANSFORMER =
            new Func1<List<PropertySet>, List<PlayableItem>>() {
                @Override
                public List<PlayableItem> call(List<PropertySet> bindings) {
                    final List<PlayableItem> items = new ArrayList<>(bindings.size());
                    for (PropertySet source : bindings) {
                        final Urn urn = source.get(EntityProperty.URN);
                        if (urn.isTrack()) {
                            if (source.contains(PromotedTrackProperty.AD_URN)) {
                                items.add(PromotedTrackItem.from(source));
                            } else {
                                items.add(TrackItem.from(source));
                            }
                        } else if (urn.isPlaylist()) {
                            items.add(PlaylistItem.from(source));
                        }
                    }
                    return items;
                }
            };

    @VisibleForTesting
    final Action1<List<PropertySet>> promotedImpression = new Action1<List<PropertySet>>() {
        @Override
        public void call(List<PropertySet> propertySets) {
            if (!propertySets.isEmpty()) {
                PropertySet first = propertySets.get(0);
                if (first.contains(PromotedTrackProperty.AD_URN)) {
                    eventBus.publish(EventQueue.TRACKING,
                            PromotedTrackEvent.forImpression(PromotedTrackItem.from(first), Screen.SIDE_MENU_STREAM.get()));
                }
            }
        }
    };

    private final SoundStreamOperations streamOperations;
    private final PlaybackOperations playbackOperations;
    private final MixedPlayableRecyclerViewAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> subscriberProvider;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;
    private boolean isOnboardingSuccess;


    @Inject
    SoundStreamPresenter(SoundStreamOperations streamOperations,
                         PlaybackOperations playbackOperations,
                         MixedPlayableRecyclerViewAdapter adapter,
                         RecyclerViewPauseOnScrollListener recyclerViewPauseOnScrollListener,
                         PullToRefreshWrapper pullToRefreshWrapper,
                         Provider<ExpandPlayerSubscriber> subscriberProvider,
                         EventBus eventBus,
                         DividerItemDecoration dividerItemDecoration) {
        super(pullToRefreshWrapper, recyclerViewPauseOnScrollListener, dividerItemDecoration);
        this.streamOperations = streamOperations;
        this.playbackOperations = playbackOperations;
        this.adapter = adapter;
        this.subscriberProvider = subscriberProvider;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    public void setOnboardingSuccess(boolean onboardingSuccess) {
        this.isOnboardingSuccess = onboardingSuccess;
    }

    @Override
    protected CollectionBinding<PlayableItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(streamOperations.initialStreamItems().doOnNext(promotedImpression), PAGE_TRANSFORMER)
                .withAdapter(adapter)
                .withPager(streamOperations.pagingFunction())
                .build();
    }

    @Override
    protected CollectionBinding<PlayableItem> onRefreshBinding() {
        return CollectionBinding.from(streamOperations.updatedStreamItems().doOnNext(promotedImpression), PAGE_TRANSFORMER)
                .withAdapter(adapter)
                .withPager(streamOperations.pagingFunction())
                .build();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        configureEmptyView();
        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new UpdatePlayingTrackSubscriber(adapter, adapter.getTrackRenderer())),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter))
        );
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    private void configureEmptyView() {
        final EmptyView emptyView = getEmptyView();
        emptyView.setImage(R.drawable.empty_stream);
        if (isOnboardingSuccess) {
            emptyView.setMessageText(R.string.list_empty_stream_message);
            emptyView.setActionText(R.string.list_empty_stream_action);
            emptyView.setButtonActions(new Intent(Actions.WHO_TO_FOLLOW));
        } else {
            emptyView.setMessageText(R.string.error_onboarding_fail);
        }
    }

    @Override
    protected void onItemClicked(View view, int position) {
        final ListItem item = adapter.getItem(position);
        final Urn playableUrn = item.getEntityUrn();
        if (item instanceof PromotedTrackItem) {
            playFromPromotedTrack(position, (PromotedTrackItem) item, playableUrn);
        } else if (playableUrn.isTrack()) {
            playTracks(position, playableUrn, new PlaySessionSource(Screen.SIDE_MENU_STREAM));
        } else if (playableUrn.isPlaylist()) {
            PlaylistDetailActivity.start(view.getContext(), playableUrn, Screen.SIDE_MENU_STREAM);
        }
    }

    private void playFromPromotedTrack(int position, PromotedTrackItem promotedTrack, Urn playableUrn) {
        eventBus.publish(EventQueue.TRACKING,
                PromotedTrackEvent.forTrackClick(promotedTrack, Screen.SIDE_MENU_STREAM.get()));
        PlaySessionSource source = new PlaySessionSource(Screen.SIDE_MENU_STREAM);
        source.setPromotedSourceInfo(PromotedSourceInfo.fromTrack(promotedTrack));
        playTracks(position, playableUrn, source);
    }

    private void playTracks(int position, Urn playableUrn, PlaySessionSource playSessionSource) {
        playbackOperations
                .playTracks(streamOperations.trackUrnsForPlayback(), playableUrn, position, playSessionSource)
                .subscribe(subscriberProvider.get());
    }

}
