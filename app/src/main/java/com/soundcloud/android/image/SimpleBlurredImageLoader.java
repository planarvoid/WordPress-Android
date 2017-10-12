package com.soundcloud.android.image;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.rx.observers.LambdaSingleObserver;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

import android.content.res.Resources;
import android.widget.ImageView;

import javax.inject.Inject;
import javax.inject.Named;

public class SimpleBlurredImageLoader {

    private final static float BLUR_RADIUS = 22.f;

    private ImageOperations imageOperations;
    private Resources resources;
    private final Scheduler scheduler;

    @Inject
    public SimpleBlurredImageLoader(ImageOperations imageOperations, Resources resources,
                                    @Named(ApplicationModule.RX_LOW_PRIORITY) Scheduler scheduler) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.scheduler = scheduler;
    }

    public void displayBlurredArtwork(ImageResource info, final ImageView blurredArtworkView) {
        imageOperations.blurredArtwork(resources,
                                       info,
                                       Optional.of(BLUR_RADIUS),
                                       scheduler,
                                       AndroidSchedulers.mainThread())
                       .subscribe(LambdaSingleObserver.onNext(blurredArtworkView::setImageBitmap));
    }
}
