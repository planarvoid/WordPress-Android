package com.soundcloud.android.api;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class FilePartTest {

    @Test
    public void shouldDefineEqualsAndHashCode() {
        EqualsVerifier.forClass(FilePart.class).verify();
    }
}
