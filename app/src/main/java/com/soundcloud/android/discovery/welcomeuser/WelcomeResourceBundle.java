package com.soundcloud.android.discovery.welcomeuser;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.R;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

@AutoValue
public abstract class WelcomeResourceBundle {
    @DrawableRes
    abstract int backgroundResId();

    @DrawableRes
    abstract int detailSpriteResId();

    @ColorRes
    abstract int backgroundColorId();

    @ColorRes
    abstract int titleTextColorId();

    @ColorRes
    abstract int descriptionTextColorId();

    @StringRes
    abstract int titleStringId();

    static WelcomeResourceBundle forTimeOfDay(TimeOfDay timeOfDay) {
        switch (timeOfDay) {
            case MORNING:
                return new AutoValue_WelcomeResourceBundle(R.drawable.morning_sprite,
                                                           R.drawable.morning_sun,
                                                           R.color.welcome_morning,
                                                           R.color.ak_almost_black,
                                                           R.color.ak_dark_gray,
                                                           R.string.welcome_user_title_morning);
            case AFTERNOON:
                return new AutoValue_WelcomeResourceBundle(R.drawable.afternoon_sprite,
                                                           R.drawable.afternoon_sun,
                                                           R.color.welcome_afternoon,
                                                           R.color.ak_almost_black,
                                                           R.color.ak_dark_gray,
                                                           R.string.welcome_user_title_afternoon);
            case EVENING:
                return new AutoValue_WelcomeResourceBundle(R.drawable.evening_sprite,
                                                           R.drawable.dark_moon,
                                                           R.color.welcome_evening,
                                                           android.R.color.white,
                                                           R.color.seventy_percent_white,
                                                           R.string.welcome_user_title_evening);
            case NIGHT:
                return new AutoValue_WelcomeResourceBundle(R.drawable.night_sprite,
                                                           R.drawable.dark_moon,
                                                           R.color.welcome_night,
                                                           android.R.color.white,
                                                           R.color.seventy_percent_white,
                                                           R.string.welcome_user_title_night);
            default:
                throw new IllegalArgumentException("Not a valid time of day");
        }
    }
}
