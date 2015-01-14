package com.soundcloud.android.configuration.features;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ScTestSharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class FeatureStorageTest {

    private FeatureStorage storage;
    private List<Feature> features;

    @Before
    public void setUp() throws Exception {
        features = Arrays.asList(new Feature("feature_disabled", false), new Feature("feature_enabled", true));
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
        expect(storage.listFeatures()).toBeEmpty();
    }

    @Test
    public void listFeaturesShouldReturnAllFeatures() {
        storage.updateFeature(features);

        final List<Feature> features = storage.listFeatures();

        expect(features).toContain(features.get(0), features.get(1));
    }

    @Test
    public void updateFeatureEnabledValues() {
        final Feature feature = new Feature("feature_disabled", false);
        storage.updateFeature(feature);

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