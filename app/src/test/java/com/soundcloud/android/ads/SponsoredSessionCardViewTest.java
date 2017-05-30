package com.soundcloud.android.ads;

import static com.soundcloud.android.ads.PrestitialAdapter.PrestitialPage;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class SponsoredSessionCardViewTest extends AndroidUnitTest {

    private final static SponsoredSessionAd SPONSORED_SESSION_AD = AdFixtures.sponsoredSessionAd();

    @Mock ImageOperations imageOperations;
    @Mock PrestitialView.Listener listener;

    private View view;
    private SponsoredSessionCardView cardView;

    @Before
    public void setUp() {
        view = LayoutInflater.from(context()).inflate(R.layout.sponsored_session_action_page, new FrameLayout(context()));
        cardView = spy(new SponsoredSessionCardView(imageOperations, resources()));
    }

    @Test
    public void descriptionTextForOptInCardGetsSetupWithAdFreeListeningTime() {
        cardView.setupContentView(view, SPONSORED_SESSION_AD, listener, PrestitialPage.OPT_IN_CARD);

        assertThat(((TextView) view.findViewById(R.id.opt_in_text)).getText()).isEqualTo("Put a pause on ads: watch this video and unlock 60-minutes of nonstop music");
    }

    @Test
    public void optionButtonsTextForOptInIsSet() {
        cardView.setupContentView(view, SPONSORED_SESSION_AD, listener, PrestitialPage.OPT_IN_CARD);

        assertThat(((TextView) view.findViewById(R.id.btn_left)).getText()).isEqualTo("No Thanks");
        assertThat(((TextView) view.findViewById(R.id.btn_right)).getText()).isEqualTo("Watch Now");
    }

    @Test
    public void optionButtonsTextForEndCardIsSet() {
        cardView.setupContentView(view, SPONSORED_SESSION_AD, listener, PrestitialPage.END_CARD);

        assertThat(((TextView) view.findViewById(R.id.btn_left)).getText()).isEqualTo("Learn More");
        assertThat(((TextView) view.findViewById(R.id.btn_right)).getText()).isEqualTo("Start Session");
    }


    @Test
    public void firstOptionButtonForwardsClickToListener() {
        cardView.setupContentView(view, SPONSORED_SESSION_AD, listener, PrestitialPage.OPT_IN_CARD);

        final View buttonView = view.findViewById(R.id.btn_left);
        buttonView.performClick();

        verify(listener).onOptionOneClick(PrestitialPage.OPT_IN_CARD, SPONSORED_SESSION_AD, buttonView.getContext());
    }

    @Test
    public void secondOptionButtonForwardsClickToListener() {
        cardView.setupContentView(view, SPONSORED_SESSION_AD, listener, PrestitialPage.OPT_IN_CARD);

        final View buttonView = view.findViewById(R.id.btn_right);
        buttonView.performClick();

        verify(listener).onOptionTwoClick(PrestitialPage.OPT_IN_CARD, SPONSORED_SESSION_AD);
    }
}