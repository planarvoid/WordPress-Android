package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class UpsellNotificationItemRenderer implements CellRenderer<StreamItem> {

    interface Listener {
        void onUpsellItemDismissed(int position);

        void onUpsellItemClicked();

        void onUpsellItemCreated();
    }

    private Listener listener;

    @Inject
    public UpsellNotificationItemRenderer() {
        // no - op
    }

    @Override
    public View createItemView(ViewGroup parent) {
        if (listener != null) {
            listener.onUpsellItemCreated();
        }
        return LayoutInflater.from(parent.getContext())
                .inflate(R.layout.stream_upsell_card, parent, false);
    }

    @Override
    public void bindItemView(final int position, View itemView, List<StreamItem> items) {
        itemView.setEnabled(false);
        if (listener != null) {
            itemView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onUpsellItemDismissed(position);
                }
            });

            itemView.findViewById(R.id.invite_button).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    listener.onUpsellItemClicked();
                }
            });
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
