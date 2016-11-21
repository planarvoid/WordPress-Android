package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class UpsellItemCellRenderer implements CellRenderer<CollectionItem> {

    interface Listener {
        void onUpsellClose(int position);
        void onUpsell(Context context);
    }

    @Nullable private Listener listener;

    @Inject UpsellItemCellRenderer() {}

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.collections_upsell_item, parent, false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<CollectionItem> items) {
        itemView.setEnabled(false);
        if (listener != null) {
            final View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch(v.getId()) {
                        case R.id.close_button:
                            listener.onUpsellClose(position);
                            break;
                        case R.id.upsell_button:
                            listener.onUpsell(v.getContext());
                            break;
                        default:
                            break;
                    }
                }
            };
            itemView.findViewById(R.id.close_button).setOnClickListener(clickListener);
            itemView.findViewById(R.id.upsell_button).setOnClickListener(clickListener);
        }
    }

}
