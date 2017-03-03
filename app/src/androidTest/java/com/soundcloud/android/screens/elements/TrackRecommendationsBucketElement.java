package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.discovery.ViewAllTrackRecommendationsScreen;

public class TrackRecommendationsBucketElement {
    private final Han testDriver;
    private final ViewElement wrapped;

    public TrackRecommendationsBucketElement(Han testDriver,
                                             ViewElement wrapped) {
        this.testDriver = testDriver;
        this.wrapped = wrapped;
    }

    public boolean isOnScreen() {
        return wrapped.isOnScreen();
    }

    public String getReason() {
        return getText(reason());
    }

    private ViewElement reason() {
        return wrapped.findOnScreenElementWithPopulatedText(With.id(R.id.reason));
    }

    public VisualPlayerElement clickReason() {
        reason().click();

        return new VisualPlayerElement(testDriver);
    }

    public String getFirstRecommendedTrackTitle() {
        return getText(wrapped.findOnScreenElementWithPopulatedText(With.id(R.id.recommendation_title)));
    }

    public VisualPlayerElement clickFirstRecommendedTrack() {
        firstRecommendation().click();

        return new VisualPlayerElement(testDriver);
    }

    public ViewAllTrackRecommendationsScreen clickViewAll() {
        viewAllTrackRecommendations().click();
        return new ViewAllTrackRecommendationsScreen(testDriver);
    }

    private ViewElement firstRecommendation() {
        return wrapped.findOnScreenElement(With.id(R.id.recommendation_item));
    }

    private ViewElement viewAllTrackRecommendations() {
        return wrapped.findOnScreenElement(With.id(R.id.recommendations_view_all));

    }

    private String getText(ViewElement element) {
        return new TextElement(element).getText();
    }
}
