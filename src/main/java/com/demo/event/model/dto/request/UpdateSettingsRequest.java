package com.demo.event.model.dto.request;

import lombok.Data;

@Data
public class UpdateSettingsRequest {

    private String language;    // vi, en
    private Boolean darkMode;
}
