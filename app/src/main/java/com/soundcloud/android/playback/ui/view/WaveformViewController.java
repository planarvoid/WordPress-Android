package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.playback.Player.PlayerState.BUFFERING;
import static com.soundcloud.android.playback.Player.PlayerState.IDLE;
import static com.soundcloud.android.playback.Player.PlayerState.PLAYING;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_CANCELLED;
import static com.soundcloud.android.playback.ui.progress.ScrubController.SCRUB_STATE_SCRUBBING;

import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ProgressController;
import com.soundcloud.android.playback.ui.progress.ScrollXHelper;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.progress.TranslateXHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.waveform.WaveformData;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.view.View;

import javax.inject.Inject;
import java.util.BitSet;

public class WaveformViewController implements ScrubController.OnScrubListener, ProgressAware, WaveformView.OnWidthChangedListener {

    private static final int NUM_FLAGS = 5;

    private static final int IS_FOREGROUND = 0;
    private static final int HAS_WIDTH = 1;
    private static final int IS_EXPANDED = 2;
    private static final int HAS_WAVEFORM_DATA = 3;
    private static final int IS_CREATION_PENDING = 4;

    private static final BitSet SHOULD_CREATE_WAVEFORM = trueSet(NUM_FLAGS);
    private final BitSet createState = new BitSet(NUM_FLAGS);

    private final WaveformView waveformView;
    private final float waveformWidthRatio;
    private final ProgressController leftProgressController;
    private final ProgressController rightProgressController;
    private final ProgressController dragProgressController;

    private final ScrubController scrubController;

    private boolean isCollapsed;
    private boolean canShow = true;

    private TranslateXHelper leftProgressHelper;
    private TranslateXHelper rightProgressHelper;

    private Observable<WaveformData> waveformObservable;
    private Subscription waveformSubscription = RxUtils.invalidSubscription();

    private int adjustedWidth;
    private boolean suppressProgress;
    private boolean isWaveformLoaded;

    private PlaybackProgress latestProgress = PlaybackProgress.empty();
    private Player.PlayerState currentState = IDLE;

    WaveformViewController(WaveformView waveform,
                           ProgressController.Factory animationControllerFactory,
                           final ScrubController.Factory scrubControllerFactory) {
        this.waveformView = waveform;
        this.waveformWidthRatio = waveform.getWidthRatio();
        this.scrubController = scrubControllerFactory.create(waveformView.getDragViewHolder());

        createState.set(IS_CREATION_PENDING);

        waveformView.setOnWidthChangedListener(this);
        scrubController.addScrubListener(this);

        leftProgressController = animationControllerFactory.create(waveformView.getLeftWaveform());
        rightProgressController = animationControllerFactory.create(waveformView.getRightWaveform());
        dragProgressController = animationControllerFactory.create(waveformView.getDragViewHolder());
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        suppressProgress = newScrubState == SCRUB_STATE_SCRUBBING;
        if (suppressProgress) {
            cancelProgressAnimations();
        }
        if (newScrubState == SCRUB_STATE_CANCELLED && currentState == PLAYING) {
            startProgressAnimations(latestProgress);
        }
    }

    @Override
    public void displayScrubPosition(float scrubPosition) {
        leftProgressHelper.setValueFromProportion(waveformView.getLeftWaveform(), scrubPosition);
        rightProgressHelper.setValueFromProportion(waveformView.getRightWaveform(), scrubPosition);
        if (currentState == IDLE) {
            leftProgressHelper.setValueFromProportion(waveformView.getLeftLine(), scrubPosition);
            rightProgressHelper.setValueFromProportion(waveformView.getRightLine(), scrubPosition);
        }
    }

    public void setProgress(PlaybackProgress progress) {
        latestProgress = progress;
        if (!progress.isEmpty()) {
            scrubController.setDuration(progress.getDuration());
        }
        if (!suppressProgress) {
            leftProgressController.setPlaybackProgress(progress);
            rightProgressController.setPlaybackProgress(progress);
            dragProgressController.setPlaybackProgress(progress);

            if (currentState == IDLE) {
                waveformView.showIdleLinesAtWaveformPositions();
            }
        }
    }

    @Override
    public void onWaveformViewWidthChanged(int newWidth) {
        adjustedWidth = (int) (waveformWidthRatio * newWidth);
        waveformView.setWaveformWidths(adjustedWidth);

        final int middle = newWidth / 2;
        waveformView.setWaveformTranslations(middle, 0);

        leftProgressHelper = new TranslateXHelper(middle, middle - adjustedWidth);
        leftProgressController.setHelper(leftProgressHelper);

        rightProgressHelper = new TranslateXHelper(0, -adjustedWidth);
        rightProgressController.setHelper(rightProgressHelper);

        final ScrollXHelper dragProgressHelper = new ScrollXHelper(0, adjustedWidth);
        dragProgressController.setHelper(dragProgressHelper);
        scrubController.setProgressHelper(dragProgressHelper);

        setProgress(latestProgress);
        createWaveforms(HAS_WIDTH);
    }

