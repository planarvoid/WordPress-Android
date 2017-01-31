package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.playback.ui.progress.ProgressController;
import dagger.Module;
import dagger.Provides;

import android.content.Context;
import android.os.Build;

@Module
public class PlayQueueModule {

    @Provides
    public ArtworkView providesArtworkView(ArtworkPresenter artworkPresenter, ProgressController.Factory factory, BlurringPlayQueueArtworkLoader playerArtworkLoader) {
        if (Build.VERSION_CODES.JELLY_BEAN_MR1 < Build.VERSION.SDK_INT) {
            return new KitKatArtworkView(artworkPresenter, factory, playerArtworkLoader);
        } else {
            return new NoOpArtworkView();
        }
    }

    @Provides
    public TopPaddingDecorator providesTopPaddingDecorator(Context context) {
        return new TopPaddingDecorator(context);
    }

}
