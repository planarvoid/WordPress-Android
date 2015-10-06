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
    static final int MAX_IMAGES = 3;

    @Inject ImageOperations imageOperations;
    private ViewGroup holder;
    private LayoutInflater inflater;

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

    void populateArtwork(List<Urn> entities) {
        final int imagesToDisplay = Math.min(MAX_IMAGES, entities.size());

        for (int i = 0; i < imagesToDisplay; i++) {
            if (needsViewForIndex(i)) {
                inflateImageViewIntoHolder();
            }
            ImageView icon = (ImageView) holder.getChildAt(i + EXTRA_HOLDER_VIEWS);
            imageOperations.displayWithPlaceholder(entities.get(i), ApiImageSize.getListItemImageSize(holder.getResources()), icon);
        }

        removeExtraImageViews(imagesToDisplay);
    }

    private boolean needsViewForIndex(int i) {
        return holder.getChildCount() == i + EXTRA_HOLDER_VIEWS;
    }

    private void inflateImageViewIntoHolder() {
        inflater.inflate(R.layout.collections_preview_item_icon_sm, holder);
    }

    private void removeExtraImageViews(int displayedImages) {
        int extraPosition = displayedImages + EXTRA_HOLDER_VIEWS;
        while (extraPosition < holder.getChildCount()){
            holder.removeViewAt(extraPosition);
        }
    }
}
