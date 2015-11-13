package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class TrackItemElement {
    private final ViewElement wrapped;
    private final Han testDriver;

    public TrackItemElement(Han testDriver, ViewElement wrapped) {
        this.wrapped = wrapped;
        this.testDriver = testDriver;
    }

    public DownloadImageViewElement downloadElement() {
        return new DownloadImageViewElement(wrapped.findElement(With.id(R.id.item_download_state)));
    }

    public String getTitle() {
        return new TextElement(wrapped.findElement(With.id(R.id.list_item_subheader))).getText();
    }

    public boolean isPromotedTrack() {
        return wrapped.findElement(With.id(R.id.promoted_track)).isVisible();
    }

    public boolean hasPromoter() {
        return hasPromoterInCardItem() || hasPromoterInListItem();
    }

    private boolean hasPromoterInListItem() {
        return getPromotedTrackText().getText().contains("Promoted by");
    }

    private boolean hasPromoterInCardItem() {
        return wrapped.findElement(With.id(R.id.promoter)).isVisible();
    }

    public boolean hasReposter() {
        return wrapped.findElement(With.id(R.id.reposter)).isVisible();
    }

    private TextElement getPromotedTrackText() {
        return new TextElement(wrapped.findElement(With.id(R.id.promoted_track)));
    }

    public VisualPlayerElement click() {
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        wrapped.click();
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public TrackItemMenuElement clickOverflowButton() {
        wrapped
                .findElement(With.id(R.id.overflow_button))
                .click();

        return new TrackItemMenuElement(testDriver);
    }
}
