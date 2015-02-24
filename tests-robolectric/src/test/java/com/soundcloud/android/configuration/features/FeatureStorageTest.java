package com.soundcloud.android.configuration.features;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ScTestSharedPreferences;
import com.soundcloud.android.testsupport.fixtures.TestFeatures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class FeatureStorageTest {

    private FeatureStorage storage;
    private Map<String, Boolean> features;

    private TestObserver<Boolean> testObserver = new TestObserver<>();

    @Before
    public void setUp() throws Exception {
        features = TestFeatures.asMap();
        storage = new FeatureStorage(new ScTestSharedPreferences());
    }

    @Test
    public void updateFeaturesEnabledValues() {
        storage.update(features);

        expect(storage.isEnabled("feature_disabled", true)).toBeFalse();
        expect(storage.isEnabled("feature_enabled", false)).toBeTrue();
    }

    @Test
    public void listFeaturesShouldReturnEmptyListWhenNoFeature() {
        expect(storage.list().isEmpty()).toBeTrue();
    }

    @Test
    public void listFeaturesShouldReturnAllFeatures() {
        storage.update(features);

        expect(storage.list()).toEqual(features);
    }

    @Test
    public void updateFeatureEnabledValues() {
        final Feature feature = new Feature("feature_disabled", false);
        storage.update(feature.name, feature.enabled);

        expect(storage.isEnabled("feature_disabled", true)).toBeFalse();
    }

    @Test
    public void returnDefaultValueWhenFeatureIsAbsent() {
        expect(storage.isEnabled("absent_feature", true)).toBeTrue();
    }

    @Test
    public void receivesUpdatesToFeatureStatusChanges() {
        storage.getUpdates("my_feature").subscribe(testObserver);
        storage.update("my_feature", true);
        expect(testObserver.getOnNextEvents().get(0)).toBeTrue();
    }

    @Test
    public void clearsSettingsStorage() {
        storage.update(features);
        storage.clear();
        expect(storage.isEnabled("feature_disabled", true)).toBeTrue();
    }

}