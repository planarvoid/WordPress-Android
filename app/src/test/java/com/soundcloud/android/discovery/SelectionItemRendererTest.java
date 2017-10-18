package com.soundcloud.android.discovery;

import static com.soundcloud.java.optional.Optional.fromNullable;
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
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;

public class SelectionItemRendererTest extends AndroidUnitTest {

    private static final String SHORT_TITLE = "title";
    private static final String SHORT_SUBTITLE = "subtitle";
    private static final Integer COUNT = 1;
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
                selectionItem.getUrn() == null ? Urn.NOT_SET : selectionItem.getUrn(),
                fromNullable(selectionItem.getArtworkUrlTemplate()),
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
        final SelectionItemViewModel selectionItem = createSelectionItem(null, SHORT_SUBTITLE, COUNT);
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
        final SelectionItemViewModel selectionItem = createSelectionItem(SHORT_TITLE, null, COUNT);
        TextView subtitle = itemView.findViewById(R.id.secondary_text);

        renderer.bindItemView(0, itemView, Collections.singletonList(selectionItem));

        assertThat(subtitle).isNotVisible();
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

    private SelectionItemViewModel createSelectionItem(@Nullable String shortTitle, @Nullable String shortSubtitle, @Nullable Integer count) {
        return new SelectionItemViewModel(null,
                                          Urn.forSystemPlaylist("sel_item"),
                                          "artwork url",
                                          ImageStyle.CIRCULAR,
                                          count,
                                          shortTitle,
                                          shortSubtitle,
                                          null,
                                          null,
                                          null);
    }
}
