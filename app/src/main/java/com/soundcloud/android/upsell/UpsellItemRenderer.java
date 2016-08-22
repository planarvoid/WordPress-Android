package com.soundcloud.android.upsell;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.TypedListItem;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public abstract class UpsellItemRenderer implements CellRenderer<TypedListItem> {

    public interface Listener {
        void onUpsellItemDismissed(int position);
        void onUpsellItemClicked(Context context);
        void onUpsellItemCreated();
    }

    private Listener listener;

    @Override
    public View createItemView(ViewGroup parent) {
        if (listener != null) {
            listener.onUpsellItemCreated();
        }
        return parent;
    }

    @Override
    public void bindItemView(final int position, final View itemView, final List<TypedListItem> items) {
        itemView.setEnabled(false);
        if (listener != null) {
            itemView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onUpsellItemDismissed(position);
                }
            });

            itemView.findViewById(R.id.action_button).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    listener.onUpsellItemClicked(itemView.getContext());
                }
            });
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
