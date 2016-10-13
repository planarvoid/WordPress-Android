package com.soundcloud.android.suggestedcreators;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.PlaceholderGenerator;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.users.User;
import com.soundcloud.java.collections.PropertySet;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.List;

public class SuggestedCreatorRenderer implements CellRenderer<SuggestedCreatorItem> {

    private static final Func1<Notification<?>, Boolean> FILTER_OUT_COMPLETED = new Func1<Notification<? extends Object>, Boolean>() {
        @Override
        public Boolean call(Notification<? extends Object> paletteNotification) {
            return paletteNotification.getKind() != Notification.Kind.OnCompleted;
        }
    };
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final FollowingOperations followingOperations;
    private final Navigator navigator;
    private final PlaceholderGenerator placeholderGenerator;
    private final BackgroundAnimator backgroundAnimator;
    private final CompositeSubscription subscriptions = new CompositeSubscription();

    @Inject
    SuggestedCreatorRenderer(ImageOperations imageOperations,
                             Resources resources,
                             FollowingOperations followingOperations,
                             Navigator navigator,
                             PlaceholderGenerator placeholderGenerator) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.followingOperations = followingOperations;
        this.navigator = navigator;
        this.placeholderGenerator = placeholderGenerator;
        this.backgroundAnimator = new BackgroundAnimator(placeholderGenerator);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        View itemView = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.suggested_creators_item, parent, false);
        // unfortunately this doesn't work when you do it in the XML
        itemView.findViewById(R.id.suggested_creators_item)
                .setBackgroundResource(R.drawable.card_border);
        return itemView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<SuggestedCreatorItem> items) {
        final SuggestedCreatorItem suggestedCreatorItem = items.get(position);
        bindArtistName(itemView, suggestedCreatorItem.creator());
        bindArtistLocation(itemView, suggestedCreatorItem.creator());
        bindImages(itemView, suggestedCreatorItem);
        bindReason(itemView, suggestedCreatorItem.relation());
        bindFollowButton(itemView, suggestedCreatorItem);
    }

    private void bindImages(View itemView, final SuggestedCreatorItem suggestedCreatorItem) {
        final ImageView bannerView = (ImageView) itemView.findViewById(R.id.suggested_creator_visual_banner);
        final ImageView avatarView = (ImageView) itemView.findViewById(R.id.suggested_creator_avatar);

        Observable<Notification<Bitmap>> loadBanner = bindVisualBanner(bannerView,
                                                                       suggestedCreatorItem).materialize()
                                                                                            .filter(FILTER_OUT_COMPLETED);
        Observable<Notification<Palette>> loadAvatar = bindAvatar(avatarView,
                                                                  suggestedCreatorItem).materialize()
                                                                                       .filter(FILTER_OUT_COMPLETED);
        subscriptions.add(Observable.zip(loadBanner, loadAvatar,
                                         combineImageLoadingEvents(suggestedCreatorItem))
                                    .subscribe(new ImageLoadingSubscriber(bannerView, backgroundAnimator)));
    }

    private Func2<Notification<Bitmap>, Notification<Palette>, SuggestedCreatorItem> combineImageLoadingEvents(
            final SuggestedCreatorItem suggestedCreatorItem) {
        return new Func2<Notification<Bitmap>, Notification<Palette>, SuggestedCreatorItem>() {
            @Override
            public SuggestedCreatorItem call(Notification<Bitmap> bitmapNotification,
                                             Notification<Palette> paletteNotification) {
                if (paletteNotification.getKind() == Notification.Kind.OnNext) {
                    suggestedCreatorItem.palette = Optional.of(
                            paletteNotification.getValue());

                    if (bitmapNotification.getKind() == Notification.Kind.OnError) {
                        suggestedCreatorItem.shouldDefaultToPalette = true;
                    }
                }
                return suggestedCreatorItem;
            }
        };
    }

    private void bindFollowButton(View view, final SuggestedCreatorItem suggestedCreatorItem) {
        final ToggleButton toggleButton = (ToggleButton) view.findViewById(R.id.toggle_btn_follow);
        toggleButton.setChecked(suggestedCreatorItem.following);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                suggestedCreatorItem.following = isChecked;
                followingOperations.toggleFollowing(suggestedCreatorItem.creator().urn(), isChecked)
                                   .subscribe(new DefaultSubscriber<PropertySet>());
            }
        });
    }

    private void bindArtistName(View view, final User creator) {
        final TextView textView = (TextView) view.findViewById(R.id.suggested_creator_artist);
        textView.setText(creator.username());
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO Fix event for tracking
                navigator.openProfile(v.getContext(),
                                      creator.urn(),
                                      new UIEvent(UIEvent.KIND_NAVIGATION));
            }
        });
    }

    private void bindArtistLocation(View view, User creator) {
        final TextView textView = (TextView) view.findViewById(R.id.suggested_creator_location);
        if (!creator.city().isPresent() && !creator.country().isPresent()) {
            textView.setVisibility(View.GONE);
        } else if (creator.city().isPresent() && creator.country().isPresent()) {
            textView.setText(String.format("%s, %s",
                                           creator.city().get(),
                                           creator.country().get()));
        } else {
            textView.setText(creator.city().or(creator.country()).get());
        }
    }

    private void bindReason(View view, SuggestedCreatorRelation relation) {
        final String key = "suggested_creators_relation_" + relation.value();
        final int resourceId = resources.getIdentifier(key,
                                                       "string",
                                                       view.getContext().getPackageName());
        final String text = (resourceId != 0) ? resources.getString(resourceId) : "";
        ((TextView) view.findViewById(R.id.suggested_creator_relation)).setText(text);
    }

    private Observable<Bitmap> bindVisualBanner(final ImageView imageView,
                                                final SuggestedCreatorItem suggestedCreatorItem) {
        final User creator = suggestedCreatorItem.creator();
        final Optional<Palette> palette = suggestedCreatorItem.palette;
        if (shouldDisplayGradientFromPalette(suggestedCreatorItem)) {
            return Observable.just(null);
        } else {
            return imageOperations.displayInAdapterView(SimpleImageResource.create(creator.urn(),
                                                                                   creator.visualUrl()),
                                                        ApiImageSize.getFullBannerSize(resources),
                                                        imageView,
                                                        palette.isPresent() ?
                                                        Optional.<Drawable>of(placeholderGenerator.generateDrawableFromPalette(
                                                                creator.urn().toString(),
                                                                palette.get())) :
                                                        Optional.<Drawable>absent());
        }
    }

    private Observable<Palette> bindAvatar(final ImageView imageView,
                                           final SuggestedCreatorItem suggestedCreatorItem) {
        return imageOperations.displayCircularInAdapterViewAndGeneratePalette(
                SimpleImageResource.create(suggestedCreatorItem.creator().urn(),
                                           suggestedCreatorItem.creator().avatarUrl()),
                ApiImageSize.getFullImageSize(resources),
                imageView);
    }

    private static boolean shouldDisplayGradientFromPalette(SuggestedCreatorItem suggestedCreatorItem) {
        return (!suggestedCreatorItem.creator()
                                     .visualUrl()
                                     .isPresent() || suggestedCreatorItem.shouldDefaultToPalette) && suggestedCreatorItem.palette
                .isPresent();
    }

    void unsubscribe() {
        subscriptions.clear();
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

    private static class ImageLoadingSubscriber extends DefaultSubscriber<SuggestedCreatorItem> {

        private final ImageView bannerView;
        private final BackgroundAnimator backgroundAnimator;

        public ImageLoadingSubscriber(ImageView bannerView,
                                      BackgroundAnimator backgroundAnimator) {
            this.bannerView = bannerView;
            this.backgroundAnimator = backgroundAnimator;
        }

        @Override
        public void onNext(SuggestedCreatorItem item) {
            if (shouldDisplayGradientFromPalette(item)) {
                backgroundAnimator.animate(item.palette
                                                   .get(),
                                           bannerView,
                                           item.creator()
                                               .urn());
            }

        }
    }
}
