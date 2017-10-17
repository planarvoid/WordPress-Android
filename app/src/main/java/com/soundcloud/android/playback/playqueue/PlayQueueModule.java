package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.ui.progress.ProgressController;
import dagger.Module;
import dagger.Provides;

import android.os.Build;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module
public abstract class PlayQueueModule {

    @Provides
    static ArtworkView providesArtworkView(ArtworkPresenter artworkPresenter, ProgressController.Factory factory, BlurringPlayQueueArtworkLoader playerArtworkLoader) {
        if (Build.VERSION_CODES.JELLY_BEAN_MR1 < Build.VERSION.SDK_INT) {
            return new KitKatArtworkView(artworkPresenter, factory, playerArtworkLoader);
        } else {
            return new NoOpArtworkView();
        }
    }
}
