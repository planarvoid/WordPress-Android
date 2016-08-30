package com.soundcloud.android.image;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import android.content.res.Resources;
import android.graphics.Bitmap;
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
                                    @Named(ApplicationModule.LOW_PRIORITY) Scheduler scheduler) {
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
                       .subscribe(new DefaultSubscriber<Bitmap>() {
                           @Override
                           public void onNext(Bitmap args) {
                               blurredArtworkView.setImageBitmap(args);
                           }
                       });
    }


}
