package com.soundcloud.android.discovery;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DisplayMetricsStub;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.res.Resources;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;

public class SelectionItemRendererTest extends AndroidUnitTest {

    @Mock private Resources resources;
    @Mock private SelectionItem selectionItem;
    @Mock private ImageOperations imageOperations;
    @Mock private Navigator navigator;
    @Mock private OnClickListener onClickListener;

    private SelectionItemRenderer renderer;
    private View itemView;

    @Before
    public void setUp() throws Exception {
        renderer = new SelectionItemRenderer(imageOperations, resources, navigator);
        itemView = renderer.createItemView(new LinearLayout(context()));

        when(selectionItem.shortTitle()).thenReturn(Optional.of("title"));
        when(selectionItem.shortSubtitle()).thenReturn(Optional.of("subtitle"));
        when(selectionItem.artworkUrlTemplate()).thenReturn(Optional.of("artwork url"));
        when(selectionItem.count()).thenReturn(Optional.of(1));
        when(resources.getDisplayMetrics()).thenReturn(new DisplayMetricsStub(50, 50));
    }

    @Test
    public void bindsArtworkWhenPresent() {
        ImageView imageView = (ImageView) itemView.findViewById(R.id.artwork);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(imageView).isVisible();
        verify(imageOperations).displayWithPlaceholder(
                String.valueOf(selectionItem.hashCode()), selectionItem.artworkUrlTemplate(), ApiImageSize.getFullImageSize(resources), imageView);

    }

    @Test
    public void bindsArtworkWhenNotPresent() {
        when(selectionItem.artworkUrlTemplate()).thenReturn(Optional.absent());
        ImageView imageView = (ImageView) itemView.findViewById(R.id.artwork);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(imageView).isVisible();
        verify(imageOperations).displayWithPlaceholder(
                String.valueOf(selectionItem.hashCode()), selectionItem.artworkUrlTemplate(), ApiImageSize.getFullImageSize(resources), imageView);
    }

    @Test
    public void bindsTitleWhenPresent() {
        TextView title = (TextView) itemView.findViewById(R.id.title);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(title).isVisible();
    }

    @Test
    public void doesNotBindTitleWhenNotPresent() {
        when(selectionItem.shortTitle()).thenReturn(Optional.absent());
        TextView title = (TextView) itemView.findViewById(R.id.title);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(title).isNotVisible();
    }

    @Test
    public void bindsSubtitleWhenPresent() {
        TextView subtitle = (TextView) itemView.findViewById(R.id.secondary_text);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(subtitle).isVisible();
    }

    @Test
    public void doesNotBindSubtitleWhenNotPresent() {
        when(selectionItem.shortSubtitle()).thenReturn(Optional.absent());
        TextView subtitle = (TextView) itemView.findViewById(R.id.secondary_text);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(subtitle).isNotVisible();
    }

    @Test
    public void bindsCountWhenPresent() {
        TextView count = (TextView) itemView.findViewById(R.id.track_count);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(count).isVisible();
    }

    @Test
    public void doesNotBindCountWhenNotPresent() {
        when(selectionItem.count()).thenReturn(Optional.absent());
        TextView count = (TextView) itemView.findViewById(R.id.track_count);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(count).isNotVisible();
    }

    @Test
    public void doesNotBindOverflowMenu() {
        View overflowMenu = itemView.findViewById(R.id.overflow_button);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(overflowMenu).isNotVisible();
    }

    @Test
    public void bindsClickHandlingFromSelectionItem() {
        when(selectionItem.onClickListener(navigator)).thenReturn(onClickListener);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));
        itemView.performClick();

        verify(onClickListener).onClick(itemView);
    }

}
