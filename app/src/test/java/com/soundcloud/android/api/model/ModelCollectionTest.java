package com.soundcloud.android.api.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

public class ModelCollectionTest {

    @Test
    public void implementsHashCodeAndEquals() {
        EqualsVerifier.forClass(ModelCollection.class)
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

}