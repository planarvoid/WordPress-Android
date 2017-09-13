package com.soundcloud.android.discovery.systemplaylist;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;
import rx.Subscription;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.List;

public class SystemPlaylistArtworkView extends FrameLayout {
    private LayoutInflater inflater;
    private ViewFlipper artworkAnimator;
    private Subscription subscription = RxUtils.invalidSubscription();
    int artworkLayout;

    public SystemPlaylistArtworkView(Context context) {
        super(context);
        init(context, null);
    }

    public SystemPlaylistArtworkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SystemPlaylistArtworkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void bindWithoutAnimation(ImageOperations imageOperations, SystemPlaylistItem.Header item) {
        artworkAnimator.removeAllViews();

        inflater.inflate(artworkLayout, artworkAnimator);
        final ImageView imageView = (ImageView) artworkAnimator.getChildAt(0);
        if (item.image().isPresent()) {
            ImageResource imageResource = item.image().get();
            imageOperations.displayWithPlaceholder(imageResource.getUrn(), imageResource.getImageUrlTemplate(), ApiImageSize.getFullImageSize(imageView.getResources()), imageView);
        } else {
            imageOperations.displayDefaultPlaceholder(imageView);
        }
    }

    public void bindWithAnimation(ImageOperations imageOperations, List<? extends ImageResource> imageResources) {
        List<Observable<Bitmap>> observables = new ArrayList<>(imageResources.size());

        artworkAnimator.removeAllViews();

        for (int i = 0; i < imageResources.size(); i++) {
            inflater.inflate(artworkLayout, artworkAnimator);
            final ImageView imageView = (ImageView) artworkAnimator.getChildAt(i);
            observables.add(imageOperations.displayWithPlaceholderObservable(imageResources.get(i),
                                                                             ApiImageSize.getFullImageSize(imageView.getResources()),
                                                                             imageView));
        }

        subscription = Observable.zip(observables, ignored -> ignored)
                                 .subscribe(new DefaultSubscriber<Object[]>() {
                                                @Override
                                                public void onNext(Object[] ignored) {
                                                    artworkAnimator.startFlipping();
                                                    artworkAnimator.setInAnimation(AnimationUtils.loadAnimation(artworkAnimator.getContext(), R.anim.slow_fade_in));
                                                    artworkAnimator.setOutAnimation(AnimationUtils.loadAnimation(artworkAnimator.getContext(), R.anim.slow_fade_out));
                                                }
                                            }
                                 );
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        int containerLayout;

        if (attrs != null) {
            final TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.SystemPlaylistArtworkView);
            final boolean isLarge = styledAttributes.getBoolean(R.styleable.SystemPlaylistArtworkView_large, false);
            styledAttributes.recycle();

            containerLayout = isLarge ? R.layout.system_playlist_artwork_container_large : R.layout.system_playlist_artwork_container;
            artworkLayout = isLarge ? R.layout.system_playlist_artwork_large : R.layout.system_playlist_artwork;
        } else {
            containerLayout = R.layout.system_playlist_artwork_container;
            artworkLayout = R.layout.system_playlist_artwork;
        }

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(containerLayout, this);
        artworkAnimator = findViewById(R.id.artwork_animator);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        subscription.unsubscribe();
    }
}
