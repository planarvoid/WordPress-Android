package com.soundcloud.android.utils;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.R;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

import android.content.res.Resources;
import android.graphics.Color;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ScTextUtils {
    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "\\A([a-z0-9_\\-][a-z0-9_\\-\\+\\.]{0,62})?[a-z0-9_\\-]@(([a-z0-9]|[a-z0-9][a-z0-9\\-]*[a-z0-9])\\.)+[a-z]{2,}\\Z"
    );

    public static final String EMPTY_STRING = "";
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###,###,###,###");
    public static final DecimalFormat ROUNDED_FORMAT = getRoundedFormat();
    public static final String SPACE_SEPARATOR = " ";

    private ScTextUtils() {
    }

    @Deprecated // use Strings.isBlank
    public static boolean isBlank(@Nullable String string) {
        return Strings.isBlank(string);
    }

    @Deprecated // use Strings.isNotBlank
    public static boolean isNotBlank(@Nullable String string) {
        return Strings.isNotBlank(string);
    }

    @Deprecated // use Strings.safeToString
    public static String safeToString(@Nullable Object object) {
        return Strings.safeToString(object);
    }

    /**
     * Prefer this method over Guava's Longs.tryParse and Java's parseLong, since the former will fail on 2.2 devices
     * (due to String.isEmpty not being available) and the latter will throw NumberFormatException on null and bogus
     * Strings. This method does not throw any exceptions.
     *
     * @param longString a string containing a long (might be null)
     * @return the long, or -1 if parsing failed
     */
    public static long safeParseLong(@Nullable String longString) {
        try {
            return Long.parseLong(longString);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Like {@link android.text.Html#fromHtml(String)}, but with line separation handling
     * and guard against RuntimeExceptions.
     *
     * @param source the string to be transformed
     * @return spanned text
     */
    public static Spanned fromHtml(String source) {
        if (source == null || TextUtils.isEmpty(source)) {
            return new SpannedString(EMPTY_STRING);
        }

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
     *
     * @param view      the textview
     * @param link      the link to set, or null to use the whole text
     * @param listener  the listener
     * @param underline underline the text
     * @param highlight highlight the clickable text on state change
     * @return true if the link was added
     */
    public static boolean clickify(TextView view, final String link, final ClickSpan.OnClickListener listener, boolean underline, boolean highlight) {
        CharSequence text = view.getText();
        String string = text.toString();
        ClickSpan span = new ClickSpan(listener, underline);

        int start = 0, end = string.length();
        if (link != null) {
            start = string.indexOf(link);
            end = start + link.length();
            if (start == -1) {
                return false;
            }
        }

        if (text instanceof Spannable) {
            ((Spannable) text).setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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

        if (!highlight) {
            view.setHighlightColor(Color.TRANSPARENT);
        } // it will highlight by default
        return true;
    }

    public static String getClippedString(String string, int maxLength) {
        checkArgument(isNotBlank(string), "String must be non null/not empty");
        int length = (string.length() < maxLength) ? string.length() : maxLength;
        return string.substring(0, length);
    }

    /**
     * @return formatted time string in the form of 0:05 or 2:12:04
     */
    public static String formatTimestamp(long duration, TimeUnit unit) {
        final StringBuilder builder = new StringBuilder();
        final long hours = unit.toHours(duration);
        if (hours > 0) {
            builder.append(hours).append(':');
        }
        final long minutes = unit.toMinutes(duration) % 60;
        if (hours > 0 && minutes < 10) {
            builder.append('0');
        }

        builder.append(minutes).append(':');
        final long seconds = unit.toSeconds(duration) % 60;
        if (seconds < 10) {
            builder.append('0');
        }
        return builder.append(seconds).toString();
    }

    public static boolean usesSameTimeElapsedString(double elapsedSeconds1, double elapsedSeconds2) {
        if (elapsedSeconds1 < 60) {
            return (int) elapsedSeconds1 == (int) elapsedSeconds2;
        } else if (elapsedSeconds1 < 3600) {
            return (int) (elapsedSeconds1 / 60) == (int) (elapsedSeconds2 / 60);
        } else if (elapsedSeconds1 < 86400) {
            return (int) (elapsedSeconds1 / 3600) == (int) (elapsedSeconds2 / 3600);
        } else if (elapsedSeconds1 < 2592000) {
            return (int) (elapsedSeconds1 / 86400) == (int) (elapsedSeconds2 / 86400);
        } else if (elapsedSeconds1 < 31536000) {
            return (int) (elapsedSeconds1 / 2592000) == (int) (elapsedSeconds2 / 2592000);
        } else {
            return (int) (elapsedSeconds1 / 31536000) == (int) (elapsedSeconds2 / 31536000);
        }
    }

    public static String formatSecondsOrMinutes(Resources resources, long time, TimeUnit unit) {
        int seconds = (int) unit.toSeconds(time);
        if (seconds < 60) {
            return String.format(resources.getString(R.string.format_abbreviated_seconds), seconds);
        }
        return String.format(resources.getString(R.string.format_abbreviated_minutes), unit.toMinutes(time));
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    public static String formatTimeElapsed(Resources r, double elapsedSeconds, boolean longerText) {
        if (elapsedSeconds < 60) {
            return r.getQuantityString(longerText ? R.plurals.elapsed_seconds_ago : R.plurals.elapsed_seconds, (int) elapsedSeconds, (int) elapsedSeconds);
        } else if (elapsedSeconds < 3600) {
            return r.getQuantityString(longerText ? R.plurals.elapsed_minutes_ago : R.plurals.elapsed_minutes, (int) (elapsedSeconds / 60), (int) (elapsedSeconds / 60));
        } else if (elapsedSeconds < 86400) {
            return r.getQuantityString(longerText ? R.plurals.elapsed_hours_ago : R.plurals.elapsed_hours, (int) (elapsedSeconds / 3600), (int) (elapsedSeconds / 3600));
        } else if (elapsedSeconds < 2592000) {
            return r.getQuantityString(longerText ? R.plurals.elapsed_days_ago : R.plurals.elapsed_days, (int) (elapsedSeconds / 86400), (int) (elapsedSeconds / 86400));
        } else if (elapsedSeconds < 31536000) {
            return r.getQuantityString(longerText ? R.plurals.elapsed_months_ago : R.plurals.elapsed_months, (int) (elapsedSeconds / 2592000), (int) (elapsedSeconds / 2592000));
        } else {
            return r.getQuantityString(longerText ? R.plurals.elapsed_years_ago : R.plurals.elapsed_years, (int) (elapsedSeconds / 31536000), (int) (elapsedSeconds / 31536000));
        }
    }

    public static String formatTimeElapsed(Resources r, long elapsedSeconds) {
        return formatTimeElapsedSince(r, elapsedSeconds, false);
    }

    public static String formatTimeElapsedSince(Resources r, long timestamp, boolean longerText) {
        return formatTimeElapsed(r, Math.max(0, (System.currentTimeMillis() - timestamp) / 1000), longerText);
    }

    public static boolean isEmail(CharSequence string) {
        return !TextUtils.isEmpty(string) && EMAIL_ADDRESS_PATTERN.matcher(string.toString()
                .toLowerCase(Locale.US)).matches();
    }

    public static String getLocation(String city, String country) {
        if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(country)) {
            return city + ", " + country;
        } else if (!TextUtils.isEmpty(city)) {
            return city;
        } else if (!TextUtils.isEmpty(country)) {
            return country;
        } else {
            return EMPTY_STRING;
        }
    }

    public static String formatFollowingMessage(Resources r, int otherFollowers) {
        if (otherFollowers == 0) {
            return r.getString(R.string.following_zero);
        } else {
            String others = formatNumberWithCommas(otherFollowers);
            return r.getQuantityString(R.plurals.following_message, otherFollowers, others);
        }
    }

    public static String formatFollowersMessage(Resources r, int followers) {
        return r.getQuantityString(R.plurals.followers_message, followers, formatNumberWithCommas(followers));
    }

    public static boolean isNotBlank(CharSequence sequence) {
        return sequence != null && isNotBlank(sequence.toString());
    }

    public static class ClickSpan extends ClickableSpan {
        private final OnClickListener listener;
        private final boolean underline;

        public ClickSpan(OnClickListener listener, boolean underline) {
            this.listener = listener;
            this.underline = underline;
        }

        @Override
        public void onClick(View widget) {
            if (listener != null) {
                listener.onClick();
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(underline);
        }

        public interface OnClickListener {
            void onClick();
        }
    }

    public static abstract class TextValidator implements TextWatcher {
        private final TextView textView;

        public TextValidator(TextView textView) {
            this.textView = textView;
        }

        public abstract void validate(TextView textView, String text);

        @Override
        final public void afterTextChanged(Editable s) {
            validate(textView, textView.getText().toString());
        }

        @Override
        final public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Don't care */ }

        @Override
        final public void onTextChanged(CharSequence s, int start, int before, int count) { /* Don't care */ }
    }

    public static String formatNumberWithCommas(long number) {
        return DECIMAL_FORMAT.format(number);
    }

    private static DecimalFormat getRoundedFormat() {
        final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        final DecimalFormat format = new DecimalFormat("###,###.#", symbols);
        format.setRoundingMode(RoundingMode.DOWN);
        return format;
    }

    private static String shortenFactorialNumber(double number) {
        return ROUNDED_FORMAT.format(number);
    }

    public static String shortenLargeNumber(int number) {
        if (number <= 0) {
            return EMPTY_STRING;
        } else if (number >= 10000) {
            return "9K+"; // top out at 9k or text gets too long again
        } else if (number >= 1000) {
            return number / 1000 + "K+";
        } else {
            return String.valueOf(number);
        }
    }

    public static String formatLargeNumber(long number) {
        if (number <= 0) {
            return EMPTY_STRING;
        } else if (number <= 9999) {
            return formatNumberWithCommas(number);
        } else if (number <= 999999) {
            return shortenFactorialNumber(number / 1000.0) + "K";
        } else if (number <= 999999999) {
            return shortenFactorialNumber(number / 1000000.0) + "M";
        } else {
            return shortenFactorialNumber(number / 1000000000.0) + "BN";
        }
    }

    public static String fromSnakeCaseToCamelCase(String string) {
        String[] parts = string.split("_");
        StringBuilder stringBuilder = new StringBuilder();
        for (String part : parts) {
            stringBuilder.append(toTitleCase(part));
        }
        return stringBuilder.toString();
    }

    public static String toTitleCase(String word) {
        return word.substring(0, 1).toUpperCase() +
                word.substring(1).toLowerCase();
    }

}
