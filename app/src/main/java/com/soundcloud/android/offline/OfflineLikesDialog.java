package com.soundcloud.android.offline;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.rx.eventbus.EventBus;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class OfflineLikesDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String TAG = "OfflineLikes";

    @Inject OfflineContentOperations offlineOperations;
    @Inject ScreenProvider screenProvider;
    @Inject EventBus eventBus;
    @Inject ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @Inject
    public OfflineLikesDialog() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    OfflineLikesDialog(OfflineContentOperations offlineOperations, ScreenProvider provider, EventBus eventBus) {
        this.offlineOperations = offlineOperations;
        this.screenProvider = provider;
        this.eventBus = eventBus;
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        offlineOperations.enableOfflineLikedTracks().subscribe(new DefaultObserver<>());
        eventBus.publish(EventQueue.TRACKING,
                         OfflineInteractionEvent.fromEnableOfflineLikes(screenProvider.getLastScreenTag()));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = new CustomFontViewBuilder(getActivity())
                .setTitle(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.OFFLINE_LIKES_DIALOG_TITLE))
                .setMessage(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.OFFLINE_LIKES_DIALOG_MESSAGE))
                .get();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.make_offline_available, OfflineLikesDialog.this)
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
    }

}
