package com.soundcloud.android.profile;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.PlaceholderGenerator;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import rx.Notification;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.graphics.Palette;
import android.widget.ImageView;

import javax.inject.Inject;

public class ProfileImageHelper {

    private static final Func1<Notification<?>, Boolean> FILTER_OUT_COMPLETED = paletteNotification -> paletteNotification.getKind() != Notification.Kind.OnCompleted;

    private final ImageOperations imageOperations;
    private final PlaceholderGenerator placeholderGenerator;
    private final BackgroundAnimator backgroundAnimator;
    private final Resources resources;
    private final CompositeSubscription subscriptions = new CompositeSubscription();

    @Inject
    public ProfileImageHelper(ImageOperations imageOperations,
                              PlaceholderGenerator placeholderGenerator,
                              Resources resources) {
        this.imageOperations = imageOperations;
        this.placeholderGenerator = placeholderGenerator;
        this.backgroundAnimator = new BackgroundAnimator(placeholderGenerator);
        this.resources = resources;
    }

    public void bindImages(UserImageSource userImageSource, ImageView bannerView, ImageView avatarView) {
        avatarView.setImageResource(android.R.color.transparent);
        bannerView.setImageResource(android.R.color.transparent);

        Observable<Notification<Bitmap>> loadBanner = bindVisualBanner(bannerView,
                userImageSource).materialize()
                .filter(FILTER_OUT_COMPLETED);
        Observable<Notification<Palette>> loadAvatar = bindAvatar(avatarView, userImageSource)
                                                        .materialize()
                                                        .filter(FILTER_OUT_COMPLETED);
        subscriptions.add(Observable.zip(loadBanner, loadAvatar, combineImageLoadingEvents(userImageSource))
                                    .subscribe(new ImageLoadingSubscriber(bannerView, backgroundAnimator)));
    }

    public void unsubscribe() {
        subscriptions.clear();
    }

    private Func2<Notification<Bitmap>, Notification<Palette>, UserImageSource> combineImageLoadingEvents(
            final UserImageSource suggestedCreatorItem) {
        return (bitmapNotification, paletteNotification) -> {
            if (paletteNotification.getKind() == Notification.Kind.OnNext) {
                suggestedCreatorItem.setPalette(Optional.of(
                        paletteNotification.getValue()));

                if (bitmapNotification.getKind() == Notification.Kind.OnError) {
                    suggestedCreatorItem.setShouldDefaultToPalette(true);
                }
            }
            return suggestedCreatorItem;
        };
    }

    private Observable<Bitmap> bindVisualBanner(final ImageView imageView,
                                                final UserImageSource suggestedCreatorItem) {
        final Urn creatorUrn = suggestedCreatorItem.getCreatorUrn();

        if (shouldDisplayGradientFromPalette(suggestedCreatorItem)) {
            return Observable.just(null);
        } else {
            final Optional<Palette> palette = suggestedCreatorItem.getPalette();
            return imageOperations.displayInAdapterView(
                    SimpleImageResource.create(
                            creatorUrn,
                            suggestedCreatorItem.getVisualUrl()
                    ),
                    ApiImageSize.getFullBannerSize(resources),
                    imageView,
                    generateFallbackDrawable(palette, creatorUrn)
            );
        }
    }

    private Optional<Drawable> generateFallbackDrawable(Optional<Palette> palette, Urn creatorUrn) {
        if (palette.isPresent()) {
            return Optional.of(
                    placeholderGenerator.generateDrawableFromPalette(
                            creatorUrn.toString(),
                            palette.get()
                    )
            );
        } else {
            return Optional.absent();
        }
    }

    private Observable<Palette> bindAvatar(final ImageView imageView,
                                           final UserImageSource suggestedCreatorItem) {
        return imageOperations.displayCircularInAdapterViewAndGeneratePalette(
                SimpleImageResource.create(suggestedCreatorItem.getCreatorUrn(),
                        suggestedCreatorItem.getAvatarUrl()),
                ApiImageSize.getFullImageSize(resources),
                imageView);
    }

    private static class BackgroundAnimator {
        private final PlaceholderGenerator placeholderGenerator;

        BackgroundAnimator(PlaceholderGenerator placeholderGenerator) {
            this.placeholderGenerator = placeholderGenerator;
        }

        void animate(Palette palette, ImageView bannerView, Urn urn) {
            final Drawable originalDrawable = bannerView.getDrawable();
            final GradientDrawable gradientDrawable = placeholderGenerator.generateDrawableFromPalette(
                    urn.toString(),
                    palette);
            if (originalDrawable != null) {
                final TransitionDrawable td = new TransitionDrawable(new Drawable[]{originalDrawable, gradientDrawable});
                bannerView.setImageDrawable(td);
                td.startTransition(500);
            } else {
                bannerView.setImageDrawable(gradientDrawable);
            }
        }
    }

    private static boolean shouldDisplayGradientFromPalette(UserImageSource suggestedCreatorItem) {
        return (!suggestedCreatorItem.getVisualUrl()
                .isPresent() || suggestedCreatorItem.shouldDefaultToPalette()) && suggestedCreatorItem.getPalette()
                .isPresent();
    }

    private static class ImageLoadingSubscriber extends DefaultSubscriber<UserImageSource> {

        private final ImageView bannerView;
        private final BackgroundAnimator backgroundAnimator;

        ImageLoadingSubscriber(ImageView bannerView,
                               BackgroundAnimator backgroundAnimator) {
            this.bannerView = bannerView;
            this.backgroundAnimator = backgroundAnimator;
        }

        @Override
        public void onNext(UserImageSource item) {
            if (shouldDisplayGradientFromPalette(item)) {
                backgroundAnimator.animate(item.getPalette().get(), bannerView, item.getCreatorUrn());
            }
        }
    }
}
