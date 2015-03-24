package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class ApiDisplayProperties {

    public final String defaultTextColor;
    public final String defaultBackgroundColor;
    public final String pressedTextColor;
    public final String pressedBackgroundColor;
    public final String focusedTextColor;
    public final String focusedBackgroundColor;

    @JsonCreator
    public ApiDisplayProperties(
            @JsonProperty("color.learn_more.default.text") String defaultTextColor,
            @JsonProperty("color.learn_more.default.background") String defaultBackgroundColor,
            @JsonProperty("color.learn_more.pressed.text") String pressedTextColor,
            @JsonProperty("color.learn_more.pressed.background") String pressedBackgroundColor,
            @JsonProperty("color.learn_more.focused.text") String focusedTextColor,
            @JsonProperty("color.learn_more.focused.background") String focusedBackgroundColor) {
        this.defaultTextColor = defaultTextColor;
        this.defaultBackgroundColor = defaultBackgroundColor;
        this.pressedTextColor = pressedTextColor;
        this.pressedBackgroundColor = pressedBackgroundColor;
        this.focusedTextColor = focusedTextColor;
        this.focusedBackgroundColor = focusedBackgroundColor;
    }

}
