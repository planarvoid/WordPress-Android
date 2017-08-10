package com.soundcloud.android.utils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.List;

public class UrnsTest extends AndroidUnitTest {

    private static final String KEY = "key";
    private static final Urn URN = Urn.forTrack(123);

    @Test
    public void shouldConvertUrnsToStringsWithFunction() {
        final List<Urn> input = asList(new Urn("a"), new Urn("b"));
        final List<String> output = Lists.transform(input, input1 -> input1.getContent());
        assertThat(output).containsExactly("a", "b");
    }

    @Test
    public void shouldConvertUrnsToStrings() {
        final List<Urn> input = asList(new Urn("a"), new Urn("b"));
        final List<String> output = Urns.toString(input);
        assertThat(output).containsExactly("a", "b");
    }

    @Test
    public void shouldConvertUrnsToIdString() {
        final List<Urn> input = asList(Urn.forTrack(1), Urn.forTrack(2));
        final String output = Urns.toJoinedIds(input, ",");
        assertThat(output).isEqualTo("1,2");
    }

    @Test
    public void shouldExtractAllIdsFromUrns() throws Exception {
        List<Long> ids = Urns.extractIds(sampleUrns(), Optional.absent());

        assertThat(ids).containsExactly(1L, 2L, 3L);
    }

    @Test
    public void shouldExtractIdsFromUrnsMatchingPredicate() throws Exception {
        List<Long> ids = Urns.extractIds(sampleUrns(), Optional.of(Urns.playlistPredicate()));

        assertThat(ids).containsExactly(2L);
    }

    @Test
    public void writesAndReadsUrnInBundle() {
        final Bundle bundle = new Bundle();
        Urns.writeToBundle(bundle, KEY, URN);

        assertThat(Urns.urnFromBundle(bundle, KEY)).isEqualTo(URN);
    }

    @Test
    public void returnsEmptyWhenUrnNotInBundle() {
        final Bundle bundle = new Bundle();

        assertThat(Urns.urnFromBundle(bundle, KEY)).isNull();
        assertThat(Urns.optionalUrnFromBundle(bundle, KEY).isPresent()).isFalse();
    }

    @Test
    public void writesAndReadsUrnInIntent() {
        final Intent intent = new Intent();
        Urns.writeToIntent(intent, KEY, URN);

        assertThat(Urns.urnFromIntent(intent, KEY)).isEqualTo(URN);
    }

    @Test
    public void returnsEmptyWhenUrnNotInIntent() {
        final Intent intent = new Intent();

        assertThat(Urns.urnFromIntent(intent, KEY)).isNull();
        assertThat(Urns.optionalUrnFromIntent(intent, KEY).isPresent()).isFalse();
    }

    @Test
    public void writesAndReadsUrnsInIntent() {
        final Intent intent = new Intent();
        final List<Urn> urns = sampleUrns();
        Urns.writeToIntent(intent, KEY, urns);

        assertThat(Urns.urnsFromIntent(intent, KEY)).isEqualTo(urns);
    }

    @Test
    public void writesAndReadsUrnInParcel() {
        final Parcel parcel = Parcel.obtain();
        Urns.writeToParcel(parcel, URN);

        parcel.setDataPosition(0);

        assertThat(Urns.urnFromParcel(parcel)).isEqualTo(URN);
    }

    @Test
    public void returnsEmptyWhenUrnNotInParcel() {
        final Parcel parcel = Parcel.obtain();

        assertThat(Urns.urnFromParcel(parcel)).isNull();
        assertThat(Urns.optionalUrnFromParcel(parcel).isPresent()).isFalse();
    }

    @Test
    public void writesAndReadsUrnsInParcel() {
        final Parcel parcel = Parcel.obtain();
        final List<Urn> urns = sampleUrns();
        Urns.writeToParcel(parcel, urns);

        parcel.setDataPosition(0);

        assertThat(Urns.urnsFromParcel(parcel)).isEqualTo(urns);
    }

    @NonNull
    private List<Urn> sampleUrns() {
        return asList(
                Urn.forTrack(1L),
                Urn.forPlaylist(2L),
                Urn.forUser(3L)
        );
    }
}
