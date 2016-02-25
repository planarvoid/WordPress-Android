package com.soundcloud.android.creators.record;

import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.upload.MetadataFragment;
import com.soundcloud.android.creators.upload.UploadMonitorFragment;
import com.soundcloud.android.creators.upload.UploadService;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import javax.inject.Inject;

public class RecordActivity extends LoggedInActivity {

    private static final String RECORD_FRAGMENT_TAG = "recording_fragment";
    private static final String METADATA_FRAGMENT_TAG = "metadata_fragment";
    private static final String UPLOAD_PROGRESS_FRAGMENT_TAG = "upload_progress_fragment";

    @Inject @LightCycle ActionBarHelper actionBarHelper;

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject EventBus eventBus;
    @Inject SoundRecorder recorder;
    @Inject Navigator navigator;

    private Subscription initialStateSubscription = RxUtils.invalidSubscription();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        super.onCreate(savedInstanceState);
        restoreCurrentFragment();
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setContainerLayout(this);
    }

    private void restoreCurrentFragment() {
        if (getSupportFragmentManager().findFragmentById(R.id.container) == null) {
            initialStateSubscription = eventBus.queue(EventQueue.UPLOAD).first().subscribe(new DefaultSubscriber<UploadEvent>() {
                @Override
                public void onNext(UploadEvent uploadEvent) {
                    if (uploadEvent.isUploading()) {
                        displayMonitor(uploadEvent.getRecording());
                    } else {
                        if (!setRecordingFromIntent(getIntent())) {
                            displayRecord();
                        } else {
                            onRecordToMetadata(false);
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (setRecordingFromIntent(intent)) {
            onRecordToMetadata(false);
        }
    }

    @Override
    protected void onDestroy() {
        initialStateSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public Screen getScreen() {
        // This is a container, screens are the fragments.
        return Screen.UNKNOWN;
    }

    @Override
    public boolean onSupportNavigateUp() {
        navigator.openHome(this);
        finish();
        return true;
    }

    public void trackScreen(ScreenEvent screenEvent) {
        if (screenTracker.isEnteringScreen()) {
            eventBus.publish(EventQueue.TRACKING, screenEvent);
        }
    }

    private Fragment getRecordFragment() {
        return getSupportFragmentManager().findFragmentByTag(RECORD_FRAGMENT_TAG);
    }


    public void onRecordToMetadata(boolean addToBackStack) {
        Fragment fragment = getMetadataFragment();

        if (fragment == null) {
            fragment = MetadataFragment.create();
        }

        transition(fragment, METADATA_FRAGMENT_TAG, addToBackStack);
    }

    public void onUploadToRecord() {
        Fragment fragment = getRecordFragment();

        if (fragment == null) {
            fragment = RecordFragment.create();
        }

        transition(fragment, RECORD_FRAGMENT_TAG, false);
    }

    private void transition(Fragment fragment, String fragmentTag, boolean addToBackStack) {
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.ak_fade_in, R.anim.ak_fade_out, R.anim.ak_fade_in, R.anim.ak_fade_out);
        fragmentTransaction.replace(R.id.container, fragment, fragmentTag);

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

    private boolean setRecordingFromIntent(Intent intent) {
        Recording recording = Recording.fromIntent(intent);

        if (recording != null) {
            recorder.reset();
            recorder.setRecording(recording);
            intent.removeExtra(Recording.EXTRA);
            return true;
        }

        return false;
    }
}
