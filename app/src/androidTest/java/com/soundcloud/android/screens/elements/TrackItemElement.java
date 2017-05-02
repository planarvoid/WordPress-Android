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
        return new DownloadImageViewElement(testDriver, wrapped.findOnScreenElement(With.id(R.id.item_download_state)));
    }

    public String getTitle() {
        return new TextElement(wrapped.findOnScreenElement(With.id(R.id.list_item_subheader))).getText();
    }

    public boolean isPromotedTrack() {
        return wrapped.findElement(With.id(R.id.promoted_item)).hasVisibility();
    }

    private boolean hasPromoter() {
        return wrapped.findElement(With.id(R.id.promoter)).hasVisibility();
    }

    public boolean hasReposter() {
        return wrapped.findElement(With.id(R.id.reposter)).hasVisibility();
    }

    public boolean isLongerThanOneMinute() {
        final String durationText = new TextElement(wrapped.findElement(With.id(R.id.list_item_right_info))).getText();
        return durationText.length() > 0 && Integer.parseInt(durationText.substring(0, 1)) != 0;
    }

    public VisualPlayerElement click() {
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        wrapped.click();
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public TrackItemMenuElement clickOverflowButton() {
        wrapped
                .findOnScreenElement(With.id(R.id.overflow_button))
                .click();

        return new TrackItemMenuElement(testDriver);
    }

    public static With WithReposter(final Han testDriver) {
        return new With() {

            @Override
            public boolean apply(ViewElement view) {
                return new TrackItemElement(testDriver, view).hasReposter();
            }

            @Override
            public String getSelector() {
                return "Track with reposter";
            }
        };
    }

    public static With WithTitle(final Han testDriver, final String title) {
        return new With() {

            @Override
            public boolean apply(ViewElement view) {
                return new TrackItemElement(testDriver, view).getTitle().equals(title);
            }

            @Override
            public String getSelector() {
                return String.format("Track with title %s", title);
            }
        };
    }

    public static With NotPromoted(final Han testDriver) {
        return new With() {

            @Override
            public boolean apply(ViewElement view) {
                return !new TrackItemElement(testDriver, view).isPromotedTrack();
            }

            @Override
            public String getSelector() {
                return "Not Promoted Track";
            }
        };
    }

    public static With LongTrack(final Han testDriver) {
        return new With() {
            @Override
            public String getSelector() {
                return "Track with duration >1min";
            }

            @Override
            public boolean apply(ViewElement input) {
                if (input.getId() != R.id.track_list_item) {
                    return false;
                }

                return new TrackItemElement(testDriver, input).isLongerThanOneMinute();
            }
        };
    }
}
