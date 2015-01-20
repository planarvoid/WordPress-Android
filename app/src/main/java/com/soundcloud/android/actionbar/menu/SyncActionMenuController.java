package com.soundcloud.android.actionbar.menu;

import com.soundcloud.android.R;
import com.soundcloud.android.cast.CastConnectionHelper;

import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import javax.inject.Inject;

public class SyncActionMenuController implements ActionMenuController {

    private final CastConnectionHelper castConnectionHelper;
    private MenuItem startSync;
    private MenuItem syncing;
    private MenuItem removeSync;

    @Inject
    public SyncActionMenuController(CastConnectionHelper castConnectionHelper) {
        this.castConnectionHelper = castConnectionHelper;
    }

    @Override
    public void onCreate(Fragment fragment) {
        fragment.setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.offline_sync, menu);
        startSync = menu.findItem(R.id.action_start_sync);
        syncing = menu.findItem(R.id.action_syncing);
        removeSync = menu.findItem(R.id.action_remove_sync);

        castConnectionHelper.addMediaRouterButton(menu, R.id.media_route_menu_item);

        showStartSync();
    }

    @Override
    public boolean onOptionsItemSelected(Fragment fragment, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start_sync:
                // To be implemented
                return true;
            case R.id.action_syncing:
                // To be implemented
                return true;
            case R.id.action_remove_sync:
                // To be implemented
                return true;
            default:
                return false;
        }
    }

    public void showStartSync() {
        startSync.setVisible(true);
        syncing.setVisible(false);
        removeSync.setVisible(false);
    }

    public void showSyncing() {
        startSync.setVisible(false);
        syncing.setVisible(true);
        removeSync.setVisible(false);
    }

    public void showRemoveSync() {
        startSync.setVisible(false);
        syncing.setVisible(false);
        removeSync.setVisible(true);
    }
}
