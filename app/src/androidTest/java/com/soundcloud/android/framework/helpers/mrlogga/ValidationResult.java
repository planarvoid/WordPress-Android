package com.soundcloud.android.framework.helpers.mrlogga;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;

class ValidationResult {

    private String name;
    private String status;
    private List<HashMap<String, Object>> invalidParams;

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("invalid_params")
    public void setInvalidParams(List<HashMap<String, Object>> invalidParams) {
        this.invalidParams = invalidParams;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append(" ").append(status).append(" ");

        for (HashMap<String, Object> param : invalidParams) {
            sb.append("[").append(param.toString()).append("]");
        }
        return sb.toString();
    }

}
