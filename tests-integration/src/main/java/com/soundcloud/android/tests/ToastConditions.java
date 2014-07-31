package com.soundcloud.android.tests;

import com.robotium.solo.Condition;
import com.soundcloud.android.tests.with.With;

import java.util.List;


public class ToastConditions {
    private static final int TOAST_ID = android.R.id.message;
    private static final int TOAST_TIMEOUT = 5000;
    private final Waiter waiter;
    private final Han testDriver;

    public ToastConditions(Waiter waiter, Han testDriver) {
        this.waiter = waiter;
        this.testDriver = testDriver;
    }

    public boolean toHaveText(String text) {
        return testDriver.waitForCondition(new ToastHasText(text), TOAST_TIMEOUT);
    }

    private List<ViewElement> getToasts() {
        return testDriver.findElements(With.id(TOAST_ID));
    }

    private class ToastHasText implements Condition {
        private String text;

        public ToastHasText(String text) {
            this.text = text;
        }

        @Override
        public boolean isSatisfied() {
            return hasText(getToasts(), text);
        }

        private boolean hasText(List<ViewElement> viewElementList, String text) {
            for (ViewElement viewElement : viewElementList) {
                if (viewElement.getText().equals(text)) {
                    return true;
                }
            }
            return false;
        }
    }
}
