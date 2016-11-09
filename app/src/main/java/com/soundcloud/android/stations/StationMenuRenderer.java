package com.soundcloud.android.stations;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class StationMenuRenderer implements PopupMenuWrapper.PopupMenuWrapperListener {

    private StationWithTracks station;

    interface Listener {

        void handleLike(StationWithTracks station);

        void onDismiss();

    }

    private final Listener listener;

    private PopupMenuWrapper menu;

    StationMenuRenderer(Listener listener,
                        View button,
                        @Provided PopupMenuWrapper.Factory menuFactory) {

        this.listener = listener;
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
        final MenuItem item = menu.findItem(R.id.add_to_likes);
        item.setTitle(isLiked ? R.string.btn_unlike : R.string.btn_like);
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
