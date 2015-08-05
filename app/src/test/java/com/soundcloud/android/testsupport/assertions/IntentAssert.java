package com.soundcloud.android.testsupport.assertions;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.java.checks.Preconditions;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import android.content.Intent;
import android.net.Uri;

/**
 * Custom assertion class for testing Android Intents.
 */
public final class IntentAssert extends AbstractAssert<IntentAssert, Intent> {

    protected IntentAssert(Intent actual) {
        super(actual, IntentAssert.class);
    }

    public IntentAssert containsAction(String action) {
        Preconditions.checkNotNull(action);
        isNotNull();
        Assertions
                .assertThat(actual.getAction())
                .overridingErrorMessage(errorMessage("Intent does not contain actions: " + action))
                .isEqualTo(action);
        return this;
    }

    public IntentAssert containsExtra(String key, Object value) {
        Preconditions.checkNotNull(key);
        isNotNull();
        Assertions
                .assertThat(actual.getExtras().get(key))
                .overridingErrorMessage(errorMessage("Intent does not contain extra. Key: " + key + ", value: " + value))
                .isEqualTo(value);
        return this;
    }

    public IntentAssert intentExtraIsNotNull(String key) {
        Preconditions.checkNotNull(key);
        isNotNull();
        Assertions
                .assertThat(actual.getExtras().get(key))
                .overridingErrorMessage(errorMessage("Intent extra key: " + key + "is Null"))
                .isNotNull();
        return this;
    }

    public IntentAssert containsFlag(int intentFlags) {
        isNotNull();
        Assertions
                .assertThat(actual.getFlags() & intentFlags)
                .overridingErrorMessage(errorMessage("Intent does not contain flags: " + intentFlags))
                .isNotEqualTo(0);
        return this;
    }

    public IntentAssert containsUri(Uri uri) {
        Preconditions.checkNotNull(uri);
        isNotNull();
        Assertions
                .assertThat(actual.getData())
                .overridingErrorMessage(errorMessage("Intent does not contain Uri: " + uri))
                .isEqualTo(uri);
        return this;
    }

    public IntentAssert opensActivity(Class expectedActivity) {
        Preconditions.checkNotNull(expectedActivity);
        isNotNull();
        Assertions
                .assertThat(actual.getComponent().getClassName())
                .overridingErrorMessage(errorMessage("Expected started activity was: " + expectedActivity.getSimpleName()))
                .isEqualTo(expectedActivity.getCanonicalName());
        return this;
    }

    public IntentAssert containsScreen(Screen screen) {
        Preconditions.checkNotNull(screen);
        isNotNull();
        Assertions
                .assertThat(Screen.fromIntent(actual))
                .overridingErrorMessage(errorMessage("Intent does not contain Screen: " + screen.name()))
                .isEqualTo(screen);
        return this;
    }

    private String errorMessage(String message) {
        return message + ". Actual subject: " + actual.getClass().getSimpleName();
    }
}
