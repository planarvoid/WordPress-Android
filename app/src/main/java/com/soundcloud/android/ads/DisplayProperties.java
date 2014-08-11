package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DisplayProperties {

    private String defaultTextColor;
    private String defaultBackgroundColor;
    private String pressedTextColor;
    private String pressedBackgroundColor;
    private String focusedTextColor;
    private String focusedBackgroundColor;

    @JsonCreator
    public DisplayProperties(
            @JsonProperty("color.learn_more.default.text") String defaultTextColor,
            @JsonProperty("color.learn_more.default.background") String defaultBackgroundColor,
            @JsonProperty("color.learn_more.pressed.text") String pressedTextColor,
            @JsonProperty("color.learn_more.pressed.background")String pressedBackgroundColor,
            @JsonProperty("color.learn_more.focused.text") String focusedTextColor,
            @JsonProperty("color.learn_more.focused.background") String focusedBackgroundColor) {
        this.defaultTextColor = defaultTextColor;
        this.defaultBackgroundColor = defaultBackgroundColor;
        this.pressedTextColor = pressedTextColor;
        this.pressedBackgroundColor = pressedBackgroundColor;
        this.focusedTextColor = focusedTextColor;
        this.focusedBackgroundColor = focusedBackgroundColor;
    }

    public String getFocusedBackgroundColor() {
        return focusedBackgroundColor;
    }

    public String getFocusedTextColor() {
        return focusedTextColor;
    }

    public String getPressedBackgroundColor() {
        return pressedBackgroundColor;
    }

    public String getPressedTextColor() {
        return pressedTextColor;
    }

    public String getDefaultBackgroundColor() {
        return defaultBackgroundColor;
    }

    public String getDefaultTextColor() {
        return defaultTextColor;
    }

}
