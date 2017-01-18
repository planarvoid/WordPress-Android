package com.soundcloud.android.cast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class CastConfigStorageTest extends AndroidUnitTest {

    @Mock private ApplicationProperties appProperties;
    @Mock private FeatureFlags featureFlags;

    private CastConfigStorage castConfigStorage;

    @Before
    public void setUp() throws Exception {
        castConfigStorage = new CastConfigStorage(context(), sharedPreferences(), appProperties, featureFlags);
    }

    @Test
    public void returnsDefaultReceiverIDForReleaseBuild() {
        when(appProperties.isReleaseBuild()).thenReturn(true);

        assertThat(castConfigStorage.getReceiverID()).isEqualTo(getDefaultReceiverID());
    }

    @Test
    public void returnsDefaultReceiverIDNonReleaseBuildWhenNoOverride() {
        when(appProperties.isReleaseBuild()).thenReturn(false);

        assertThat(castConfigStorage.getReceiverID()).isEqualTo(getDefaultReceiverID());
    }

    @Test
    public void savesReceiverIDOverride() {
        final String newID = "NEW_ID";
        when(appProperties.isReleaseBuild()).thenReturn(false);

        castConfigStorage.saveReceiverIDOverride(newID);

        assertThat(castConfigStorage.getReceiverID()).isEqualTo(newID);
    }

    @Test
    public void resetRemovesPreviousReceiverIdOverride() {
        final String newID = "NEW_ID";
        when(appProperties.isReleaseBuild()).thenReturn(false);

        castConfigStorage.saveReceiverIDOverride(newID);
        assertThat(castConfigStorage.getReceiverID()).isEqualTo(newID);

        castConfigStorage.reset();
        assertThat(castConfigStorage.getReceiverID()).isEqualTo(getDefaultReceiverID());

    }

    private String getDefaultReceiverID() {
        return resources().getString(R.string.cast_receiver_app_id);
    }

}
