package com.soundcloud.android.crypto;

import com.soundcloud.android.utils.Log;
import com.soundcloud.java.strings.Charsets;

import android.util.Base64;

import javax.inject.Inject;

public class Obfuscator {

    private static final String RANDOM = "VkIjYfvMq2U4v0IdSD1vtjuncSVbXnhZtOloUMiR773TMhx1yeYhN8YLnkrx";

    private static final String TAG = Obfuscator.class.getSimpleName();

    @Inject
    public Obfuscator() {}

    public String obfuscate(String input) {
        String output = Base64.encodeToString(xor(input, RANDOM).getBytes(Charsets.UTF_8), Base64.DEFAULT).trim();
        Log.d(TAG, input + " -> " + output);
        return output;
    }

    public String obfuscate(boolean input) {
        return obfuscate(String.valueOf(input));
    }

    public String deobfuscateString(String input) {
        String output = xor(new String(Base64.decode(input.getBytes(Charsets.UTF_8), Base64.DEFAULT)), RANDOM);
        Log.d(TAG, input + " -> " + output);
        return output;
    }

    public boolean deobfuscateBoolean(String input) {
        return Boolean.parseBoolean(deobfuscateString(input));
    }

    private String xor(String a, String b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length() && i < b.length(); i++) {
            sb.append((char) (a.charAt(i) ^ b.charAt(i)));
        }
        return sb.toString();
    }

}
