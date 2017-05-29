package com.soundcloud.android.discovery;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import io.reactivex.subjects.PublishSubject;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;

public class MultipleContentSelectionCardRendererTest extends AndroidUnitTest {

    @Mock SelectionItemAdapterFactory selectionItemAdapterFactory;
    @Mock SelectionItemAdapter adapter;
    @Mock DiscoveryCard.MultipleContentSelectionCard card;
    private View itemView;
    private MultipleContentSelectionCardRenderer renderer;

    @Before
    public void setUp() {
        when(selectionItemAdapterFactory.create(any(PublishSubject.class))).thenReturn(adapter);
        when(adapter.selectionUrn()).thenReturn(Optional.of(Urn.NOT_SET));
        when(card.title()).thenReturn(Optional.of("title"));
        when(card.description()).thenReturn(Optional.of("description"));

        renderer = new MultipleContentSelectionCardRenderer(selectionItemAdapterFactory);

        itemView = renderer.createItemView(new LinearLayout(context()));
    }

    @Test
    public void bindsTitleWhenPresent() {
        TextView title = (TextView) itemView.findViewById(R.id.selection_title);

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(title).isVisible();
    }

    @Test
    public void doesNotBindTitleWhenNotPresent() {
        TextView title = (TextView) itemView.findViewById(R.id.selection_title);

        when(card.title()).thenReturn(Optional.absent());

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(title).isNotVisible();
    }

    @Test
    public void bindsDescriptionWhenPresent() {
        TextView description = (TextView) itemView.findViewById(R.id.selection_description);

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(description).isVisible();
    }

    @Test
    public void doesNotBindDescriptionWhenNotPresent() {
        TextView description = (TextView) itemView.findViewById(R.id.selection_description);

        when(card.description()).thenReturn(Optional.absent());

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(description).isNotVisible();
    }

    @Test
    public void bindsTheSelectionItemsInTheCarousel() {
        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        verify(adapter).updateSelection(card);
    }
}
