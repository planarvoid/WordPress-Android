package com.soundcloud.android.framework.helpers.mrlogga;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;

class ValidationResult {

    private String name;
    private String status;
    private List<HashMap<String, String>> invalidParams;

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("invalid_params")
    public void setInvalidParams(List<HashMap<String, String>> invalidParams) {
        this.invalidParams = invalidParams;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append(" ").append(status).append(" ");

        for (HashMap<String, String> param : invalidParams) {
            sb.append("[").append(param).append("]");
        }
        return sb.toString();
    }

}
