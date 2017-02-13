package com.soundcloud.android.discovery.newforyou;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;
import rx.Subscription;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.List;

public class NewForYouArtworkView extends FrameLayout {
    private LayoutInflater inflater;
    private ViewFlipper artworkAnimator;
    private Subscription subscription = RxUtils.invalidSubscription();

    public NewForYouArtworkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflateLayout(context);
    }

    public NewForYouArtworkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateLayout(context);
    }

    public NewForYouArtworkView(Context context) {
        super(context);
        inflateLayout(context);
    }

    private void inflateLayout(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.new_for_you_artwork_container, this);
        artworkAnimator = (ViewFlipper) findViewById(R.id.artwork_animator);
    }

    void bind(ImageOperations imageOperations, List<? extends ImageResource> imageResources) {
        List<Observable<Bitmap>> observables = new ArrayList<>(imageResources.size());

        artworkAnimator.removeAllViews();

        for (int i = 0; i < imageResources.size(); i++) {
            inflater.inflate(R.layout.new_for_you_artwork, artworkAnimator);
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        subscription.unsubscribe();
    }
}
