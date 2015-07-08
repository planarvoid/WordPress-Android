package com.soundcloud.android.configuration.features;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.testsupport.PlatformUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestFeatures;
import com.soundcloud.android.utils.ObfuscatedPreferences;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestObserver;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.List;

public class FeatureStorageTest extends PlatformUnitTest {

    private FeatureStorage storage;
    private List<Feature> features;

    private TestObserver<Boolean> testObserver = new TestObserver<>();

    @Before
    public void setUp() throws Exception {
        features = TestFeatures.asList();
        SharedPreferences prefs = new ObfuscatedPreferences(sharedPreferences("test", Context.MODE_PRIVATE), new Obfuscator());
        storage = new FeatureStorage(prefs);
    }

    @Test
    public void updateFeaturesStoresEnabledStates() {
        storage.update(features);

        assertThat(storage.isEnabled("feature_disabled", true)).isFalse();
        assertThat(storage.isEnabled("feature_enabled", false)).isTrue();
    }

    @Test
    public void updateFeatureStoresEnabledState() {
        storage.update(new Feature("feature_disabled", false, Arrays.asList("mid_tier")));

        assertThat(storage.isEnabled("feature_disabled", true)).isFalse();
    }

    @Test
    public void updateFeatureStoresPlans() {
        storage.update(new Feature("plan_related_feature", false, Arrays.asList("mid_tier", "high_tier")));

        assertThat(storage.getPlans("plan_related_feature")).containsOnly("mid_tier", "high_tier");
    }

    @Test
    public void returnDefaultValueWhenFeatureIsAbsent() {
        assertThat(storage.isEnabled("absent_feature", true)).isTrue();
    }

    @Test
    public void receivesUpdatesToFeatureStatusChanges() {
        storage.getUpdates("my_feature").subscribe(testObserver);

        storage.update(new Feature("my_feature", true, Arrays.asList("mid_tier")));

        assertThat(testObserver.getOnNextEvents().get(0)).isTrue();
    }

    @Test
    public void clearsSettingsStorage() {
        storage.update(features);
        storage.clear();
        assertThat(storage.isEnabled("feature_disabled", true)).isTrue();
    }

}
