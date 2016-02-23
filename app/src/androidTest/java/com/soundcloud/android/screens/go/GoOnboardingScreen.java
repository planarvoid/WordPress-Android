package com.soundcloud.android.screens.go;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.GoOnboardingErrorElement;
import com.soundcloud.android.upgrade.GoOnboardingActivity;

import android.support.annotation.StringRes;

public class GoOnboardingScreen extends Screen {
    public GoOnboardingScreen(Han testDriver) {
        super(testDriver);
    }

    @Override
    protected Class getActivity() {
        return GoOnboardingActivity.class;
    }

    public ViewElement startButton() {
        return actionButton(R.string.go_onboarding_start);
    }

    public CollectionScreen clickStartButton() {
        startButton().click();
        return new CollectionScreen(testDriver);
    }

    public GoOnboardingErrorElement clickStartButtonForError() {
        startButton().click();
        return new GoOnboardingErrorElement(testDriver);
    }

    public CollectionScreen clickRetryButton() {
        retryButton().click();
        return new CollectionScreen(testDriver);
    }

    public ViewElement retryButton() {
        return actionButton(R.string.go_onboarding_retry);
    }

    private ViewElement actionButton(@StringRes int string) {
        return testDriver
                .findOnScreenElement(With.text(string))
                .findAncestor(root(), With.classSimpleName("LoadingButton"));
    }

    private ViewElement root() {
        return testDriver.findOnScreenElement(With.id(R.id.go_onboarding_container));
    }

}

