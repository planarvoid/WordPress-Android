package com.soundcloud.android.api.legacy.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.onboarding.suggestions.SuggestedUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class ScModelTest {

    private ScModel model = new ScModel("soundcloud:users:1");

    @Test
    public void shouldUseIdFromIdFieldIfSet() {
        model.setId(2L);
        expect(model.getId()).toBe(2L);
    }

    @Test
    public void shouldSetIdFieldIfConstructedFromUrn() {
        expect(model.getId()).toBe(1L);
        expect(model.getId()).toBe(1L);
    }

    @Test
    public void shouldParseIdFromUrnIfIdFieldNotSet() {
        model.setId(ScModel.NOT_SET);
        expect(model.getId()).toBe(1L);
    }

    @Test
    public void shouldUpdateIdWhenSettingUrn() {
        model.setUrn("soundcloud:users:2");
        expect(model.getId()).toBe(2L);
    }

    @Test
    public void shouldReturnIdNotSetIfNoUrnSet() {
        expect((int) new SuggestedUser().getId()).toEqual(ScModel.NOT_SET);
    }

    @Test
    public void shouldImplementEqualsInTermsOfId() {
        expect(new ScModel()).not.toEqual(new ScModel(1L));
        expect(new ScModel(2L)).not.toEqual(model);
        expect(new ScModel("soundcloud:sounds:2")).not.toEqual(model);
        expect(new ScModel()).toEqual(new ScModel());
        expect(new ScModel(1L)).toEqual(model);
        expect(new ScModel("soundcloud:sounds:1")).toEqual(model);
    }

    @Test
    public void shouldImplementHashCodeInTermsOfId() {
        expect(new ScModel().hashCode()).not.toEqual(model.hashCode());
        expect(new ScModel(2L).hashCode()).not.toEqual(model.hashCode());
        expect(new ScModel("soundcloud:sounds:2").hashCode()).not.toEqual(model.hashCode());
        expect(new ScModel().hashCode()).toEqual(new ScModel().hashCode());
        expect(new ScModel(1L).hashCode()).toEqual(model.hashCode());
        expect(new ScModel("soundcloud:sounds:1").hashCode()).toEqual(model.hashCode());
    }

    @Test
    public void shouldBeParcelable() {
        Parcel parcel = Parcel.obtain();
        model.writeToParcel(parcel, 0);

        ScModel unparceledModel = new ScModel(parcel);
        expect(unparceledModel.getUrn()).toEqual(model.getUrn());
        expect(unparceledModel.getId()).toEqual(model.getId());
    }
}
