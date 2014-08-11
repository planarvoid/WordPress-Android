package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DisplayProperties {

    private String defaultText;
    private String defaultBackground;
    private String pressedText;
    private String pressedBackground;
    private String focusedText;
    private String focusedBackground;

    @JsonCreator
    public DisplayProperties(
            @JsonProperty("color.learn_more.default.text") String defaultText,
            @JsonProperty("color.learn_more.default.background") String defaultBackground,
            @JsonProperty("color.learn_more.pressed.text") String pressedText,
            @JsonProperty("color.learn_more.pressed.background")String pressedBackground,
            @JsonProperty("color.learn_more.focused.text") String focusedText,
            @JsonProperty("color.learn_more.focused.background") String focusedBackground) {
        this.defaultText = defaultText;
        this.defaultBackground = defaultBackground;
        this.pressedText = pressedText;
        this.pressedBackground = pressedBackground;
        this.focusedText = focusedText;
        this.focusedBackground = focusedBackground;
    }

    public String getFocusedBackground() {
        return focusedBackground;
    }

    public String getFocusedText() {
        return focusedText;
    }

    public String getPressedBackground() {
        return pressedBackground;
    }

    public String getPressedText() {
        return pressedText;
    }

    public String getDefaultBackground() {
        return defaultBackground;
    }

    public String getDefaultText() {
        return defaultText;
    }

}
