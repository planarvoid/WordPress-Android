package com.soundcloud.android.likes;

import static com.soundcloud.android.actionbar.menu.ActionMenuController.STATE_REMOVE_SYNC;
import static com.soundcloud.android.actionbar.menu.ActionMenuController.STATE_START_SYNC;

import com.soundcloud.android.actionbar.menu.DefaultActionMenuController;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import javax.inject.Inject;

public class TrackLikesActionMenuController extends DefaultSupportFragmentLightCycle<Fragment> {

    private final DefaultActionMenuController actionMenuController;
    private final OfflineContentOperations offlineOperations;

    private Subscription subscription = Subscriptions.empty();

    @Inject
    public TrackLikesActionMenuController(DefaultActionMenuController actionMenuController,
                                          OfflineContentOperations offlineOperations) {
        this.actionMenuController = actionMenuController;
        this.offlineOperations = offlineOperations;
    }

    @Override
    public void onResume(Fragment fragment) {
        fragment.getActivity().supportInvalidateOptionsMenu();
        subscription = offlineOperations.getOfflineLikesSettingsStatus().subscribe(new OfflineLikesSettingSubscriber());
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        actionMenuController.onCreateOptionsMenu(menu, inflater);
        updateOfflineLikesState();
    }

    @Override
    public boolean onOptionsItemSelected(Fragment fragment, MenuItem item) {
        return actionMenuController.onOptionsItemSelected(fragment, item);
    }

    @Override
    public void onPause(Fragment fragment) {
        subscription.unsubscribe();
    }

    private void updateOfflineLikesState() {
        setOfflineLikesSelected(offlineOperations.isOfflineLikedTracksEnabled());
    }

    private void setOfflineLikesSelected(boolean offlineLikes) {
        actionMenuController.setState(offlineLikes
                ? STATE_REMOVE_SYNC
                : STATE_START_SYNC);
    }

    private class OfflineLikesSettingSubscriber extends DefaultSubscriber<Boolean> {
        @Override
        public void onNext(Boolean offlineLikes) {
            setOfflineLikesSelected(offlineLikes);
        }
    }

}
