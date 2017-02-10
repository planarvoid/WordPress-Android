package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

class EmptyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Status status = Status.WAITING;
    private EmptyViewWrapper emptyStateProvider;


    enum Status {
        WAITING,
        ERROR,
        CONNECTION_ERROR,
        SERVER_ERROR,
        OK
    }

    EmptyAdapter(CollectionRenderer.EmptyStateProvider emptyStateProvider, boolean renderEmptyAtTop) {
        this.emptyStateProvider = new EmptyViewWrapper(emptyStateProvider, renderEmptyAtTop);
    }

    public void setStatus(Status status) {
        if (this.status != status) {
            this.status = status;
            notifyItemChanged(0);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(getEmptyView(parent, Status.values()[viewType])) {
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // noop
    }

    private View getEmptyView(ViewGroup parent, Status status) {
        switch (status) {
            case WAITING:
                return emptyStateProvider.waitingView(parent);
            case CONNECTION_ERROR:
                return emptyStateProvider.connectionErrorView(parent);
            case SERVER_ERROR:
                return emptyStateProvider.serverErrorView(parent);
            case OK:
                return emptyStateProvider.emptyView(parent);
            default:
                throw new IllegalArgumentException("Unknown status " + status);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return status.ordinal();
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    private static class EmptyViewWrapper {

        private CollectionRenderer.EmptyStateProvider emptyStateProvider;
        private View waitingView;
        private View connectionErrorView;
        private View serverErrorView;
        private View emptyView;
        private final boolean renderEmptyAtTop;

        EmptyViewWrapper(CollectionRenderer.EmptyStateProvider emptyStateProvider,
                         boolean renderEmptyAtTop) {
            this.emptyStateProvider = emptyStateProvider;
            this.renderEmptyAtTop = renderEmptyAtTop;
        }

        View waitingView(ViewGroup parent) {
            if (waitingView == null) {
                waitingView = getWrappedEmptyView(parent, emptyStateProvider.waitingView());
            }
            return waitingView;
        }

        View connectionErrorView(ViewGroup parent) {
            if (connectionErrorView == null) {
                connectionErrorView = getWrappedEmptyView(parent, emptyStateProvider.connectionErrorView());
            }
            return connectionErrorView;
        }

        View serverErrorView(ViewGroup parent) {
            if (serverErrorView == null) {
                serverErrorView = getWrappedEmptyView(parent, emptyStateProvider.serverErrorView());
            }
            return serverErrorView;
        }

        public View emptyView(ViewGroup parent) {
            if (emptyView == null) {
                emptyView = getWrappedEmptyView(parent, emptyStateProvider.emptyView());
            }
            return emptyView;
        }

        private View getWrappedEmptyView(ViewGroup parent, int emptyViewLayout) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            RelativeLayout rootView = (RelativeLayout) inflater.inflate(R.layout.emptyview_container, parent, false);

            emptyView = inflater.inflate(emptyViewLayout, rootView, false);
            rootView.addView(emptyView, getEmptyItemParams());
            return rootView;
        }

        private RelativeLayout.LayoutParams getEmptyItemParams() {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (renderEmptyAtTop) {
                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            } else {
                params.addRule(RelativeLayout.CENTER_IN_PARENT);
            }
            return params;
        }

    }
}
