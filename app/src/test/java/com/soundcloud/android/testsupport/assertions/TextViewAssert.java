package com.soundcloud.android.testsupport.assertions;

import com.soundcloud.java.checks.Preconditions;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import android.view.View;
import android.widget.TextView;

/**
 * Custom assertion class for testing Android TextViews.
 */
public class TextViewAssert extends AbstractAssert<TextViewAssert, TextView> {

    public TextViewAssert(TextView actual) {
        super(actual, TextViewAssert.class);
    }

    public TextViewAssert containsText(String text) {
        Preconditions.checkNotNull(text);
        isNotNull();
        Assertions
                .assertThat(actual.getText().toString().trim())
                .overridingErrorMessage(errorMessage("TextView does not contain: " + text))
                .isEqualTo(text);
        return this;
    }

    public TextViewAssert isVisible() {
        isNotNull();
        Assertions
                .assertThat(actual.getVisibility())
                .overridingErrorMessage(errorMessage("TextView is not VISIBLE"))
                .isEqualTo(View.VISIBLE);
        return this;
    }

    public TextViewAssert isInvisible() {
        isNotNull();
        Assertions
                .assertThat(actual.getVisibility())
                .overridingErrorMessage(errorMessage("TextView is VISIBLE"))
                .isEqualTo(View.INVISIBLE);
        return this;
    }

    private String errorMessage(String message) {
        return message + ". Actual subject: " + actual.getClass().getSimpleName();
    }
}
