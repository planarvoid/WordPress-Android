package com.soundcloud.android.upsell;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;
import java.util.List;

public class UpsellItemRenderer<T> implements CellRenderer<T> {

    private final FeatureOperations featureOperations;
    private final FeatureFlags flags;

    public interface Listener {
        void onUpsellItemDismissed(int position);
        void onUpsellItemClicked(Context context, int position);
        void onUpsellItemCreated();
    }

    private Listener listener;

    @Inject
    UpsellItemRenderer(FeatureOperations featureOperations, FeatureFlags flags) {
        this.featureOperations = featureOperations;
        this.flags = flags;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        if (listener != null) {
            listener.onUpsellItemCreated();
        }
        return parent;
    }

    @Override
    public void bindItemView(int position, View view, List<T> items) {
        bindItemView(position, view);
    }

    public void bindItemView(final int position, View view) {
        view.setEnabled(false);
        if (listener != null) {
            ButterKnife.findById(view, R.id.close_button).setOnClickListener(v -> listener.onUpsellItemDismissed(position));
            bindActionButton(view, position);
        }
    }

    private void bindActionButton(final View view, int position) {
        final Button action = ButterKnife.findById(view, R.id.action_button);
        setButtonText(view, action);
        action.setOnClickListener(v -> listener.onUpsellItemClicked(view.getContext(), position));
    }

    private void setButtonText(View view, Button action) {
        if (featureOperations.isHighTierTrialEligible()) {
            action.setText(view.getResources().getString(R.string.conversion_buy_trial,
                    featureOperations.getHighTierTrialDays()));
        } else {
            action.setText(flags.isEnabled(Flag.MID_TIER)
                           ? R.string.upsell_upgrade_button
                           : R.string.upsell_upgrade_button_legacy);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
