package com.soundcloud.android.ads;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class VisualAdDisplayProperties {

    public static VisualAdDisplayProperties create(ApiDisplayProperties apiDisplayProperties) {
        return new AutoValue_VisualAdDisplayProperties(
                apiDisplayProperties.defaultTextColor,
                apiDisplayProperties.defaultBackgroundColor,
                apiDisplayProperties.pressedTextColor,
                apiDisplayProperties.pressedBackgroundColor,
                apiDisplayProperties.focusedTextColor,
                apiDisplayProperties.focusedBackgroundColor
        );
    }

    public abstract String getDefaultTextColor();

    public abstract String getDefaultBackgroundColor();

    public abstract String getPressedTextColor();

    public abstract String getPressedBackgroundColor();

    public abstract String getFocusedTextColor();

    public abstract String getFocusedBackgroundColor();
}
