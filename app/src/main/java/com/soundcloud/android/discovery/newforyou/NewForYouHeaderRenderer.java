package com.soundcloud.android.discovery.newforyou;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.discovery.newforyou.NewForYouItem.NewForYouHeaderItem;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.SimpleBlurredImageLoader;
import com.soundcloud.android.presentation.CellRenderer;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class NewForYouHeaderRenderer implements CellRenderer<NewForYouHeaderItem> {
    private final Listener listener;
    private final ImageOperations imageOperations;
    private final SimpleBlurredImageLoader simpleBlurredImageLoader;

    public interface Listener {
        void playClicked();
    }

    NewForYouHeaderRenderer(Listener listener,
                            @Provided ImageOperations imageOperations,
                            @Provided SimpleBlurredImageLoader simpleBlurredImageLoader) {
        this.listener = listener;
        this.imageOperations = imageOperations;
        this.simpleBlurredImageLoader = simpleBlurredImageLoader;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.new_for_you_header, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<NewForYouHeaderItem> items) {
        final NewForYouHeaderItem item = items.get(position);

        bindArtwork(item, itemView);
        bindPlayButton(item, itemView);
        bindTextViews(item, itemView);
    }

    private void bindArtwork(NewForYouHeaderItem item, View itemView) {
        final NewForYouArtworkView artworkView = ButterKnife.findById(itemView, R.id.artwork);
        final ImageView blurredArtworkView = ButterKnife.findById(itemView, R.id.blurred_background);

        if (item.mainImage().isPresent()) {
            artworkView.bindWithoutAnimation(imageOperations, item.mainImage().get());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                simpleBlurredImageLoader.displayBlurredArtwork(item.mainImage().get(), blurredArtworkView);
            }
        }
    }

    private void bindPlayButton(NewForYouHeaderItem item, View itemView) {
        itemView.findViewById(R.id.btn_play).setOnClickListener(v -> listener.playClicked());
    }

    private void bindTextViews(NewForYouHeaderItem item, View itemView) {
        final TextView duration = (TextView) itemView.findViewById(R.id.new_for_you_duration);
        final TextView updatedAt = (TextView) itemView.findViewById(R.id.new_for_you_updated_at);

        duration.setText(item.duration());
        updatedAt.setText(item.updatedAt());
    }
}
