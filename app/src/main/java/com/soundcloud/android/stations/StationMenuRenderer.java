package com.soundcloud.android.stations;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class StationMenuRenderer implements PopupMenuWrapper.PopupMenuWrapperListener {

    private final Listener listener;
    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    private StationWithTracks station;
    private PopupMenuWrapper menu;

    interface Listener {

        void handleLike(StationWithTracks station);

        void onDismiss();

    }

    StationMenuRenderer(Listener listener,
                        View button,
                        @Provided PopupMenuWrapper.Factory menuFactory,
                        @Provided ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {

        this.listener = listener;
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
        this.menu = menuFactory.build(button.getContext(), button);
        menu.inflate(R.menu.station_item_actions);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(this);
    }

    void render(StationWithTracks station) {
        this.station = station;
        setupMenu();
    }

    private void setupMenu() {
        updateLikeActionTitle(station.isLiked());
        menu.show();
    }

    private void updateLikeActionTitle(boolean isLiked) {
        final String addToLikesString = isLiked
                                        ? changeLikeToSaveExperimentStringHelper.getString(ExperimentString.UNLIKE)
                                        : changeLikeToSaveExperimentStringHelper.getString(ExperimentString.LIKE);
        menu.setItemText(R.id.add_to_likes, addToLikesString);
        menu.setItemVisible(R.id.add_to_likes, true);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem, Context context) {
        switch (menuItem.getItemId()) {
            case R.id.add_to_likes:
                listener.handleLike(station);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDismiss() {
        menu = null;
        listener.onDismiss();
    }

}
