package com.soundcloud.android.playback.playqueue;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueView extends SupportFragmentLightCycleDispatcher<Fragment> {

    private final PlayQueuePresenter playQueuePresenter;
    private final PlayQueueAdapter playQueueAdapter;
    private final PlayQueueSwipeToRemoveCallback swipeToRemoveCallback;
    private final FeedbackController feedbackController;
    private final TopPaddingDecorator topPaddingDecorator;
    private final SmoothScrollLinearLayoutManager layoutManager;

    @BindView(R.id.repeat_button) ImageView repeatView;
    @BindView(R.id.shuffle_button) ToggleButton shuffleView;
    @BindView(R.id.loading_indicator) View loadingIndicator;
    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.player_strip) View playerStrip;
    private Unbinder unbinder;

    @Inject
    public PlayQueueView(PlayQueuePresenter playQueuePresenter,
                         PlayQueueAdapter playQueueAdapter,
                         PlayQueueSwipeToRemoveCallbackFactory swipeToRemoveCallbackFactory,
                         FeedbackController feedbackController,
                         TopPaddingDecorator topPaddingDecorator,
                         SmoothScrollLinearLayoutManager layoutManager) {
        this.playQueuePresenter = playQueuePresenter;
        this.playQueueAdapter = playQueueAdapter;
        this.feedbackController = feedbackController;
        this.topPaddingDecorator = topPaddingDecorator;
        this.layoutManager = layoutManager;
        swipeToRemoveCallback = swipeToRemoveCallbackFactory.create(playQueuePresenter);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        initRecyclerView();
        playQueuePresenter.attachView(this);
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(playQueueAdapter);
        recyclerView.setHasFixedSize(false);
        recyclerView.addItemDecoration(topPaddingDecorator, 0);
        recyclerView.setItemAnimator(buildItemAnimator());
        recyclerView.addOnScrollListener(new PositionScrollListener());

        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeToRemoveCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        playQueueAdapter.setDragListener(itemTouchHelper::startDrag);
        playQueueAdapter.setTrackClickListener(playQueuePresenter::trackClicked);
        playQueueAdapter.addNowPlayingChangedListener(playQueuePresenter::nowPlayingChanged);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        unbinder.unbind();
        playQueueAdapter.removeListeners();
        playQueuePresenter.detachContract();
    }

    public void setGoPlayerStrip() {
        playerStrip.setBackgroundResource(R.drawable.go_gradient);
    }

    public void setDefaultPlayerStrip() {
        playerStrip.setBackgroundResource(R.color.ak_sc_orange);
    }

    public void setShuffledState(boolean shuffled) {
        shuffleView.setChecked(shuffled);
    }

    public void setRepeatMode(PlayQueueManager.RepeatMode nextRepeatMode) {
        switch (nextRepeatMode) {
            case REPEAT_ONE:
                repeatView.setImageResource(R.drawable.ic_repeat_one);
                break;
            case REPEAT_ALL:
                repeatView.setImageResource(R.drawable.ic_repeat_all);
                break;
            case REPEAT_NONE:
            default:
                repeatView.setImageResource(R.drawable.ic_repeat_off);
        }
        playQueueAdapter.updateInRepeatMode(nextRepeatMode);
    }

    public void scrollTo(int position) {
        recyclerView.scrollToPosition(position);
    }

    public int getAdapterPosition(PlayQueueItem playQueueItem) {
        return playQueueAdapter.getAdapterPosition(playQueueItem);
    }

    public int getItemCount() {
        return playQueueAdapter.getItemCount();
    }

    public PlayQueueUIItem getItem(int position) {
        return playQueueAdapter.getItem(position);
    }

    public void removeItem(int position) {
        playQueueAdapter.removeItem(position);
    }

    public void switchItems(int fromPosition, int toPosition) {
        playQueueAdapter.switchItems(fromPosition, toPosition);
    }

    public int getQueuePosition(int position) {
        return playQueueAdapter.getQueuePosition(position);
    }

    public List<PlayQueueUIItem> getItems() {
        return playQueueAdapter.getItems();
    }

    public void clear() {
        playQueueAdapter.clear();
    }

    public void addItem(int position, PlayQueueUIItem item) {
        playQueueAdapter.addItem(position, item);
    }

    public void addItem(PlayQueueUIItem item) {
        playQueueAdapter.addItem(item);
    }

    public void notifyDataSetChanged() {
        playQueueAdapter.notifyDataSetChanged();
    }

    public void updateNowPlaying(int adapterPosition, boolean notifyListener, boolean isPlaying) {
        playQueueAdapter.updateNowPlaying(adapterPosition, notifyListener, isPlaying);
    }

    public boolean isEmpty() {
        return playQueueAdapter.isEmpty();
    }

    public void removeLoadingIndicator() {
        loadingIndicator.setVisibility(View.GONE);
    }

    public void showUndo() {
        final Feedback feedback = Feedback.create(R.string.track_removed, R.string.undo, view -> playQueuePresenter.undoClicked());
        feedbackController.showFeedback(feedback);
    }

    @OnClick(R.id.close_play_queue)
    void closePlayQueue() {
        playQueuePresenter.closePlayQueue();
    }

    @OnClick(R.id.up_next)
    void onNextClick() {
        playQueuePresenter.onNextClick();
    }

    @OnClick(R.id.shuffle_button)
    void shuffleClicked(ToggleButton toggle) {
        playQueuePresenter.shuffleClicked(toggle.isChecked());
    }

    @OnClick(R.id.repeat_button)
    void repeatClicked() {
        playQueuePresenter.repeatClicked();
    }

    private DefaultItemAnimator buildItemAnimator() {
        final DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setRemoveDuration(150);
        return animator;
    }

    interface DragListener {
        void startDrag(RecyclerView.ViewHolder viewHolder);
    }

    private class PositionScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (dy > 0) {
                playQueuePresenter.scrollDown(layoutManager.findLastVisibleItemPosition());
            } else {
                playQueuePresenter.scrollUp(layoutManager.findFirstVisibleItemPosition());
            }
        }
    }

}
