package com.soundcloud.android.discovery;

import com.soundcloud.propeller.utils.StringUtils;
import com.squareup.sqldelight.ColumnAdapter;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public class StringListAdapter implements ColumnAdapter<List<String>, String> {
    @NonNull
    @Override
    public List<String> decode(String s) {
        return Arrays.asList(s.split(","));
    }

    @Override
    public String encode(@NonNull List<String> strings) {
        return StringUtils.join(strings, ",");
    }
}
