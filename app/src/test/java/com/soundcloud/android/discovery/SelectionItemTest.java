package com.soundcloud.android.discovery;

import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SelectionItemTest {

    private static final Urn SELECTION_URN = Urn.forSystemPlaylist("upload");
    private static final String APP_LINK = "appLink";
    private static final String WEB_LINK = "webLink";

    @Test
    public void linkReturnsAppLinkWhenPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(of(APP_LINK), absent());

        final Optional<String> link = selectionItem.link();

        assertThat(link.isPresent()).isTrue();
        assertThat(link.get()).isEqualTo(APP_LINK);
    }

    @Test
    public void linkReturnsWebLinkWhenPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(absent(), of(WEB_LINK));

        final Optional<String> link = selectionItem.link();

        assertThat(link.isPresent()).isTrue();
        assertThat(link.get()).isEqualTo(WEB_LINK);
    }

    @Test
    public void linkReturnsAppLinkWhenBothAppAndWebLinkPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(of(APP_LINK), of(WEB_LINK));

        final Optional<String> link = selectionItem.link();

        assertThat(link.isPresent()).isTrue();
        assertThat(link.get()).isEqualTo(APP_LINK);
    }


    @Test
    public void linkAbsentWhenNeitherIsPresent() {
        final SelectionItem selectionItem = selectionItemWithLinks(absent(), absent());

        final Optional<String> link = selectionItem.link();

        assertThat(link.isPresent()).isFalse();
    }

    private SelectionItem selectionItemWithLinks(Optional<String> appLink, Optional<String> webLink) {
        return SelectionItem.create(absent(), SELECTION_URN, absent(), absent(), absent(), absent(), absent(), appLink, webLink);
    }
}
