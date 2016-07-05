package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.android.ApplicationModule.LIGHT_TRACK_ADAPTER;
import static com.soundcloud.android.playback.PlayQueueManager.RepeatMode.REPEAT_ONE;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlayQueueManager.RepeatMode;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.view.adapters.TracksRecyclerItemAdapter;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class PlayQueuePresenter extends SupportFragmentLightCycleDispatcher<Fragment>
        implements TrackItemRenderer.Listener {

    private final TracksRecyclerItemAdapter adapter;
    private final PlayQueueManager playQueueManager;
    private final PlayQueueOperations playQueueOperations;
    private final EventBus eventBus;
    private final CompositeSubscription subscriptions = new CompositeSubscription();
    private Subscription updateSubscription = RxUtils.invalidSubscription();

    @Bind(R.id.recycler_view) RecyclerView recyclerView;
    @Bind(R.id.play_queue_drawer) View playQueueDrawer;

    @Inject
    public PlayQueuePresenter(@Named(LIGHT_TRACK_ADAPTER) TracksRecyclerItemAdapter adapter,
                              PlayQueueManager playQueueManager,
                              PlayQueueOperations playQueueOperations,
                              EventBus eventBus) {
        this.adapter = adapter;
        this.playQueueManager = playQueueManager;
        this.playQueueOperations = playQueueOperations;
        this.eventBus = eventBus;
        adapter.setTrackItemListener(this);
    }

    @Override
    public void onViewCreated(final Fragment fragment, final View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.bind(this, view);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemAnimator(new PlayQueueItemAnimator());

        playQueueDrawer.setVisibility(View.VISIBLE);
        subscriptions.add(eventBus.subscribeImmediate(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                                                      new UpdatePlayingTrackSubscriber(adapter)));
        subscriptions.add(eventBus.subscribeImmediate(EventQueue.PLAY_QUEUE, new UpdateSubscriber()));
        refreshPlayQueue();
    }

    @Override
    public void onResume(Fragment fragment) {
        final View view = fragment.getView();
        setupRepeatButton(view);
        setupShuffleButton(view);

        super.onResume(fragment);
    }

    @OnClick(R.id.close_play_queue)
    public void closePlayQueue(View view) {
        eventBus.publish(EventQueue.PLAY_QUEUE_UI, PlayQueueUIEvent.createHideEvent());
    }

    private class UpdateSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent playQueueEvent) {
            updateSubscription.unsubscribe();
            refreshPlayQueue();
        }
    }

    private void refreshPlayQueue() {
        updateSubscription = playQueueOperations.getTrackItems()
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(new PlayQueueSubscriber());
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<List<TrackItem>> {
        @Override
        public void onNext(List<TrackItem> trackItems) {
            adapter.clear();
            for (TrackItem item : trackItems) {
                adapter.addItem(item);
            }
            adapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(getScrollPosition());
            adapter.updateNowPlaying(playQueueManager.getCurrentPlayQueueItem().getUrn());
        }
    }

    private int getScrollPosition() {
        int currentPlayQueuePosition = playQueueManager.getPositionOfCurrentPlayQueueItem();
        if (currentPlayQueuePosition < 1) {
            return 0;
        } else {
            return currentPlayQueuePosition - 1;
        }
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        subscriptions.unsubscribe();
        updateSubscription.unsubscribe();
        ButterKnife.unbind(this);
        super.onDestroyView(fragment);
    }

    @Override
    public void trackItemClicked(Urn urn) {
        playQueueManager.setCurrentPlayQueueItem(urn);
        adapter.updateNowPlaying(urn);
    }

    private void setupRepeatButton(View view) {
        ImageView button = ButterKnife.findById(view, R.id.repeat_button);
        setupRepeatButtonIcon(button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playQueueManager.setRepeatMode(getNextRepeatMode());
                updateRepeatAdapter();
                setupRepeatButtonIcon((ImageView) v);
            }
        });
    }

    private void setupShuffleButton(View view) {
        ToggleButton button = ButterKnife.findById(view, R.id.shuffle_button);
        button.setChecked(playQueueManager.isShuffled());
    }

    @OnClick(R.id.shuffle_button)
    void shuffleClicked(ToggleButton toggle) {
        if (toggle.isChecked()) {
            playQueueManager.shuffle();
        } else {
            playQueueManager.unshuffle();
        }
    }

    private RepeatMode getNextRepeatMode() {
        final RepeatMode[] repeatModes = RepeatMode.values();
        final int currentOrdinal = playQueueManager.getRepeatMode().ordinal();
        final int nextOrdinal = (currentOrdinal + 1) % repeatModes.length;
        return repeatModes[nextOrdinal];
    }

    private void setupRepeatButtonIcon(ImageView button) {
        switch (playQueueManager.getRepeatMode()) {
            case REPEAT_ONE:
                button.setImageResource(R.drawable.ic_repeat_one);
                break;
            case REPEAT_ALL:
                button.setImageResource(R.drawable.ic_repeat_all);
                break;
            case REPEAT_NONE:
            default:
                button.setImageResource(R.drawable.ic_repeat);
        }
    }

    private void updateRepeatAdapter() {
        adapter.updateInRepeatMode(playQueueManager.getRepeatMode() == REPEAT_ONE);
    }
}
