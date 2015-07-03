package com.soundcloud.android.configuration.features;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ScTestSharedPreferences;
import com.soundcloud.android.testsupport.fixtures.TestFeatures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class FeatureStorageTest {

    public static final String MOCK_ENCRYPTION = "-obfuscated";

    private FeatureStorage storage;
    private List<Feature> features;

    @Mock private Obfuscator obfuscator;

    private TestObserver<Boolean> testObserver = new TestObserver<>();

    @Before
    public void setUp() throws Exception {
        features = TestFeatures.asList();
        storage = new FeatureStorage(new ScTestSharedPreferences(), obfuscator);
        configureMockObfuscation();
    }

    @Test
    public void updateFeaturesStoresEnabledStates() {
        storage.update(features);

        expect(storage.isEnabled("feature_disabled", true)).toBeFalse();
        expect(storage.isEnabled("feature_enabled", false)).toBeTrue();
    }

    @Test
    public void updateFeatureStoresEnabledState() {
        storage.update(new Feature("feature_disabled", false, Arrays.asList("mid_tier")));

        expect(storage.isEnabled("feature_disabled", true)).toBeFalse();
    }

    @Test
    public void updateFeatureStoresPlans() {
        storage.update(new Feature("plan_related_feature", false, Arrays.asList("mid_tier", "high_tier")));

        expect(storage.getPlans("plan_related_feature")).toContainExactlyInAnyOrder("mid_tier", "high_tier");
    }

    @Test
    public void returnDefaultValueWhenFeatureIsAbsent() {
        expect(storage.isEnabled("absent_feature", true)).toBeTrue();
    }

    @Test
    public void receivesUpdatesToFeatureStatusChanges() {
        storage.getUpdates("my_feature").subscribe(testObserver);

        storage.update(new Feature("my_feature", true, Arrays.asList("mid_tier")));

        expect(testObserver.getOnNextEvents().get(0)).toBeTrue();
    }

    @Test
    public void clearsSettingsStorage() {
        storage.update(features);
        storage.clear();
        expect(storage.isEnabled("feature_disabled", true)).toBeTrue();
    }

    private void configureMockObfuscation() throws Exception {
        when(obfuscator.obfuscate(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0] + MOCK_ENCRYPTION;
            }
        });
        when(obfuscator.deobfuscateString(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String encrypted = invocation.getArguments()[0].toString();
                return encrypted.substring(0, encrypted.length() - MOCK_ENCRYPTION.length());
            }
        });
        when(obfuscator.obfuscate(anyBoolean())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0].toString() + MOCK_ENCRYPTION;
            }
        });
        when(obfuscator.deobfuscateBoolean(anyString())).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                String obfuscated = invocation.getArguments()[0].toString();
                return Boolean.parseBoolean(obfuscated.substring(0, obfuscated.length() - MOCK_ENCRYPTION.length()));
            }
        });
    }

}
