package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class OnboardingItemCellRenderer implements CellRenderer<CollectionItem> {

    interface Listener {
        void onCollectionsOnboardingItemClosed(int position);
    }

    @Nullable private Listener listener;

    @Inject
    OnboardingItemCellRenderer() {}

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.collections_onboarding_item, parent, false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<CollectionItem> items) {
        itemView.setEnabled(false);
        if (listener != null) {
            itemView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onCollectionsOnboardingItemClosed(position);
                }
            });
        }
    }

}
