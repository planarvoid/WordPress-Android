package com.soundcloud.android.utils;

import com.soundcloud.android.R;

import android.content.res.Resources;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Random;
import java.util.regex.Pattern;

public class ScTextUtils {
    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\._%\\-\\+]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
    );

    private ScTextUtils() {
    }

    /**
     * Like {@link android.text.Html#fromHtml(String)}, but with line separation handling
     * and guard against RuntimeExceptions.
     *
     * @param source the string to be transformed
     * @return spanned text
     */
    public static Spanned fromHtml(String source) {
        if (source == null || TextUtils.isEmpty(source)) return new SpannedString("");

        source = source.replace(System.getProperty("line.separator"), "<br/>");

        try {
            return Html.fromHtml(source);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                // Pushback buffer full, retry with smaller input
                return fromHtml(source.substring(0, source.length() / 2));
            } else {
                throw e;
            }
        }
    }

    /**
     * Adapted from the {@link android.text.util.Linkify} class. Changes the
     * first instance of {@code link} into a clickable link attached to the given listener
     * @param view the textview
     * @param link the link to set, or null to use the whole text
     * @param listener the listener
     * @param underline underline the text
     * @return true if the link was added
     */
    public static boolean clickify(TextView view, final String link, final ClickSpan.OnClickListener listener, boolean underline) {
        CharSequence text = view.getText();
        String string = text.toString();
        ClickSpan span = new ClickSpan(listener, underline);

        int start = 0, end = string.length();
        if (link != null) {
            start = string.indexOf(link);
            end = start + link.length();
            if (start == -1) return false;
        }

        if (text instanceof Spannable) {
            ((Spannable)text).setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            SpannableString s = SpannableString.valueOf(text);
            if (s != null) {  // robolectric
                s.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                view.setText(s);
            }
        }
        MovementMethod m = view.getMovementMethod();
        if (!(m instanceof LinkMovementMethod)) {
            view.setMovementMethod(LinkMovementMethod.getInstance());
        }
        return true;
    }

    public static String hexString(byte[] bytes) {
        return String.format("%0" + (bytes.length << 1) + "x", new BigInteger(1, bytes));
    }

    /**
     * @param pos play position in ms
     * @return formatted time string in the form of 0.05 or 2.12.04
     */
    public static String formatTimestamp(long pos){
        StringBuilder builder = new StringBuilder();
        int secs = (int) (pos / 1000);
        int minutes = secs  / 60;
        int hours = minutes / 60;
        if (hours > 0) {
            builder.append(hours);
            builder.append('.');
        }
        minutes = minutes % 60;
        if (hours > 0 && minutes < 10) builder.append('0');
        secs = secs % 60;
        builder.append(minutes).append('.');
        if (secs < 10) builder.append('0');
        builder.append(secs);
        return builder.toString();
    }

    public static CharSequence getElapsedTimeString(Resources r, long start, boolean longerText) {
        double elapsed = Double.valueOf(Math.ceil((System.currentTimeMillis() - start) / 1000d)).longValue();
        return getTimeString(r, elapsed, longerText);
    }

    public static String getTimeString(Resources r, double elapsedSeconds, boolean longerText) {
        if (elapsedSeconds < 60)
            return r.getQuantityString(longerText ? R.plurals.elapsed_seconds_ago : R.plurals.elapsed_seconds, (int) elapsedSeconds, (int) elapsedSeconds);
        else if (elapsedSeconds < 3600)
            return r.getQuantityString(longerText ? R.plurals.elapsed_minutes_ago : R.plurals.elapsed_minutes, (int) (elapsedSeconds / 60), (int) (elapsedSeconds / 60));
        else if (elapsedSeconds < 86400)
            return r.getQuantityString(longerText ? R.plurals.elapsed_hours_ago : R.plurals.elapsed_hours, (int) (elapsedSeconds / 3600), (int) (elapsedSeconds / 3600));
        else if (elapsedSeconds < 2592000)
            return r.getQuantityString(longerText ? R.plurals.elapsed_days_ago : R.plurals.elapsed_days, (int) (elapsedSeconds / 86400), (int) (elapsedSeconds / 86400));
        else if (elapsedSeconds < 31536000)
            return r.getQuantityString(longerText ? R.plurals.elapsed_months_ago : R.plurals.elapsed_months, (int) (elapsedSeconds / 2592000), (int) (elapsedSeconds / 2592000));
        else
            return r.getQuantityString(longerText ? R.plurals.elapsed_years_ago : R.plurals.elapsed_years, (int) (elapsedSeconds / 31536000), (int) (elapsedSeconds / 31536000));
    }

    public static String getTimeElapsed(Resources r, long timestamp){
        return getTimeString(r, Math.max(0, (System.currentTimeMillis() - timestamp)/1000), false);
    }

    public static boolean isEmail(CharSequence string) {
        return !TextUtils.isEmpty(string) && EMAIL_ADDRESS_PATTERN.matcher(string).matches();
    }

    public static class ClickSpan extends ClickableSpan {
        private OnClickListener mListener;
        private boolean mUnderline;

        public ClickSpan(OnClickListener listener, boolean underline) {
            mListener = listener;
            mUnderline = underline;
        }

        @Override
        public void onClick(View widget) {
            if (mListener != null) mListener.onClick();
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(mUnderline);
        }

        public interface OnClickListener {
            void onClick();
        }
    }

    /**
     * Based on
     * <a href="http://truelicense.java.net/apidocs/de/schlichtherle/util/ObfuscatedString.html">
     *  ObfuscatedString
     * </a>
     * @param obfuscated the obfuscated array
     * @return unobfuscated string
     */
    public static String deobfuscate(long[] obfuscated) {
        final int length = obfuscated.length;
        // The original UTF8 encoded string was probably not a multiple
        // of eight bytes long and is thus actually shorter than this array.
        final byte[] encoded = new byte[8 * (length - 1)];
        // Obtain the seed and initialize a new PRNG with it.
        final long seed = obfuscated[0];
        final Random prng = new Random(seed);

        // De-obfuscate.
        for (int i = 1; i < length; i++) {
            final long key = prng.nextLong();
            long l = obfuscated[i] ^ key;
            final int end = Math.min(encoded.length, 8 * (i - 1) + 8);
            for (int i1 = 8 * (i - 1); i1 < end; i1++) {
                encoded[i1] = (byte) l;
                l >>= 8;
            }
        }

        // Decode the UTF-8 encoded byte array into a string.
        // This will create null characters at the end of the decoded string
        // in case the original UTF8 encoded string was not a multiple of
        // eight bytes long.
        final String decoded;
        try {
            decoded = new String(encoded,
                    new String(new char[]{'\u0055', '\u0054', '\u0046', '\u0038'}) /* UTF8 */);
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex); // UTF-8 is always supported
        }

        // Cut off trailing null characters in case the original UTF8 encoded
        // string was not a multiple of eight bytes long.
        final int i = decoded.indexOf(0);
        return -1 == i ? decoded : decoded.substring(0, i);
    }
}
