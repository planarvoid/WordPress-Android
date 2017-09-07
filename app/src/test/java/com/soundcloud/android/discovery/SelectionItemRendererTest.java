package com.soundcloud.android.discovery;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;


import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageStyle;
import com.soundcloud.android.model.Urn;
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

    private static final Optional<String> SHORT_TITLE = Optional.of("title");
    private static final Optional<String> SHORT_SUBTITLE = Optional.of("subtitle");
    private static final Optional<Integer> COUNT = Optional.of(1);
    @Mock private ImageOperations imageOperations;
    @Mock private Navigator navigator;
    @Mock private ScreenProvider screenProvider;

    private final PublishSubject<SelectionItemViewModel> selectionItemPublishSubject = PublishSubject.create();

    private SelectionItemRenderer renderer;
    private View itemView;

    @Before
    public void setUp() throws Exception {
        renderer = new SelectionItemRenderer(imageOperations, selectionItemPublishSubject);
        itemView = renderer.createItemView(new LinearLayout(context()));
    }

    @Test
    public void bindsArtworkWhenPresent() {
        final SelectionItemViewModel selectionItem = defaultSelectionItem();
        ImageView circularImageView = itemView.findViewById(R.id.circular_artwork);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        verify(imageOperations).displayCircularWithPlaceholder(
                selectionItem.getUrn().isPresent() ? selectionItem.getUrn().get() : Urn.NOT_SET,
                selectionItem.getArtworkUrlTemplate(),
                ApiImageSize.getFullImageSize(context().getResources()),
                circularImageView);
    }

    @Test
    public void bindsTitleWhenPresent() {
        final SelectionItemViewModel selectionItem = defaultSelectionItem();
        TextView title = itemView.findViewById(R.id.title);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(title).isVisible();
    }

    @Test
    public void doesNotBindTitleWhenNotPresent() {
        final SelectionItemViewModel selectionItem = createSelectionItem(Optional.absent(), SHORT_SUBTITLE, COUNT);
        TextView title = itemView.findViewById(R.id.title);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(title).isNotVisible();
    }

    @Test
    public void bindsSubtitleWhenPresent() {
        final SelectionItemViewModel selectionItem = defaultSelectionItem();
        TextView subtitle = itemView.findViewById(R.id.secondary_text);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(subtitle).isVisible();
    }

    @Test
    public void doesNotBindSubtitleWhenNotPresent() {
        final SelectionItemViewModel selectionItem = createSelectionItem(SHORT_TITLE, Optional.absent(), COUNT);
        TextView subtitle = itemView.findViewById(R.id.secondary_text);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(subtitle).isNotVisible();
    }

    @Test
    public void bindsCountWhenPresent() {
        final SelectionItemViewModel selectionItem = defaultSelectionItem();
        TextView count = itemView.findViewById(R.id.track_count);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(count).isVisible();
    }

    @Test
    public void doesNotBindCountWhenNotPresent() {
        final SelectionItemViewModel selectionItem = createSelectionItem(SHORT_TITLE, SHORT_SUBTITLE, Optional.absent());
        TextView count = itemView.findViewById(R.id.track_count);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(count).isNotVisible();
    }

    @Test
    public void doesNotBindOverflowMenu() {
        final SelectionItemViewModel selectionItem = defaultSelectionItem();
        View overflowMenu = itemView.findViewById(R.id.overflow_button);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(overflowMenu).isNotVisible();
    }

    @Test
    public void bindsClickHandlingFromSelectionItem() {
        final SelectionItemViewModel selectionItem = defaultSelectionItem();
        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));
        final TestObserver<SelectionItemViewModel> testObserver = selectionItemPublishSubject.test();

        itemView.performClick();

        testObserver.assertValue(selectionItem);
    }

    private SelectionItemViewModel defaultSelectionItem() {
        return createSelectionItem(SHORT_TITLE, SHORT_SUBTITLE, COUNT);
    }

    private SelectionItemViewModel createSelectionItem(Optional<String> shortTitle, Optional<String> shortSubtitle, Optional<Integer> count) {
        return new SelectionItemViewModel(Optional.absent(),
                                          Urn.forSystemPlaylist("sel_item"),
                                          Optional.of("artwork url"),
                                          Optional.of(ImageStyle.CIRCULAR),
                                          count,
                                          shortTitle,
                                          shortSubtitle,
                                          Optional.absent(),
                                          Optional.absent(),
                                          Optional.absent());
    }
}
