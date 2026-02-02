package com.astro.backend.ResponseDTO;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemedialsResponse {
    private List<Remedy> gemstones;
    private List<Remedy> rudraksha;
    private List<Remedy> mantras;
    private List<Remedy> colors;
    private List<Remedy> days;
    private List<Remedy> directions;
    private String overallAdvice;
}
