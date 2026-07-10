package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class DailyConversionResponse {
    private final LocalDate date;
    private final long conversions;
}
