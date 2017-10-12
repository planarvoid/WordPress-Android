package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.playback.ui.BlurringPlayerArtworkLoader;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

import android.content.res.Resources;
import android.widget.ImageView;

import javax.inject.Inject;
import javax.inject.Named;

class BlurringPlayQueueArtworkLoader extends BlurringPlayerArtworkLoader {

    @Inject
    public BlurringPlayQueueArtworkLoader(ImageOperations imageOperations, Resources resources,
                                          @Named(ApplicationModule.RX_LOW_PRIORITY) Scheduler graphicsScheduler) {
        super(imageOperations, resources, graphicsScheduler, AndroidSchedulers.mainThread());
    }

    public void loadArtwork(ImageResource imageResource,
                            ImageView imageOverlay) {
        loadBlurredArtwork(imageResource, imageOverlay, Optional.absent());
    }

}
