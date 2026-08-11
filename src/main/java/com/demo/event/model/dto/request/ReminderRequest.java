package com.demo.event.model.dto.request;

import lombok.Data;

@Data
public class ReminderRequest {

    /** Chọn 1 trong 2 — không dùng đồng thời cả hai */
    private Integer remindDaysBefore;
    private Integer remindHoursBefore;

    /** Bật/tắt reminder — null được hiểu là bật (true) */
    private Boolean isEnabled;
}
