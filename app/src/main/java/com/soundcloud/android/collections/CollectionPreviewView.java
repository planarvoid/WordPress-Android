package com.soundcloud.android.collections;

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
    @VisibleForTesting
    static final int EXTRA_HOLDER_VIEWS = 1;

    @Inject ImageOperations imageOperations;
    private ViewGroup holder;
    private LayoutInflater inflater;
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
        holder = (ViewGroup) findViewById(R.id.holder);
    }

    public void refreshThumbnails(List<Urn> entities, int numThumbnails) {
        final int numEmptyThumbnails = Math.max(numThumbnails - entities.size(), 0);
        this.numThumbnails = numThumbnails;

        clearThumbnails();
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

            ImageView thumbnail = (ImageView) holder.getChildAt(j + numEmptyThumbnails + EXTRA_HOLDER_VIEWS);
            imageOperations.displayWithPlaceholder(entities.get(j), ApiImageSize.getListItemImageSize(holder.getResources()), thumbnail);
        }
    }

    private void inflateThumbnailViewIntoHolder() {
        inflater.inflate(R.layout.collections_preview_item_icon_sm, holder);

        if (isLastThumbnailView()) {
            holder.getChildAt(holder.getChildCount() - 1).setBackgroundResource(R.drawable.bg_collection_empty_slot_end);
        }
    }

    private boolean isLastThumbnailView() {
        return holder.getChildCount() - EXTRA_HOLDER_VIEWS == numThumbnails;
    }

    void clearThumbnails() {
        while (EXTRA_HOLDER_VIEWS < holder.getChildCount()){
            holder.removeViewAt(EXTRA_HOLDER_VIEWS);
        }
    }
}
