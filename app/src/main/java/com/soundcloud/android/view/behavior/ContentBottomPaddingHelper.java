package com.soundcloud.android.view.behavior;

import com.soundcloud.android.R;

import android.support.design.widget.BottomSheetBehavior;
import android.view.View;

import javax.inject.Inject;

public class ContentBottomPaddingHelper {

    static final Object IS_PADDED = new Object();

    private final PlayerBehaviorFactory playerBehaviorFactory;

    @Inject
    public ContentBottomPaddingHelper(PlayerBehaviorFactory playerBehaviorFactory) {
        this.playerBehaviorFactory = playerBehaviorFactory;
    }

    boolean isPlayer(View view) {
        return view.getId() == R.id.player_root;
    }

    void onPlayerChanged(View player, View dependentView) {
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(player);
        PlayerBehavior playerBehavior = playerBehaviorFactory.create(bottomSheetBehavior);
        if (playerBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            removePaddingIfNeeded(playerBehavior, dependentView);
        } else {
            addPaddingIfNeeded(playerBehavior, dependentView);
        }
    }

    private void addPaddingIfNeeded(PlayerBehavior playerBehavior, View dependentView) {
        Object isPadded = dependentView.getTag(R.id.content_view_bottom_padded);
        if (isPadded == null) {
            int collapsedPlayerHeight = playerBehavior.getPeekHeight();
            setPadding(dependentView, dependentView.getPaddingBottom() + collapsedPlayerHeight);
            dependentView.setTag(R.id.content_view_bottom_padded, IS_PADDED);
        }
    }

    private void removePaddingIfNeeded(PlayerBehavior playerBehavior, View dependentView) {
        Object isPadded = dependentView.getTag(R.id.content_view_bottom_padded);
        if (isPadded != null) {
            int collapsedPlayerHeight = playerBehavior.getPeekHeight();
            setPadding(dependentView, dependentView.getPaddingBottom() - collapsedPlayerHeight);
            dependentView.setTag(R.id.content_view_bottom_padded, null);
        }
    }

    private void setPadding(View view, int bottom) {
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), bottom);
    }
}
