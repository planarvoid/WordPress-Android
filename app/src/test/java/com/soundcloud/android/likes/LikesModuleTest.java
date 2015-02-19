package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.actionbar.menu.ActionMenuController;
import com.soundcloud.android.actionbar.menu.DefaultActionMenuController;
import com.soundcloud.android.actionbar.menu.SyncActionMenuController;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import dagger.Lazy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class LikesModuleTest {

    private LikesModule module;

    @Mock private SyncActionMenuController syncActionMenuController;
    @Mock private DefaultActionMenuController defaultActionMenuController;
    @Mock private FeatureOperations featureOperations;
    private Lazy<SyncActionMenuController> syncActionMenuControllerProvider;
    private Lazy<DefaultActionMenuController> defaultActionMenuControllerProvider;

    @Before
    public void setUp() throws Exception {
        module = new LikesModule();

        syncActionMenuControllerProvider = new Lazy<SyncActionMenuController>() {
            @Override
            public SyncActionMenuController get() {
                return syncActionMenuController;
            }
        };
        defaultActionMenuControllerProvider = new Lazy<DefaultActionMenuController>() {
            @Override
            public DefaultActionMenuController get() {
                return defaultActionMenuController;
            }
        };
    }

    @Test
    public void providesSyncActionMenuControllerWhenOfflineSyncEnabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        ActionMenuController actionMenuController = module.provideTrackLikesActionMenuController(syncActionMenuControllerProvider,
                defaultActionMenuControllerProvider, featureOperations);

        expect(actionMenuController).toBe(syncActionMenuController);
    }

    @Test
    public void providesSyncActionMenuControllerWhenOfflineSyncUpsell() {
        when(featureOperations.isOfflineContentUpsellEnabled()).thenReturn(true);

        ActionMenuController actionMenuController = module.provideTrackLikesActionMenuController(syncActionMenuControllerProvider,
                defaultActionMenuControllerProvider, featureOperations);

        expect(actionMenuController).toBe(syncActionMenuController);
    }

    @Test
    public void providesDefaultActionMenuControllerWhenOfflineSyncNotAvailable() {
        ActionMenuController actionMenuController = module.provideTrackLikesActionMenuController(syncActionMenuControllerProvider,
                defaultActionMenuControllerProvider, featureOperations);

        expect(actionMenuController).toBe(defaultActionMenuController);
    }
}