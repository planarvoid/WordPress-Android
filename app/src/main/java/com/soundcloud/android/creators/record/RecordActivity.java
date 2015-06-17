package com.soundcloud.android.creators.record;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.upload.MetadataFragment;
import com.soundcloud.android.creators.upload.UploadMonitorFragment;
import com.soundcloud.android.creators.upload.UploadService;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.LightCycle;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import javax.inject.Inject;

public class RecordActivity extends ScActivity {

    private static final String RECORD_FRAGMENT_TAG = "recording_fragment";
    private static final String METADATA_FRAGMENT_TAG = "metadata_fragment";
    private static final String UPLOAD_PROGRESS_FRAGMENT_TAG = "upload_progress_fragment";

    @Inject @LightCycle ActionBarController actionBarController;
    @Inject EventBus eventBus;

    private Subscription initialStateSubscription = Subscriptions.empty();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        super.onCreate(savedInstanceState);
        restoreCurrentFragment();
    }

    @Override
    protected void setContentView() {
        presenter.setContainerLayout();
    }

    private void restoreCurrentFragment() {
        if (getSupportFragmentManager().findFragmentById(R.id.container) == null) {
            initialStateSubscription = eventBus.queue(EventQueue.UPLOAD).first().subscribe(new DefaultSubscriber<UploadEvent>() {
                @Override
                public void onNext(UploadEvent uploadEvent) {
                    if (uploadEvent.isUploading()) {
                        displayMonitor(uploadEvent.getRecording());
                    } else {
                        displayRecord();
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        initialStateSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        startActivity(new Intent(Actions.STREAM).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
        return super.onSupportNavigateUp();
    }

    public void trackScreen(ScreenEvent screenEvent) {
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, screenEvent);
        }
    }

    private Fragment getRecordFragment() {
        return getSupportFragmentManager().findFragmentByTag(RECORD_FRAGMENT_TAG);
    }


    public void onRecordToMetadata() {
        Fragment fragment = getMetadataFragment();

        if (fragment == null) {
            fragment = MetadataFragment.create();
        }

        transition(fragment, METADATA_FRAGMENT_TAG, true);
    }

    public void onUploadToRecord() {
        Fragment fragment = getRecordFragment();

        if (fragment == null) {
            fragment = RecordFragment.create();
        }

        transition(fragment, RECORD_FRAGMENT_TAG, false);
    }

    private void transition(Fragment fragment, String fragmentTag, boolean addToBackStack) {
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.ak_fade_in, R.anim.ak_fade_out, R.anim.ak_fade_in, R.anim.ak_fade_out)
                .replace(R.id.container, fragment, fragmentTag);

        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }

        fragmentTransaction.commit();
    }

    public void displayRecord() {
        Fragment fragment = getRecordFragment();
        if (fragment == null) {
            fragment = RecordFragment.create();
        }

        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.ak_fade_in, R.anim.ak_fade_out)
                .replace(R.id.container, fragment, RECORD_FRAGMENT_TAG)
                .commit();
    }

    public void displayMonitor(Recording recording) {
        Fragment fragment = getUploadMonitorFragment();
        if (fragment == null) {
            fragment = UploadMonitorFragment.create(recording);
        }

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.ak_fade_in, R.anim.ak_fade_out)
                .replace(R.id.container, fragment, UPLOAD_PROGRESS_FRAGMENT_TAG)
                .commit();
    }


    public void startUpload(Recording recording) {
        Intent intent = new Intent(this, UploadService.class);
        startService(intent);
        eventBus.publish(EventQueue.UPLOAD, UploadEvent.start(recording));
    }

    public void onMonitorToUpload(Recording recording) {
        Fragment fragment = getUploadMonitorFragment();
        if (fragment == null) {
            fragment = UploadMonitorFragment.create(recording);
        }

        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        transition(fragment, UPLOAD_PROGRESS_FRAGMENT_TAG, false);
    }


    private UploadMonitorFragment getUploadMonitorFragment() {
        return (UploadMonitorFragment) getSupportFragmentManager().findFragmentByTag(UPLOAD_PROGRESS_FRAGMENT_TAG);
    }

    private MetadataFragment getMetadataFragment() {
        return (MetadataFragment) getSupportFragmentManager().findFragmentByTag(METADATA_FRAGMENT_TAG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case Consts.RequestCodes.GALLERY_IMAGE_PICK:
            case Consts.RequestCodes.GALLERY_IMAGE_TAKE:
            case Crop.REQUEST_CROP:
                Fragment fragment = getMetadataFragment();
                if (fragment != null) {
                    fragment.onActivityResult(requestCode, resultCode, result);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown requestCode: " + requestCode);
        }
    }
}
