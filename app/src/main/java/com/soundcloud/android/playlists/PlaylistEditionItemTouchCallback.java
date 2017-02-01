package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
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

@AutoFactory
class PlaylistEditionItemTouchCallback extends ItemTouchHelper.SimpleCallback {

    private final Paint textPaint;
    private final Paint backgroundPaint;
    private final Rect textBounds;
    private final SwipeToRemoveStyleAttributes styleAttributes;

    private final NewPlaylistDetailFragment view;
    private boolean isDragging;

    PlaylistEditionItemTouchCallback(@Provided Context context, NewPlaylistDetailFragment view) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);

        // TODO: Do we want the same style than the PQ?
        this.styleAttributes = SwipeToRemoveStyleAttributes.from(context);
        this.isDragging = false;
        this.view = view;

        this.backgroundPaint = new Paint();
        this.backgroundPaint.setColor(styleAttributes.backgroundColor);

        this.textPaint = new Paint();
        this.textPaint.setTextSize(styleAttributes.textSize);
        this.textPaint.setColor(styleAttributes.textColor);
        this.textPaint.setTypeface(styleAttributes.font);

        this.textBounds = new Rect();
        if (styleAttributes.removeText.isPresent()) {
            final String removeText = styleAttributes.removeText.get();
            textPaint.getTextBounds(removeText, 0, removeText.length(), textBounds);
        }
    }

    @Override
    public boolean onMove(RecyclerView recyclerView,
                          RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        isDragging = true;
        view.dragItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setBackgroundResource(R.drawable.queue_item_background);
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
        view.removeItem(viewHolder.getAdapterPosition());
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            viewHolder.itemView.setBackgroundResource(R.color.play_queue_higlighted_background);
            view.onDragStarted();
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            if (isDragging) {
                isDragging = false;
                view.onDragStopped();
            }
        }
    }
}
