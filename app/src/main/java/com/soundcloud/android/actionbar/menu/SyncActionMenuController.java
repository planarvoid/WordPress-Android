package com.soundcloud.android.actionbar.menu;

import com.soundcloud.android.R;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.SyncLikesDialog;
import com.soundcloud.android.payments.SubscribeActivity;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import javax.inject.Inject;
import javax.inject.Provider;

public class SyncActionMenuController implements ActionMenuController {

    private final CastConnectionHelper castConnectionHelper;
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineOperations;
    private final Provider<SyncLikesDialog> syncLikesDialogProvider;

    private MenuItem startSync;
    private MenuItem syncing;
    private MenuItem removeSync;

    @Inject
    public SyncActionMenuController(CastConnectionHelper castConnectionHelper, FeatureOperations featureOperations,
                                    OfflineContentOperations offlineOperations,
                                    Provider<SyncLikesDialog> syncLikesDialogProvider) {
        this.castConnectionHelper = castConnectionHelper;
        this.featureOperations = featureOperations;
        this.offlineOperations = offlineOperations;
        this.syncLikesDialogProvider = syncLikesDialogProvider;
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
                if (featureOperations.isOfflineSyncEnabled()) {
                    syncLikesDialogProvider.get().show(fragment.getFragmentManager());
                } else {
                    upsell(fragment);
                }
                return true;
            case R.id.action_syncing:
                // TODO
                return true;
            case R.id.action_remove_sync:
                offlineOperations.setLikesOfflineSync(false);
                return true;
            default:
                return false;
        }
    }

    private void upsell(Fragment fragment) {
        final Activity activity = fragment.getActivity();
        activity.startActivity(new Intent(activity, SubscribeActivity.class));
    }

    @Override
    public void setState(int state) {
        switch (state) {
            case STATE_START_SYNC:
                showStartSync();
                break;
            case STATE_SYNCING:
                showSyncing();
                break;
            case STATE_REMOVE_SYNC:
                showRemoveSync();
                break;
            default:
                //ignore it
        }
    }

    private void showStartSync() {
        startSync.setVisible(true);
        syncing.setVisible(false);
        removeSync.setVisible(false);
    }

    private void showSyncing() {
        startSync.setVisible(false);
        syncing.setVisible(true);
        removeSync.setVisible(false);
    }

    private void showRemoveSync() {
        startSync.setVisible(false);
        syncing.setVisible(false);
        removeSync.setVisible(true);
    }
}
