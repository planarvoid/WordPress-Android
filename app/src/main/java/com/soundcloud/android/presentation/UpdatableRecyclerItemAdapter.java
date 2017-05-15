package com.soundcloud.android.presentation;

import static com.soundcloud.java.collections.Lists.newArrayList;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;

import java.util.List;

/**
 * Special subclass of {@link RecyclerItemAdapter} which is intended to be used in pages without pagination.
 * Note that it is not allowed to clear the adapter manually thus calling {@link UpdatableRecyclerItemAdapter#clear()} ha no effect.
 * If you wish to do so just emit an empty list in the source observable.
 *
 * @param <ItemT>
 * @param <VH>
 */
public abstract class UpdatableRecyclerItemAdapter<ItemT, VH extends RecyclerView.ViewHolder> extends RecyclerItemAdapter<ItemT, VH> {

    protected UpdatableRecyclerItemAdapter(CellRendererBinding<? extends ItemT>... cellRendererBindings) {
        super(cellRendererBindings);
    }

    protected UpdatableRecyclerItemAdapter(CellRenderer<? extends ItemT> cellRenderer) {
        super(cellRenderer);
    }

    @Override
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
    public void clear() {
        // We are relying on DiffUtils so there is no need to clear the adapter.
        // AK calls this method in the CollectionViewPresenter class so we can't throw an exception here.
    }

    @Override
    public void addItem(ItemT item) {
        final int oldSize = getItems().size();
        getItems().add(item);
        notifyItemInserted(oldSize);
    }

    @Override
    public void removeItem(int position) {
        final ItemT removed = getItems().remove(position);
        if (removed != null) {
            notifyItemRemoved(position);
        }
    }

    @Override
    public void prependItem(ItemT item) {
        getItems().add(0, item);
        notifyItemInserted(0);
    }

    public void setItem(int position, ItemT item) {
        getItems().set(position, item);
        notifyItemChanged(position);
    }

    @Override
    public void onNext(Iterable<ItemT> items) {
        replaceItems(newArrayList(items));
    }

    public void replaceItems(List<ItemT> newItems) {
        final List<ItemT> oldItems = getItems();

        final DiffUtil.Callback callback = createDiffUtilCallback(oldItems, newItems);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(callback);

        getItems().clear();
        getItems().addAll(newItems);

        diffResult.dispatchUpdatesTo(this);
    }

    protected abstract DiffUtil.Callback createDiffUtilCallback(List<ItemT> oldList, List<ItemT> newList);
}
