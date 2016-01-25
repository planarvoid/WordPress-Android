package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class CollectionPreviewView extends FrameLayout {

    @Inject ImageOperations imageOperations;

    private LayoutInflater inflater;
    private ViewGroup thumbnailContainer;
    private int numThumbnails;

    public CollectionPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SoundCloudApplication.getObjectGraph().inject(this);
        init(context);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CollectionPreviewView);
        final TextView title = (TextView) findViewById(R.id.title);
        title.setText(a.getString(R.styleable.CollectionPreviewView_collectionTitle));
        title.setCompoundDrawablesWithIntrinsicBounds(a.getDrawable(R.styleable.CollectionPreviewView_collectionIcon), null, null, null);
        a.recycle();
    }

    @VisibleForTesting
    public CollectionPreviewView(Context context, ImageOperations imageOperations) {
        super(context);
        this.imageOperations = imageOperations;
        init(context);
    }

    private void init(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.collection_preview, this);
        thumbnailContainer = (ViewGroup) findViewById(R.id.thumbnail_container);
    }

    public void refreshThumbnails(List<Urn> entities, int numThumbnails) {
        final int numEmptyThumbnails = Math.max(numThumbnails - entities.size(), 0);
        this.numThumbnails = numThumbnails;

        thumbnailContainer.removeAllViews();
        populateEmptyThumbnails(numEmptyThumbnails);
        populateArtwork(entities, numEmptyThumbnails);
    }

    void populateEmptyThumbnails(int numEmptyThumbnails) {
        for (int i = 0; i < numEmptyThumbnails; i++) {
            inflateThumbnailViewIntoHolder();
        }
    }

    void populateArtwork(List<Urn> entities, int numEmptyThumbnails) {
        final int numImages = numThumbnails - numEmptyThumbnails;

        for (int j = 0; j < numImages; j++) {
            inflateThumbnailViewIntoHolder();

            ImageView thumbnail = (ImageView) thumbnailContainer.getChildAt(j + numEmptyThumbnails);
            imageOperations.displayWithPlaceholder(entities.get(j), ApiImageSize.getListItemImageSize(thumbnailContainer.getResources()), thumbnail);
        }
    }

    private void inflateThumbnailViewIntoHolder() {
        inflater.inflate(R.layout.collections_preview_item_icon_sm, thumbnailContainer);
        if (isLastThumbnail()) {
            thumbnailContainer.getChildAt(thumbnailContainer.getChildCount() - 1).setBackgroundResource(R.drawable.bg_collection_empty_slot_end);
        }
    }

    private boolean isLastThumbnail() {
        return thumbnailContainer.getChildCount() == numThumbnails;
    }

}
