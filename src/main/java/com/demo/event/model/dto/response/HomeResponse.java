package com.demo.event.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HomeResponse {
    private List<EventResponse> upcomingEvents;
    private List<EventResponse> myEvents;
    private List<RelativeResponse> relatives;
}
