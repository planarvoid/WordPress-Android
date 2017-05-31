package com.soundcloud.android.discovery;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageStyle;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;

public class SelectionItemRendererTest extends AndroidUnitTest {

    @Mock private SelectionItem selectionItem;
    @Mock private ImageOperations imageOperations;
    @Mock private Navigator navigator;
    @Mock private ScreenProvider screenProvider;

    private final PublishSubject<SelectionItem> selectionItemPublishSubject = PublishSubject.create();

    private SelectionItemRenderer renderer;
    private View itemView;

    @Before
    public void setUp() throws Exception {
        renderer = new SelectionItemRenderer(imageOperations, selectionItemPublishSubject);
        itemView = renderer.createItemView(new LinearLayout(context()));

        when(selectionItem.shortTitle()).thenReturn(Optional.of("title"));
        when(selectionItem.shortSubtitle()).thenReturn(Optional.of("subtitle"));
        when(selectionItem.artworkUrlTemplate()).thenReturn(Optional.of("artwork url"));
        when(selectionItem.artworkStyle()).thenReturn(Optional.of(ImageStyle.CIRCULAR));
        when(selectionItem.count()).thenReturn(Optional.of(1));
    }

    @Test
    public void bindsArtworkWhenPresent() {
        ImageView circularImageView = (ImageView) itemView.findViewById(R.id.circular_artwork);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        verify(imageOperations).displayCircularWithPlaceholder(
                String.valueOf(selectionItem.hashCode()), selectionItem.artworkUrlTemplate(), ApiImageSize.getFullImageSize(context().getResources()), circularImageView);
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
        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));
        final TestObserver<SelectionItem> testObserver = selectionItemPublishSubject.test();

        itemView.performClick();

        testObserver.assertValue(selectionItem);
    }

}
