package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StatEntry {

    private final String label;
    private final long count;
    private final double percentage;
}
