package com.soundcloud.android.collection;

import static com.soundcloud.android.image.ApiImageSize.getListItemImageSize;
import static com.soundcloud.java.optional.Optional.fromNullable;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class CollectionPreviewView extends FrameLayout {

    private Optional<Drawable> previewIconOverlay;

    private LayoutInflater inflater;
    private ViewGroup thumbnailContainer;
    private int numThumbnails;
    private TextView title;
    private boolean titleEnabled;


    public CollectionPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.CollectionPreviewView);
        titleEnabled = styledAttributes.getBoolean(R.styleable.CollectionPreviewView_collectionTitleEnabled, true);
        if (titleEnabled) {
            init(context, R.layout.collection_preview);
            title = (TextView) findViewById(R.id.title);
            setTitle(styledAttributes.getString(R.styleable.CollectionPreviewView_collectionTitle));
            title.setCompoundDrawablesWithIntrinsicBounds(
                    styledAttributes.getDrawable(R.styleable.CollectionPreviewView_collectionIcon), null, null, null);
        } else {
            init(context, R.layout.preview_thumbnails);
        }
        previewIconOverlay = fromNullable(styledAttributes.getDrawable(R.styleable.CollectionPreviewView_collectionPreviewOverlay));
        styledAttributes.recycle();
    }

    public void setTitle(String text) {
        if (titleEnabled) {
            title.setText(text);
        }
    }

    @VisibleForTesting
    public CollectionPreviewView(Context context,
                                 @Nullable Drawable previewIconOverlay) {
        super(context);
        this.previewIconOverlay = fromNullable(previewIconOverlay);
        init(context, R.layout.collection_preview);
    }

    private void init(Context context, int layout) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(layout, this);
        thumbnailContainer = (ViewGroup) findViewById(R.id.thumbnail_container);
    }

    public void refreshThumbnails(ImageOperations imageOperations, List<? extends ImageResource> imageResources, int numThumbnails) {
        final int numEmptyThumbnails = Math.max(numThumbnails - imageResources.size(), 0);
        this.numThumbnails = numThumbnails;

        thumbnailContainer.removeAllViews();
        populateEmptyThumbnails(numEmptyThumbnails);
        populateArtwork(imageOperations, imageResources, numEmptyThumbnails);
    }

    private void populateEmptyThumbnails(int numEmptyThumbnails) {
        for (int i = 0; i < numEmptyThumbnails; i++) {
            inflateThumbnailViewIntoHolder();
        }
    }

    private void populateArtwork(ImageOperations imageOperations, List<? extends ImageResource> imageResources, int numEmptyThumbnails) {
        final int numImages = numThumbnails - numEmptyThumbnails;
        for (int j = 0; j < numImages; j++) {
            inflateThumbnailViewIntoHolder();
            setPreviewArtwork(imageOperations, imageResources, numEmptyThumbnails, j);
        }
    }

    private void setPreviewArtwork(ImageOperations imageOperations, List<? extends ImageResource> imageResources, int numEmptyThumbnails, int index) {
        final View thumbnail = thumbnailContainer.getChildAt(index + numEmptyThumbnails);
        final ImageView artwork = (ImageView) thumbnail.findViewById(R.id.preview_artwork);

        imageOperations.displayWithPlaceholder(imageResources.get(index),
                                               getListItemImageSize(thumbnailContainer.getResources()),
                                               artwork);

        if (previewIconOverlay.isPresent()) {
            final ImageView overlay = (ImageView) thumbnail.findViewById(R.id.artwork_overlay);
            overlay.setImageDrawable(previewIconOverlay.get());
            overlay.setVisibility(View.VISIBLE);
        }
    }

    private void inflateThumbnailViewIntoHolder() {
        inflater.inflate(R.layout.collections_preview_item_icon_sm, thumbnailContainer);
        if (isLastThumbnail()) {
            thumbnailContainer.getChildAt(thumbnailContainer.getChildCount() - 1)
                              .setBackgroundResource(R.drawable.bg_collection_empty_slot_end);
        }
    }

    private boolean isLastThumbnail() {
        return thumbnailContainer.getChildCount() == numThumbnails;
    }

}
