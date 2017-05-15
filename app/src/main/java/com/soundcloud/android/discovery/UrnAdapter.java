package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.squareup.sqldelight.ColumnAdapter;

import android.support.annotation.NonNull;

public class UrnAdapter implements ColumnAdapter<Urn, String> {
    @NonNull
    @Override
    public Urn decode(String s) {
        return new Urn(s);
    }

    @Override
    public String encode(@NonNull Urn urn) {
        return urn.toString();
    }
}
