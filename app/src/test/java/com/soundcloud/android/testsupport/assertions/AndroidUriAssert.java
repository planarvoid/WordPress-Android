package com.soundcloud.android.testsupport.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.android.api.Assertions;
import org.assertj.core.api.AbstractAssert;

import android.net.Uri;

public class AndroidUriAssert extends AbstractAssert<AndroidUriAssert, Uri> {
    public AndroidUriAssert(Uri actual) {
        super(actual, AndroidUriAssert.class);
    }

    public AndroidUriAssert hasHost(String host) {
        Assertions.assertThat(actual)
                  .hasHost(host);
        return this;
    }

    public AndroidUriAssert hasQueryParamWithValue(String key, String value) {
        isNotNull();

        String actualQueryValue = actual.getQueryParameter(key);

        assertThat(actualQueryValue)
                .overridingErrorMessage("Uri <%s> doesn't contain the <%s> query param", actual.toString(), key)
                .isNotNull();

        assertThat(actualQueryValue)
                .overridingErrorMessage("Expected query parameter to be <%s> but it is <%s>", value, actualQueryValue)
                .isEqualToIgnoringCase(value);

        return this;
    }

    public AndroidUriAssert hasScheme(String scheme) {
        Assertions.assertThat(actual)
                  .hasScheme(scheme);
        return this;
    }

    public AndroidUriAssert hasFragment(String fragment) {
        Assertions.assertThat(actual)
                  .hasFragment(fragment);
        return this;
    }

    public AndroidUriAssert hasPath(String path) {
        Assertions.assertThat(actual)
                  .hasPath(path);
        return this;
    }
}
