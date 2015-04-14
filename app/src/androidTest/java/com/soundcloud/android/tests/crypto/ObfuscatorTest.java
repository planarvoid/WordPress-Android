package com.soundcloud.android.tests.crypto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.crypto.Obfuscator;

import android.test.InstrumentationTestCase;

public class ObfuscatorTest extends InstrumentationTestCase {

    private Obfuscator obfuscator;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        obfuscator = new Obfuscator();
    }

    public void testObfuscatesAndRecoversString() {
        String plainText = "hi, this is some plain text! :)";

        String obfuscatedText = obfuscator.obfuscate(plainText);

        assertThat(obfuscatedText, not(equalTo(plainText)));
        assertThat(obfuscator.deobfuscateString(obfuscatedText), equalTo(plainText));
    }

    public void testObfuscatesAndRecoversBoolean() {
        String obfuscatedBoolean = obfuscator.obfuscate(true);

        assertThat(obfuscatedBoolean, not(equalTo(Boolean.toString(true))));
        assertThat(obfuscator.deobfuscateBoolean(obfuscatedBoolean), is(true));
    }

}