    public void setWaveform(Observable<WaveformData> waveformObservable, boolean isForeground) {
        this.waveformObservable = waveformObservable;
        createState.set(IS_CREATION_PENDING);
        createWaveforms(HAS_WAVEFORM_DATA);
        if (isForeground) {
            onForeground();
        } else {
            onBackground();
        }
    }

    public void reset() {
        waveformSubscription.unsubscribe(); // Matthias, help test this
        waveformObservable = null;
        isWaveformLoaded = false;
        leftProgressController.reset();
        rightProgressController.reset();
        dragProgressController.reset();
        createState.clear(HAS_WAVEFORM_DATA);
    }

    public void showPlayingState(PlaybackProgress progress) {
        currentState = PLAYING;
        showWaveform();
        if (!suppressProgress) {
            startProgressAnimations(progress);
        }
    }

    private void showWaveform() {
        if (isWaveformLoaded) {
            waveformView.showExpandedWaveform();
        } else {
            waveformView.showIdleLinesAtWaveformPositions();
        }
    }

    public void showBufferingState() {
        currentState = BUFFERING;
        showWaveform();
        cancelProgressAnimations();
    }

    public void showIdleState() {
        currentState = IDLE;
        // TODO: still necessary since we dropped NineOldAndroids?
        waveformView.showIdleLinesAtWaveformPositions();
        waveformView.showCollapsedWaveform();
        cancelProgressAnimations();
    }

    public void onPlayerSlide(float value) {
        isCollapsed = false;
        if (canShow) {
            waveformView.setVisibility(value > 0 ? View.VISIBLE : View.GONE);
        }
    }

    public void setExpanded() {
        isCollapsed = false;
        createWaveforms(IS_EXPANDED);
        if (canShow) {
            waveformView.setVisibility(View.VISIBLE);
        }
    }

    public void setCollapsed() {
        isCollapsed = true;
        createState.clear(IS_EXPANDED);
        waveformView.setVisibility(View.GONE);
    }

    public void show() {
        canShow = true;
        if (!isCollapsed) {
            waveformView.setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        canShow = false;
        waveformView.setVisibility(View.GONE);
    }

    public void setDuration(long duration) {
        scrubController.setDuration(duration);
    }

    public void onBackground() {
        createState.clear(IS_FOREGROUND);
        createState.set(IS_CREATION_PENDING);
    }

    public void onForeground() {
        createWaveforms(IS_FOREGROUND);
    }

    private void startProgressAnimations(PlaybackProgress progress) {
        leftProgressController.startProgressAnimation(progress);
        rightProgressController.startProgressAnimation(progress);
        dragProgressController.startProgressAnimation(progress);
    }

    public void cancelProgressAnimations() {
        leftProgressController.cancelProgressAnimation();
        rightProgressController.cancelProgressAnimation();
        dragProgressController.cancelProgressAnimation();
    }

    public void addScrubListener(ScrubController.OnScrubListener listener) {
        scrubController.addScrubListener(listener);
    }

    private void createWaveforms(int flag) {
        createState.set(flag);
        if (createState.equals(SHOULD_CREATE_WAVEFORM)) {
            waveformSubscription.unsubscribe();
            waveformSubscription = waveformObservable
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new WaveformSubscriber());
            createState.clear(IS_CREATION_PENDING);
        }
    }

    private class WaveformSubscriber extends DefaultSubscriber<WaveformData> {
        @Override
        public void onNext(WaveformData waveformData) {
            isWaveformLoaded = true;
            waveformView.setWaveformData(waveformData, adjustedWidth);
            if (currentState != IDLE) {
                waveformView.showExpandedWaveform();
            }
        }
    }

    public static class Factory {
        private final ProgressController.Factory animationControllerFactory;
        private final ScrubController.Factory scrubControllerFactory;

        @Inject
        Factory(ScrubController.Factory scrubControllerFactory,
                ProgressController.Factory animationControllerFactory) {
            this.scrubControllerFactory = scrubControllerFactory;
            this.animationControllerFactory = animationControllerFactory;
        }

        public WaveformViewController create(WaveformView waveformView) {
            return new WaveformViewController(waveformView, animationControllerFactory, scrubControllerFactory
            );
        }
    }

    private static BitSet trueSet(int numFlags) {
        BitSet bitSet = new BitSet(numFlags);
        bitSet.set(0, numFlags);
        return bitSet;
    }

}
