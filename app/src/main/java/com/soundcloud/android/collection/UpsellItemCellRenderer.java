package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class UpsellItemCellRenderer implements CellRenderer<CollectionItem> {

    private final FeatureOperations featureOperations;
    private final FeatureFlags flags;

    interface Listener {
        void onUpsellClose(int position);
        void onUpsell(Context context);
    }

    @Nullable private Listener listener;

    @Inject UpsellItemCellRenderer(FeatureOperations featureOperations, FeatureFlags flags) {
        this.featureOperations = featureOperations;
        this.flags = flags;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.collections_upsell_item, parent, false);
        ((TextView) layout.findViewById(R.id.title)).setText(flags.isEnabled(Flag.MID_TIER)
                                                             ? R.string.collections_upsell_title
                                                             : R.string.collections_upsell_title_legacy);
        return layout;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<CollectionItem> items) {
        itemView.setEnabled(false);
        if (listener != null) {
            final View.OnClickListener clickListener = v -> {
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
            };
            itemView.findViewById(R.id.close_button).setOnClickListener(clickListener);
            Button upgrade = (Button) itemView.findViewById(R.id.upsell_button);
            configureUpgradeButton(upgrade, clickListener);
        }
    }

    private void configureUpgradeButton(Button upgrade, View.OnClickListener clickListener) {
        upgrade.setOnClickListener(clickListener);
        if (featureOperations.isHighTierTrialEligible()) {
            upgrade.setText(upgrade.getResources().getString(R.string.conversion_buy_trial,
                    featureOperations.getHighTierTrialDays()));
        } else {
            upgrade.setText(flags.isEnabled(Flag.MID_TIER)
                            ? R.string.upsell_upgrade_button
                            : R.string.upsell_upgrade_button_legacy);
        }
    }

}
