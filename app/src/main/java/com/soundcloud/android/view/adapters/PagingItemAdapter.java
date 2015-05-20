package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.utils.AndroidUtils.assertOnUiThread;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ErrorUtils;

import android.view.View;
import android.view.ViewGroup;

public class PagingItemAdapter<T> extends ItemAdapter<T> implements ReactiveAdapter<Iterable<T>> {

    private final int progressItemLayoutResId;

    private AppendState appendState = AppendState.IDLE;
    private View.OnClickListener onErrorRetryListener;

    protected enum AppendState {
        IDLE, LOADING, ERROR
    }

    public PagingItemAdapter(CellPresenter<T> cellPresenter) {
        this(R.layout.list_loading_item, cellPresenter);
    }

    public PagingItemAdapter(int progressItemLayoutResId, CellPresenter<T> cellPresenter) {
        super(cellPresenter);
        this.progressItemLayoutResId = progressItemLayoutResId;
    }

    public PagingItemAdapter(CellPresenterEntity<?>... cellPresenterEntities) {
        super(cellPresenterEntities);
        this.progressItemLayoutResId = R.layout.list_loading_item;
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
                            + items.size() + "; count=" + getCount()));
        }
        
        return appendState != AppendState.IDLE && position == items.size() ? IGNORE_ITEM_VIEW_TYPE
                 : super.getItemViewType(position);
    }

    public void setLoading() {
        setNewAppendState(AppendState.LOADING);
        notifyDataSetChanged();
    }

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
