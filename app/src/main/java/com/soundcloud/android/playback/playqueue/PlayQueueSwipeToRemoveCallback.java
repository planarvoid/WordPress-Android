package com.soundcloud.android.playback.playqueue;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.view.SwipeToRemoveStyleAttributes;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class PlayQueueSwipeToRemoveCallback extends ItemTouchHelper.SimpleCallback {

    private final Paint textPaint;
    private final Paint backgroundPaint;
    private final Rect textBounds;
    private final SwipeToRemoveStyleAttributes styleAttributes;
    private final PlayQueuePresenter presenter;

    private int draggedFromPosition = Consts.NOT_SET;
    private int draggedToPosition = Consts.NOT_SET;

    public PlayQueueSwipeToRemoveCallback(@Provided Context context, PlayQueuePresenter presenter) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT);
        this.presenter = presenter;

        styleAttributes = SwipeToRemoveStyleAttributes.from(context);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(styleAttributes.backgroundColor);

        textPaint = new Paint();
        textPaint.setTextSize(styleAttributes.textSize);
        textPaint.setColor(styleAttributes.textColor);
        textPaint.setTypeface(styleAttributes.font);

        textBounds = new Rect();
        if (styleAttributes.removeText.isPresent()) {
            final String removeText = styleAttributes.removeText.get();
            textPaint.getTextBounds(removeText, 0, removeText.length(), textBounds);
        }
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        if (presenter.isRemovable(target.getAdapterPosition())) {
            draggedToPosition = target.getAdapterPosition();
            presenter.switchItems(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setBackgroundResource(R.drawable.queue_item_background);
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (presenter.isRemovable(viewHolder.getAdapterPosition())) {
            return super.getMovementFlags(recyclerView, viewHolder);
        }
        return ItemTouchHelper.ACTION_STATE_IDLE;
    }

    @Override
    public void onChildDraw(Canvas canvas,
                            RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            float dX,
                            float dY,
                            int actionState,
                            boolean isCurrentlyActive) {
        drawBackground(canvas, dX, viewHolder.itemView);
        drawRemoveText(canvas, dX, dY, viewHolder.itemView);
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawRemoveText(Canvas canvas, float dX, float dY, View itemView) {
        final Optional<String> removeText = styleAttributes.removeText;
        if (removeText.isPresent()) {
            final float x = itemView.getLeft() + dX - textBounds.width() - styleAttributes.textPaddingRight;
            final float y = itemView.getBottom() + dY - (itemView.getHeight() - textBounds.height()) / 2;
            canvas.drawText(removeText.get(), x, y, textPaint);
        }
    }

    private void drawBackground(Canvas canvas, float dX, View itemView) {
        backgroundPaint.setAlpha(backgroundAlphaForPosition(dX, itemView));
        canvas.drawRect(0, itemView.getTop(), dX, itemView.getBottom(), backgroundPaint);
    }

    private int backgroundAlphaForPosition(float dX, View itemView) {
        return (int) (dX * 255 / itemView.getWidth());
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
        presenter.remove(viewHolder.getAdapterPosition());
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            draggedFromPosition = viewHolder.getAdapterPosition();
            viewHolder.itemView.setBackgroundResource(R.color.play_queue_higlighted_background);
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            if (draggedFromPosition != Consts.NOT_SET && draggedToPosition != Consts.NOT_SET) {
                presenter.moveItems(draggedFromPosition, draggedToPosition);
                draggedFromPosition = Consts.NOT_SET;
                draggedToPosition = Consts.NOT_SET;
            }
        }
    }

}
