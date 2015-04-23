package com.soundcloud.android.stream;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.ListPresenter;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AdapterView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class SoundStreamPresenter extends ListPresenter<PropertySet, PlayableItem>
        implements AdapterView.OnItemClickListener {

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

    private final SoundStreamOperations streamOperations;
    private final PlaybackOperations playbackOperations;
    private final SoundStreamAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> subscriberProvider;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;
    private boolean isOnboardingSuccess;

    @Inject
    SoundStreamPresenter(SoundStreamOperations streamOperations,
                         PlaybackOperations playbackOperations,
                         SoundStreamAdapter adapter,
                         ImageOperations imageOperations,
                         PullToRefreshWrapper pullToRefreshWrapper,
                         Provider<ExpandPlayerSubscriber> subscriberProvider,
                         EventBus eventBus) {
        super(imageOperations, pullToRefreshWrapper);
        this.streamOperations = streamOperations;
        this.playbackOperations = playbackOperations;
        this.adapter = adapter;
        this.subscriberProvider = subscriberProvider;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getListBinding().connect();
    }

    public void setOnboardingSuccess(boolean onboardingSuccess) {
        this.isOnboardingSuccess = onboardingSuccess;
    }

    @Override
    public void onResume(Fragment fragment) {
        super.onResume(fragment);
        streamOperations.updateLastSeen();
    }

    @Override
    protected ListBinding<PropertySet, PlayableItem> onBuildListBinding() {
        return ListBinding.pagedList(
                streamOperations.existingStreamItems(),
                adapter,
                streamOperations.pager(),
                PAGE_TRANSFORMER
        );
    }

    @Override
    protected ListBinding<PropertySet, PlayableItem> onBuildRefreshBinding() {
        return ListBinding.pagedList(
                streamOperations.updatedStreamItems(),
                adapter,
                streamOperations.pager(),
                PAGE_TRANSFORMER
        );
    }

    @Override
    protected void onSubscribeListBinding(ListBinding<PropertySet, PlayableItem> listBinding) {
        // Nothing
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        getListView().setOnItemClickListener(this);
        configureEmptyView();

        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new UpdatePlayingTrackSubscriber(adapter, adapter.getTrackPresenter())),
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ListItem item = adapter.getItem(position);
        final Urn playableUrn = item.getEntityUrn();
        if (playableUrn.isTrack()) {
            playbackOperations
                    .playTracks(streamOperations.trackUrnsForPlayback(),
                            playableUrn,
                            position,
                            new PlaySessionSource(Screen.SIDE_MENU_STREAM))
                    .subscribe(subscriberProvider.get());
        } else if (playableUrn.isPlaylist()) {
            PlaylistDetailActivity.start(view.getContext(), playableUrn, Screen.SIDE_MENU_STREAM);
        }
    }

}
