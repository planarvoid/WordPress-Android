package com.soundcloud.android.configuration.features;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ScTestSharedPreferences;
import com.soundcloud.android.testsupport.fixtures.TestFeatures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class FeatureStorageTest {

    private FeatureStorage storage;
    private Map<String, Boolean> features;

    @Before
    public void setUp() throws Exception {
        features = TestFeatures.asMap();
        storage = new FeatureStorage(new ScTestSharedPreferences());
    }

    @Test
    public void updateFeaturesEnabledValues() {
        storage.updateFeature(features);

        expect(storage.isEnabled("feature_disabled", true)).toBeFalse();
        expect(storage.isEnabled("feature_enabled", false)).toBeTrue();
    }

    @Test
    public void listFeaturesShouldReturnEmptyListWhenNoFeature() {
        expect(storage.listFeatures().isEmpty()).toBeTrue();
    }

    @Test
    public void listFeaturesShouldReturnAllFeatures() {
        storage.updateFeature(features);

        expect(storage.listFeatures()).toEqual(features);
    }

    @Test
    public void updateFeatureEnabledValues() {
        final Feature feature = new Feature("feature_disabled", false);
        storage.updateFeature(feature.name, feature.enabled);

        expect(storage.isEnabled("feature_disabled", true)).toBeFalse();
    }

    @Test
    public void returnDefaultValueWhenFeatureIsAbsent() {
        expect(storage.isEnabled("absent_feature", true)).toBeTrue();
    }

    @Test
    public void clearsSettingsStorage() {
        storage.updateFeature(features);
        storage.clear();
        expect(storage.isEnabled("feature_disabled", true)).toBeTrue();
    }

}