package com.soundcloud.android.collections;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class SingleLineCollectionTextViewTest {

    private SingleLineCollectionTextView textView;

    final String jon = "Jon";
    final String matthias = "Matthias";
    final String mustafa = "Mustafa";
    final String jan = "Jan";

    @Before
    public void setup() {
        textView = new SingleLineCollectionTextView(Robolectric.application);
    }

    @Test
    public void shouldOutputFirstName() {
        textView.setDisplayItems(newArrayList(jon));
        textView.setTextFromCollection(20);
        expect(textView.getText()).toEqual(jon);
    }

    @Test
    public void shouldOutputFirstTwoNames() {
        textView.setDisplayItems(newArrayList(jon, matthias));
        textView.setTextFromCollection(20);
        expect(textView.getText()).toEqual(jon + " and " + matthias);
    }

    @Test
    public void shouldOutputFirstNameAnd1Other() {
        textView.setDisplayItems(newArrayList(jon, matthias));
        textView.setTextFromCollection(10);
        expect(textView.getText()).toEqual(jon + " and 1 other");
    }

    @Test
    public void shouldOutputFirstTwoNamesAnd1Other() {
        textView.setDisplayItems(newArrayList(jon, matthias, mustafa));
        textView.setTextFromCollection(30);
        expect(textView.getText()).toEqual(jon + ", " + matthias + " and 1 other");
    }

    @Test
    public void shouldOutputFirstNameAnd2Others() {
        textView.setDisplayItems(newArrayList(jon, matthias, mustafa));
        textView.setTextFromCollection(20);
        expect(textView.getText()).toEqual(jon + " and 2 others");
    }

    @Test
    public void shouldOutputThirdNameAnd2Others() {
        textView.setDisplayItems(newArrayList(matthias, mustafa, jon));
        textView.setTextFromCollection(18);
        expect(textView.getText()).toEqual(jon + " and 2 others");
    }

    @Test
    public void shouldOutputThirdAndFourthNamesAnd2Others() {
        textView.setDisplayItems(newArrayList(matthias, mustafa, jon, jan));
        textView.setTextFromCollection(22);
        expect(textView.getText()).toEqual(jon + ", " + jan + " and 2 others");
    }

    @Test
    public void shouldOutputThirdNameAnd3Others() {
        textView.setDisplayItems(newArrayList(matthias, mustafa, jon, jan));
        textView.setTextFromCollection(20);
        expect(textView.getText()).toEqual(mustafa + " and 3 others");
    }

    @Test
    public void shouldOutputLastTryOnFailure() {
        textView.setDisplayItems(newArrayList(matthias, mustafa, jon, jan));
        textView.setTextFromCollection(0);
        expect(textView.getText()).toEqual(jan + " and 3 others");
    }

}
