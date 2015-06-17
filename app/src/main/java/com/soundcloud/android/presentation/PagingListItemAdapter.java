package com.soundcloud.android.presentation;

import static com.soundcloud.android.utils.AndroidUtils.assertOnUiThread;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.adapters.ReactiveAdapter;

import android.view.View;
import android.view.ViewGroup;

public class PagingListItemAdapter<T> extends ListItemAdapter<T> implements ReactiveAdapter<Iterable<T>>, PagingAwareAdapter<T> {

    private final int progressItemLayoutResId;

    private AppendState appendState = AppendState.IDLE;
    private View.OnClickListener onErrorRetryListener;

    protected enum AppendState {
        IDLE, LOADING, ERROR
    }

    public PagingListItemAdapter(CellRenderer<T> cellRenderer) {
        this(R.layout.ak_list_loading_item, cellRenderer);
    }

    public PagingListItemAdapter(int progressItemLayoutResId, CellRenderer<T> cellRenderer) {
        super(cellRenderer);
        this.progressItemLayoutResId = progressItemLayoutResId;
    }

    public PagingListItemAdapter(CellRendererBinding<? extends T>... cellRendererBindings) {
        super(cellRendererBindings);
        this.progressItemLayoutResId = R.layout.ak_list_loading_item;
    }

    @Override
    public void setOnErrorRetryListener(View.OnClickListener onErrorRetryListener) {
        this.onErrorRetryListener = onErrorRetryListener;
    }

    @Override
    public int getItemCount() {
        if (items.isEmpty()) {
            return 0;
        } else {
            return appendState == AppendState.IDLE ? items.size() : items.size() + 1;
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != IGNORE_ITEM_VIEW_TYPE;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == IGNORE_ITEM_VIEW_TYPE) {
            final View view;
            if (convertView == null) {
                view = View.inflate(parent.getContext(), progressItemLayoutResId, null);
            } else {
                view = convertView;
            }
            configureAppendingLayout(view);
            return view;

        } else {
            return super.getView(position, convertView, parent);
        }
    }

    private void configureAppendingLayout(final View appendingLayout) {
        switch (appendState) {
            case LOADING:
                appendingLayout.setBackgroundResource(android.R.color.transparent);
                appendingLayout.findViewById(R.id.ak_list_progress).setVisibility(View.VISIBLE);
                appendingLayout.findViewById(R.id.ak_list_retry).setVisibility(View.GONE);
                appendingLayout.setOnClickListener(null);
                break;
            case ERROR:
                appendingLayout.setBackgroundResource(R.drawable.ak_list_selector_gray);
                appendingLayout.findViewById(R.id.ak_list_progress).setVisibility(View.GONE);
                appendingLayout.findViewById(R.id.ak_list_retry).setVisibility(View.VISIBLE);
                appendingLayout.setOnClickListener(onErrorRetryListener);
                break;
            default:
                throw new IllegalStateException("Unexpected idle state with progress row");
        }
    }

    private void setNewAppendState(AppendState newState) {
        assertOnUiThread("Adapter should always be uses on UI Thread. Tracking issue #2377");
        appendState = newState;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (isIdle() && position == items.size()) {
            ErrorUtils.handleSilentException(new IllegalStateException(
                    "This position is invalid in Idle state. Tracking issue #2377; position=" + position + "; items="
                            + items.size() + "; count=" + getItemCount()));
        }
        
        return appendState != AppendState.IDLE && position == items.size() ? IGNORE_ITEM_VIEW_TYPE
                 : super.getItemViewType(position);
    }

    @Override
    public void setLoading() {
        setNewAppendState(AppendState.LOADING);
        notifyDataSetChanged();
    }

    @Override
    public boolean isIdle() {
        return appendState == AppendState.IDLE;
    }

    @Override
    public void onError(Throwable e) {
        super.onError(e);
        setNewAppendState(AppendState.ERROR);
        notifyDataSetChanged();
    }

    @Override
    public void onNext(Iterable<T> items) {
        setNewAppendState(AppendState.IDLE);
        super.onNext(items);
    }
}
