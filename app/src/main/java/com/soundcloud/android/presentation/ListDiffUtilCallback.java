package com.soundcloud.android.presentation;

import com.soundcloud.java.objects.MoreObjects;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

/**
 * Implementation of {@link DiffUtil.Callback} intended to be used when the datasource is a {@link List} of items.
 * @param <T>
 */
public abstract class ListDiffUtilCallback<T> extends DiffUtil.Callback {

    private final List<T> oldList;
    private final List<T> newList;

    public ListDiffUtilCallback(List<T> oldList, List<T> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public final boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return areItemsTheSame(oldList.get(oldItemPosition), newList.get(newItemPosition));
    }

    /**
     * Called by the DiffUtil to decide whether two object represent the same Item.
     * See also  {@link DiffUtil.Callback#areItemsTheSame(int, int)}.
     *
     * @param oldItem
     * @param newItem
     * @return
     */
    protected abstract boolean areItemsTheSame(T oldItem, T newItem);

    @Override
    public final boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return areContentsTheSame(oldList.get(oldItemPosition), newList.get(newItemPosition));
    }

    /**
     * Called by the DiffUtil when it wants to check whether two items have the same data.
     * DiffUtil uses this information to detect if the contents of an item has changed.
     * See also  {@link DiffUtil.Callback#areContentsTheSame(int, int)}.
     *
     * @param oldItem
     * @param newItem
     * @return
     */
    protected boolean areContentsTheSame(T oldItem, T newItem) {
        return MoreObjects.equal(oldItem, newItem);
    }

    @Nullable
    @Override
    public final Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return getChangePayload(oldList.get(oldItemPosition), newList.get(newItemPosition));
    }

    /**
     * Used by DiffUtil to calculate the updates to perform when two items are the same but have different content.
     * See also  {@link DiffUtil.Callback#getChangePayload(int, int)}.
     *
     * @param oldItem
     * @param newItem
     * @return
     */
    @Nullable
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    protected Object getChangePayload(T oldItem, T newItem) {
        return null;
    }
}
