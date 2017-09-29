package com.soundcloud.android.offline;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.squareup.sqldelight.ColumnAdapter;

import android.support.annotation.NonNull;

@SuppressWarnings({"PMD.AbstractClassWithoutAnyMethod"})
@AutoValue
abstract class TrackDownloadsDbModel implements TrackDownloadsModel {

    static final Factory<TrackDownloadsDbModel> FACTORY = new Factory<>(AutoValue_TrackDownloadsDbModel::new, new ColumnAdapter<Urn, String>() {
        @NonNull
        @Override
        public Urn decode(String s) {
            return new Urn(s);
        }

        @Override
        public String encode(@NonNull Urn urn) {
            return urn.toString();
        }
    });
}
