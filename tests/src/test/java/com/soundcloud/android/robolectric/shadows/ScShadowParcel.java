package com.soundcloud.android.robolectric.shadows;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.shadows.ShadowParcel;

import android.os.Parcel;

import java.io.Serializable;
import java.lang.reflect.Field;

@Implements(Parcel.class)
public class ScShadowParcel extends ShadowParcel {

    @Implementation
    @SuppressWarnings("unchecked")
    public void writeSerializable(Serializable s) {
        getParcelData().add(s);
    }

    @Implementation
    public Serializable readSerializable() {
        try {
            // crazy hack to increment the private index field on ShadowParcel
            Field indexField = ShadowParcel.class.getDeclaredField("index");
            indexField.setAccessible(true);
            int index = getIndex();
            Serializable result = index < getParcelData().size() ? (Serializable) getParcelData().get(index) : null;
            indexField.setInt(this, index + 1);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
