package com.soundcloud.android.discovery;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.NavigationDelegate;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DisplayMetricsStub;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;

public class SingleSelectionContentCardRendererTest extends AndroidUnitTest {

    @Mock private Resources resources;
    @Mock private ImageOperations imageOperations;
    @Mock private DiscoveryCard.SingleContentSelectionCard card;
    @Mock private SelectionItem selectionItem;
    @Mock private NavigationDelegate navigationDelegate;
    @Mock private ScreenProvider screenProvider;
    @Mock private OnClickListener onClickListener;

    private SingleSelectionContentCardRenderer renderer;
    private View itemView;

    @Before
    public void setUp() throws Exception {
        renderer = new SingleSelectionContentCardRenderer(imageOperations, resources, navigationDelegate, screenProvider);
        itemView = renderer.createItemView(new LinearLayout(context()));

        when(card.title()).thenReturn(Optional.of("title"));
        when(card.description()).thenReturn(Optional.of("description"));
        when(card.selectionItem()).thenReturn(selectionItem);
        when(card.socialProof()).thenReturn(Optional.of("social proof"));
        when(card.socialProofAvatarUrlTemplates()).thenReturn(Collections.emptyList());
        when(selectionItem.count()).thenReturn(Optional.of(1));
        when(resources.getDisplayMetrics()).thenReturn(new DisplayMetricsStub(50, 50));
    }


    @Test
    public void bindsTitleWhenPresent() {
        TextView title = (TextView) itemView.findViewById(R.id.single_card_title);

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(title).isVisible();
    }

    @Test
    public void doesNotBindTitleWhenNotPresent() {
        when(card.title()).thenReturn(Optional.absent());
        TextView title = (TextView) itemView.findViewById(R.id.single_card_title);

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(title).isNotVisible();
    }

    @Test
    public void bindsDescriptionWhenPresent() {
        TextView description = (TextView) itemView.findViewById(R.id.single_card_description);

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(description).isVisible();
    }

    @Test
    public void doesNotBindDescriptionWhenNotPresent() {
        TextView description = (TextView) itemView.findViewById(R.id.single_card_description);
        when(card.description()).thenReturn(Optional.absent());

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(description).isNotVisible();
    }

    @Test
    public void bindsSelectionItemCountWhenPresent() {
        TextView count = (TextView) itemView.findViewById(R.id.single_card_track_count);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.single_card_artwork);

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(count).isVisible();
        verify(imageOperations).displayInAdapterView(
                String.valueOf(selectionItem.hashCode()), selectionItem.artworkUrlTemplate(), ApiImageSize.getFullImageSize(resources), imageView);
    }

    @Test
    public void doesNotBindSelectionItemCountWhenNotPresent() {
        TextView count = (TextView) itemView.findViewById(R.id.single_card_track_count);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.single_card_artwork);

        when(selectionItem.count()).thenReturn(Optional.absent());

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(count).isNotVisible();
        verify(imageOperations).displayInAdapterView(
                String.valueOf(selectionItem.hashCode()), selectionItem.artworkUrlTemplate(), ApiImageSize.getFullImageSize(resources), imageView);
    }

    @Test
    public void bindsSocialProofWhenPresent() {
        when(card.socialProofAvatarUrlTemplates()).thenReturn(Arrays.asList("link1", "link2"));
        when(resources.getConfiguration()).thenReturn(new Configuration());
        View container = itemView.findViewById(R.id.single_card_social_proof_container);
        TextView socialProofText = (TextView) container.findViewById(R.id.single_card_social_proof);

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(container).isVisible();
        assertThat(socialProofText).isVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_1)).isVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_2)).isVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_3)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_4)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_5)).isNotVisible();
    }

    @Test
    public void doesNotBindSocialProofAvatarsWhenListIsEmpty() {
        when(card.socialProofAvatarUrlTemplates()).thenReturn(Collections.emptyList());
        View container = itemView.findViewById(R.id.single_card_social_proof_container);

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(container).isVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_1)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_2)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_3)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_4)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_5)).isNotVisible();
    }

    @Test
    public void doesNotBindSocialProofWhenNotPresent() {
        View container = itemView.findViewById(R.id.single_card_social_proof_container);
        TextView socialProofText = (TextView) container.findViewById(R.id.single_card_social_proof);
        when(card.socialProof()).thenReturn(Optional.absent());

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(container).isNotVisible();
        assertThat(socialProofText).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_1)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_2)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_3)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_4)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_5)).isNotVisible();
    }

    @Test
    public void doesNotBindSocialProofAvatarsWhenNotPresent() {
        View container = itemView.findViewById(R.id.single_card_social_proof_container);
        TextView socialProofText = (TextView) container.findViewById(R.id.single_card_social_proof);

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(container).isVisible();
        assertThat(socialProofText).isVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_1)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_2)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_3)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_4)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_5)).isNotVisible();
    }

    @Test
    public void doesNotBindSocialProofTextWhenNotPresent() {
        when(card.socialProofAvatarUrlTemplates()).thenReturn(Arrays.asList("link1", "link2"));
        when(card.socialProof()).thenReturn(Optional.absent());
        when(resources.getConfiguration()).thenReturn(new Configuration());
        View container = itemView.findViewById(R.id.single_card_social_proof_container);
        TextView socialProofText = (TextView) container.findViewById(R.id.single_card_social_proof);

        renderer.bindItemView(0, itemView, Collections.singletonList(card));

        assertThat(container).isVisible();
        assertThat(socialProofText).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_1)).isVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_2)).isVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_3)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_4)).isNotVisible();
        assertThat(container.findViewById(R.id.single_card_user_artwork_5)).isNotVisible();
    }

    @Test
    public void bindsClickHandlingFromSelectionItem() {
        when(selectionItem.onClickListener(eq(navigationDelegate), any())).thenReturn(onClickListener);

        renderer.bindItemView(0, itemView, Collections.singletonList(card));
        itemView.performClick();

        verify(onClickListener).onClick(itemView);
    }
}
