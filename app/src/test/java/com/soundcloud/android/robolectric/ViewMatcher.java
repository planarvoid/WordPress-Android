package com.soundcloud.android.robolectric;

import com.pivotallabs.greatexpectations.matchers.ObjectMatcher;

import android.view.View;
import android.widget.Checkable;
import android.widget.TextView;

public class ViewMatcher<T extends View, M extends ViewMatcher<T, M>> extends ObjectMatcher<T, M> {

    public boolean toHaveText(String expectedText) {
        if (actual instanceof TextView) {
            String actualText = ((TextView) actual).getText().toString();
            failureMessage = String.format("%s to have text <%s> but was <%s>", actual, expectedText, actualText);
            return actualText.equals(expectedText);
        } else {
            failureMessage = "Not a TextView";
            return false;
        }
    }

    public boolean toBeVisible() {
        return actual.getVisibility() == View.VISIBLE;
    }

    public boolean toBeGone() {
        return actual.getVisibility() == View.GONE;
    }

    public boolean toBeInvisible() {
        return actual.getVisibility() == View.INVISIBLE;
    }

    public boolean toBeChecked() {
        if (actual instanceof Checkable) {
            return ((Checkable) actual).isChecked();
        } else {
            failureMessage = "Widget does not implement Checkable";
            return false;
        }
    }

    public boolean toBeEnabled() {
        return actual.isEnabled();
    }

    public boolean toBeDisabled() {
        return !actual.isEnabled();
    }

}
