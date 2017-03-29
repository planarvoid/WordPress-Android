package com.soundcloud.android.playback.playqueue;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;
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

public class PlayQueueView extends SupportFragmentLightCycleDispatcher<Fragment> implements MagicBoxPlayQueueItemRenderer.MagicBoxListener {

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
        playQueueAdapter.setMagicBoxListener(this);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        recyclerView.setLayoutManager(null);
        unbinder.unbind();
        playQueuePresenter.detachContract();
    }

    public void setGoPlayerStrip() {
        playerStrip.setBackgroundResource(R.drawable.go_gradient);
    }

    public void setDefaultPlayerStrip() {
        playerStrip.setBackgroundResource(R.color.soundcloud_orange);
    }

    public void setShuffledState(boolean shuffled) {
        shuffleView.setChecked(shuffled);
    }

    public void setRepeatOne() {
        repeatView.setImageResource(R.drawable.ic_repeat_one);
    }

    public void setRepeatAll() {
        repeatView.setImageResource(R.drawable.ic_repeat_all);
    }

    public void setRepeatNone() {
        repeatView.setImageResource(R.drawable.ic_repeat_off);
    }

    public void scrollTo(int position) {
        recyclerView.scrollToPosition(position);
    }

    public void switchItems(int fromPosition, int toPosition) {
        playQueueAdapter.switchItems(fromPosition, toPosition);
    }

    public void setItems(List<PlayQueueUIItem> items) {
        playQueueAdapter.clear();
        for (PlayQueueUIItem item : items) {
            playQueueAdapter.addItem(item);
        }
        playQueueAdapter.notifyDataSetChanged();
    }

    public void removeItem(int adapterPosition) {
        playQueueAdapter.removeItem(adapterPosition);
        playQueueAdapter.notifyItemRemoved(adapterPosition);
    }

    public void removeLoadingIndicator() {
        loadingIndicator.setVisibility(View.GONE);
    }

    public void showLoadingIndicator() {
        loadingIndicator.setVisibility(View.VISIBLE);
    }

    public void showUndo(int textId) {
        final Feedback feedback = Feedback.create(textId, R.string.undo, view -> playQueuePresenter.undoClicked());
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

    @Override
    public void clicked() {
        playQueuePresenter.magicBoxClicked();
    }

    @Override
    public void toggle(boolean checked) {
        playQueuePresenter.magicBoxToggled(checked);
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
