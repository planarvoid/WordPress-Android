package com.soundcloud.android.playback.playqueue;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.TooltipCompat;
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
    private final SmoothScrollLinearLayoutManager layoutManager;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @BindView(R.id.repeat_button) ImageView repeatView;
    @BindView(R.id.shuffle_button) ToggleButton shuffleView;
    @BindView(R.id.loading_indicator) View loadingIndicator;
    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.player_strip) View playerStrip;
    private Unbinder unbinder;

    @Inject
    public PlayQueueView(PlayQueuePresenter playQueuePresenter,
                         PlayQueueSwipeToRemoveCallbackFactory swipeToRemoveCallbackFactory,
                         FeedbackController feedbackController,
                         SmoothScrollLinearLayoutManager layoutManager,
                         PerformanceMetricsEngine performanceMetricsEngine,
                         TrackPlayQueueItemRenderer trackPlayQueueItemRenderer,
                         HeaderPlayQueueItemRenderer headerPlayQueueItemRenderer,
                         MagicBoxPlayQueueItemRenderer magicBoxPlayQueueItemRenderer) {
        this.playQueuePresenter = playQueuePresenter;
        this.playQueueAdapter = new PlayQueueAdapter(
                trackPlayQueueItemRenderer,
                headerPlayQueueItemRenderer,
                magicBoxPlayQueueItemRenderer
        );
        this.feedbackController = feedbackController;
        this.layoutManager = layoutManager;
        this.performanceMetricsEngine = performanceMetricsEngine;
        swipeToRemoveCallback = swipeToRemoveCallbackFactory.create(playQueuePresenter);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);
        initRecyclerView();
        setUpTooltips(view.getResources());
        playQueuePresenter.attachView(this);
    }

    private void setUpTooltips(Resources resources) {
        TooltipCompat.setTooltipText(repeatView, resources.getString(R.string.btn_repeat));
        TooltipCompat.setTooltipText(shuffleView, resources.getString(R.string.btn_shuffle));
    }

    private void initRecyclerView() {
        playQueueAdapter.setHasStableIds(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(playQueueAdapter);
        recyclerView.setHasFixedSize(false);
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
        playQueueAdapter.clear();
        recyclerView.setAdapter(null);
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

    public void scrollTo(int position, boolean animate) {
        if (animate) {
            recyclerView.smoothScrollToPosition(position);
        } else {
            recyclerView.scrollToPosition(position);
        }
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
        performanceMetricsEngine.startMeasuring(MetricType.PLAY_QUEUE_SHUFFLE);
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
