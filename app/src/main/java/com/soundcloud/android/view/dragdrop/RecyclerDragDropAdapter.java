/*
 * Copyright (C) 2015 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.android.view.dragdrop;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.java.optional.Optional;

import android.graphics.Color;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;

import java.util.Collections;

// https://github.com/iPaulPro/Android-ItemTouchHelper-Demo/blob/master/app/src/main/java/co/paulburke/android/itemtouchhelperdemo/RecyclerListAdapter.java
public abstract class RecyclerDragDropAdapter<ItemT, VH extends RecyclerDragDropAdapter.ViewHolder>
        extends RecyclerItemAdapter<ItemT, VH>
        implements DragDropAdapter {

    private final OnStartDragListener listener;

    public RecyclerDragDropAdapter(OnStartDragListener listener,
                                   CellRendererBinding<? extends ItemT>... cellRendererBindings) {
        super(cellRendererBindings);
        this.listener = listener;
    }

    public RecyclerDragDropAdapter(OnStartDragListener listener, CellRenderer<? extends ItemT> cellRenderer) {
        super(cellRenderer);
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(final VH holder, int position) {
        super.onBindViewHolder(holder, position);

        // Start a drag whenever the handle view it touched
        if (holder.handleView.isPresent()) {
            holder.handleView.get().setOnTouchListener((v, event) -> {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    listener.onStartDrag(holder);
                }
                return false;
            });
        }
    }

    @Override
    public void onItemDismiss(int position) {
        items.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(items, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    /**
     * Simple example of a view holder that implements {@link ItemTouchHelperViewHolder} and has a
     * "handle" view that initiates a drag event when touched.
     */
    public static class ViewHolder extends RecyclerItemAdapter.ViewHolder implements
            ItemTouchHelperViewHolder {

        public final Optional<View> handleView;

        public ViewHolder(View itemView) {
            super(itemView);
            handleView = Optional.fromNullable(itemView.findViewById(R.id.handle));
        }

        @Override
        public void onItemSelected() {
            // TODO: Update the color the SoundCloud color
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }
}
