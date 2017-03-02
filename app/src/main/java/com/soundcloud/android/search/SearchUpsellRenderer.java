package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.checks.Preconditions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class SearchUpsellRenderer implements CellRenderer<UpsellSearchableItem> {

    interface OnUpsellClickListener {
        void onUpsellClicked(Context context);
    }

    private OnUpsellClickListener upsellClickListener;

    @Inject
    SearchUpsellRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return inflater.inflate(R.layout.search_upsell_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UpsellSearchableItem> upsellItems) {
        itemView.findViewById(R.id.search_upsell).setOnClickListener(new UpsellClickListener(upsellClickListener));
    }

    void setUpsellClickListener(OnUpsellClickListener listener) {
        Preconditions.checkArgument(listener != null, "Click listener must not be null");
        this.upsellClickListener = listener;
    }

    private static class UpsellClickListener implements View.OnClickListener {
        private final OnUpsellClickListener listener;

        private UpsellClickListener(OnUpsellClickListener listener) {
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            if (listener != null) {
                listener.onUpsellClicked(view.getContext());
            }
        }
    }
}
