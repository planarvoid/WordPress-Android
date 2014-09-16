package com.soundcloud.android.view.adapters;

import com.soundcloud.android.R;

import android.view.View;
import android.view.ViewGroup;

public class EndlessAdapter<T> extends ItemAdapter<T> implements ReactiveAdapter<Iterable<T>> {

    private final int progressItemLayoutResId;

    private AppendState appendState = AppendState.IDLE;
    private View.OnClickListener onErrorRetryListener;

    protected enum AppendState {
        IDLE, LOADING, ERROR;
    }

    public EndlessAdapter(int progressItemLayoutResId, CellPresenter<T> cellPresenter) {
        super(cellPresenter);
        this.progressItemLayoutResId = progressItemLayoutResId;
    }

    public EndlessAdapter(CellPresenterEntity<T>... cellPresenterEntities) {
        this(R.layout.list_loading_item, cellPresenterEntities);
    }

    public EndlessAdapter(int progressItemLayoutResId, CellPresenterEntity<T>... cellPresenterEntities) {
        super(cellPresenterEntities);
        this.progressItemLayoutResId = progressItemLayoutResId;
    }

    public void setOnErrorRetryListener(View.OnClickListener onErrorRetryListener) {
        this.onErrorRetryListener = onErrorRetryListener;
    }

    @Override
    public int getCount() {
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
                appendingLayout.findViewById(R.id.list_loading_view).setVisibility(View.VISIBLE);
                appendingLayout.findViewById(R.id.list_loading_retry_view).setVisibility(View.GONE);
                appendingLayout.setOnClickListener(null);
                break;
            case ERROR:
                appendingLayout.setBackgroundResource(R.drawable.list_selector_gray);
                appendingLayout.findViewById(R.id.list_loading_view).setVisibility(View.GONE);
                appendingLayout.findViewById(R.id.list_loading_retry_view).setVisibility(View.VISIBLE);
                appendingLayout.setOnClickListener(onErrorRetryListener);
                break;
            default:
                throw new IllegalStateException("Unexpected idle state with progress row");
        }
    }

    private void setNewAppendState(AppendState newState) {
        appendState = newState;
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return appendState != AppendState.IDLE && position == items.size() ? IGNORE_ITEM_VIEW_TYPE
                 : super.getItemViewType(position);
    }

    public void setLoading() {
        setNewAppendState(AppendState.LOADING);
    }

    public boolean isIdle() {
        return appendState == AppendState.IDLE;
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
        setNewAppendState(AppendState.ERROR);
    }

    @Override
    public void onNext(Iterable<T> items) {
        for (T item : items) {
            addItem(item);
        }
        setNewAppendState(AppendState.IDLE);
        notifyDataSetChanged();
    }
}
