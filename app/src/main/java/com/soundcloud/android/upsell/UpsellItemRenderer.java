package com.soundcloud.android.upsell;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.TypedListItem;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.List;

public abstract class UpsellItemRenderer implements CellRenderer<TypedListItem> {

    private final FeatureOperations featureOperations;

    public interface Listener {
        void onUpsellItemDismissed(int position);
        void onUpsellItemClicked(Context context);
        void onUpsellItemCreated();
    }

    private Listener listener;

    UpsellItemRenderer(FeatureOperations featureOperations) {
        this.featureOperations = featureOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        if (listener != null) {
            listener.onUpsellItemCreated();
        }
        return parent;
    }

    @Override
    public void bindItemView(final int position, final View view, final List<TypedListItem> items) {
        view.setEnabled(false);
        if (listener != null) {
            ButterKnife.findById(view, R.id.close_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onUpsellItemDismissed(position);
                }
            });
            bindActionButton(view);
        }
    }

    private void bindActionButton(final View view) {
        final Button action = ButterKnife.findById(view, R.id.action_button);
        setButtonText(view, action);
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onUpsellItemClicked(view.getContext());
            }
        });
    }

    private void setButtonText(View view, Button action) {
        if (featureOperations.isHighTierTrialEligible()) {
            action.setText(view.getResources().getString(R.string.conversion_buy_trial,
                    featureOperations.getHighTierTrialDays()));
        } else {
            action.setText(R.string.upsell_upgrade_button);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
