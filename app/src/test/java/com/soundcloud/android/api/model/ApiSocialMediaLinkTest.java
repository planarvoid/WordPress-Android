package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.java.optional.Optional;
import org.junit.Test;

public class ApiSocialMediaLinkTest {
    @Test
    public void treatsEmptyTitleAsAbsent() {
        ApiSocialMediaLink link = ApiSocialMediaLink.create(Optional.of(""), "network", "url");
        assertThat(link.title()).isEqualTo(Optional.absent());
    }
}
