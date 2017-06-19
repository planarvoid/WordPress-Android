package com.soundcloud.android.testsupport.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.main.Screen;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.propeller.utils.StringUtils;
import org.assertj.core.api.AbstractAssert;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.util.Set;

/**
 * Custom assertion class for testing Android Intents.
 */
public final class IntentAssert extends AbstractAssert<IntentAssert, Intent> {

    public IntentAssert(Intent actual) {
        super(actual, IntentAssert.class);
    }

    public IntentAssert isEqualToIntent(Intent expected) {
        isNotNull();
        assertThat(actual.filterEquals(expected)).isTrue();
        hasEqualBundle(actual.getExtras(), expected.getExtras());
        return this;
    }

    private IntentAssert hasEqualBundle(Bundle actualBundle, Bundle expectedBundle) {
        if (null == actualBundle && null == expectedBundle) {
            return this;
        }

        if (null != actualBundle) {
            if (null != expectedBundle) {

                assertThat(actualBundle.size())
                        .overridingErrorMessage("Intent bundles do not match. Expected size: " + expectedBundle.size() + " Actual size: " + actualBundle.size())
                        .isEqualTo(expectedBundle.size());

                Set<String> expectedBundleKeys = expectedBundle.keySet();
                Set<String> actualBundleKeys = actualBundle.keySet();
                assertThat(actualBundleKeys)
                        .overridingErrorMessage("Intent does not have expected bundle keys. Expected: [" + StringUtils.join(expectedBundleKeys, ", ") + "] Actual: [" + StringUtils.join(
                                actualBundleKeys, ", ") + "]")
                        .isEqualTo(expectedBundleKeys);

                for (String key : expectedBundleKeys) {
                    Object actualKeyValue = actualBundle.get(key);
                    Object expectedKeyValue = expectedBundle.get(key);

                    if (actualKeyValue instanceof Bundle && expectedKeyValue instanceof Bundle) {
                        hasEqualBundle((Bundle) actualKeyValue, (Bundle) expectedKeyValue);
                    } else {
                        assertThat(actualKeyValue)
                                .overridingErrorMessage("Intent bundle key values for key: " + key + " do not match. Expected: " + expectedKeyValue + " Actual: " + actualKeyValue)
                                .isEqualTo(expectedKeyValue);
                    }
                }
                return this;
            }
        }
        throw new AssertionError("Intent extras do not match. Expected: " + expectedBundle + " Actual: " + actualBundle);
    }

    public IntentAssert containsAction(String action) {
        Preconditions.checkNotNull(action);
        isNotNull();
        assertThat(actual.getAction())
                .overridingErrorMessage(errorMessage("Intent does not contain actions: " + action))
                .isEqualTo(action);
        return this;
    }

    public IntentAssert containsExtra(String key, Object value) {
        Preconditions.checkNotNull(key);
        isNotNull();
        assertThat(actual.hasExtra(key))
                .overridingErrorMessage(errorMessage("Intent does not contain extra for key: " + key))
                .isTrue();
        assertThat(actual.getExtras().get(key))
                .overridingErrorMessage(errorMessage("Intent does not contain extra. Key: " + key + ", value: " + value))
                .isEqualTo(value);
        return this;
    }

    public IntentAssert containsFlag(int intentFlags) {
        isNotNull();
        assertThat(actual.getFlags() & intentFlags)
                .overridingErrorMessage(errorMessage("Intent does not contain flags: " + intentFlags))
                .isNotEqualTo(0);
        return this;
    }

    public IntentAssert containsUri(Uri uri) {
        Preconditions.checkNotNull(uri);
        isNotNull();
        assertThat(actual.getData())
                .overridingErrorMessage(errorMessage("Intent does not contain Uri: " + uri))
                .isEqualTo(uri);
        return this;
    }

    public IntentAssert opensActivity(Class expectedActivity) {
        Preconditions.checkNotNull(expectedActivity);
        isNotNull();
        assertThat(actual.getComponent().getClassName())
                .overridingErrorMessage(errorMessage("Expected started activity was: " + expectedActivity.getSimpleName()))
                .isEqualTo(expectedActivity.getCanonicalName());
        return this;
    }

    public IntentAssert startsService(Class expectedService) {
        Preconditions.checkNotNull(expectedService);
        isNotNull();
        assertThat(actual.getComponent().getClassName())
                .overridingErrorMessage(errorMessage("Expected started service was: " + expectedService.getSimpleName()))
                .isEqualTo(expectedService.getCanonicalName());
        return this;
    }

    public IntentAssert containsScreen(Screen screen) {
        Preconditions.checkNotNull(screen);
        isNotNull();
        assertThat(Screen.fromIntent(actual))
                .overridingErrorMessage(errorMessage("Intent does not contain Screen: " + screen.name()))
                .isEqualTo(screen);
        return this;
    }

    public IntentAssert containsReferrer(Referrer referrer) {
        Preconditions.checkNotNull(referrer);
        isNotNull();
        assertThat(Referrer.fromIntent(actual))
                .overridingErrorMessage(errorMessage("Intent does not contain Referrer: " + referrer.name()))
                .isEqualTo(referrer);
        return this;
    }

    public IntentAssert wrappedIntent() {
        isNotNull();
        Intent wrappedIntent = actual.getParcelableExtra(Intent.EXTRA_INTENT);
        assertThat(wrappedIntent).isNotNull();
        return new IntentAssert(wrappedIntent);
    }

    private String errorMessage(String message) {
        return message + ". Actual subject: " + actual.getClass().getSimpleName();
    }
}
