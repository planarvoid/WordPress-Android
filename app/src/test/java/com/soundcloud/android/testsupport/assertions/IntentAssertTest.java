package com.soundcloud.android.testsupport.assertions;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.propeller.utils.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class IntentAssertTest extends AndroidUnitTest {

    private Context context;
    private Intent actualIntent;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        context = context();

        actualIntent = new Intent(context, Integer.class);
        actualIntent.putExtra("key1", "value1");
        actualIntent.putExtra("key2", "value2");
    }

    @Test
    public void isEqualToIntentIsTrue() throws Exception {
        Intent expectedIntent = new Intent(context, Integer.class);
        expectedIntent.putExtra("key1", "value1");
        expectedIntent.putExtra("key2", "value2");

        Assertions.assertThat(actualIntent)
                  .isEqualToIntent(expectedIntent);
    }

    @Test
    public void isEqualToIntentIsTrueWhenBothBundlesAreNull() throws Exception {
        Intent actualIntent = new Intent(context, Integer.class);
        Intent expectedIntent = new Intent(context, Integer.class);

        Assertions.assertThat(actualIntent)
                  .isEqualToIntent(expectedIntent);
    }

    @Test
    public void isEqualToIntentIsTrueWhenBundleContainsBundle() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("bundleKey", "bundleValue");
        actualIntent.putExtra("bundle", bundle);

        Intent expectedIntent = new Intent(context, Integer.class);
        expectedIntent.putExtra("key1", "value1");
        expectedIntent.putExtra("key2", "value2");
        expectedIntent.putExtra("bundle", bundle);

        Assertions.assertThat(actualIntent)
                  .isEqualToIntent(expectedIntent);
    }

    @Test
    public void isEqualToIntentIsFalseWhenOneBundleIsNull() throws Exception {
        Intent expectedIntent = new Intent(context, Integer.class);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Intent extras do not match. Expected: " + expectedIntent.getExtras() + " Actual: " + actualIntent.getExtras());

        Assertions.assertThat(actualIntent)
                  .isEqualToIntent(expectedIntent);
    }

    @Test
    public void isEqualToIntentIsFalseWhenOneBundleIsBigger() throws Exception {
        Intent expectedIntent = new Intent(context, Integer.class);
        expectedIntent.putExtra("key1", "value1");
        expectedIntent.putExtra("key2", "value2");
        expectedIntent.putExtra("key3", "value3");

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Intent bundles do not match. Expected size: " + expectedIntent.getExtras().size() + " Actual size: " + actualIntent.getExtras().size());

        Assertions.assertThat(actualIntent)
                  .isEqualToIntent(expectedIntent);
    }

    @Test
    public void isEqualToIntentIsFalseWhenOneBundleHasDifferentKeys() throws Exception {
        Intent expectedIntent = new Intent(context, Integer.class);
        expectedIntent.putExtra("key3", "value1");
        expectedIntent.putExtra("key4", "value2");

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Intent does not have expected bundle keys. Expected: [" + StringUtils.join(expectedIntent.getExtras().keySet(), ", ") + "] Actual: [" + StringUtils.join(
                actualIntent.getExtras().keySet(), ", ") + "]");

        Assertions.assertThat(actualIntent)
                  .isEqualToIntent(expectedIntent);
    }

    @Test
    public void isEqualToIntentIsFalseWhenOneBundleKeyHasDifferentValue() throws Exception {
        Intent expectedIntent = new Intent(context, Integer.class);
        expectedIntent.putExtra("key1", "value1");
        expectedIntent.putExtra("key2", "value3");

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Intent bundle key values for key: key2 do not match. Expected: value3 Actual: value2");

        Assertions.assertThat(actualIntent)
                  .isEqualToIntent(expectedIntent);
    }

    @Test
    public void isEqualToIntentIsTrueWhenBundleContainsDifferentBundle() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("bundleKey", "bundleValue");
        actualIntent.putExtra("bundle", bundle);

        Bundle bundle2 = new Bundle();
        bundle2.putString("bundleKey", "bundleValue2");

        Intent expectedIntent = new Intent(context, Integer.class);
        expectedIntent.putExtra("key1", "value1");
        expectedIntent.putExtra("key2", "value2");
        expectedIntent.putExtra("bundle", bundle2);

        thrown.expect(AssertionError.class);
        thrown.expectMessage("Intent bundle key values for key: bundleKey do not match. Expected: bundleValue2 Actual: bundleValue");

        Assertions.assertThat(actualIntent)
                  .isEqualToIntent(expectedIntent);
    }

}
