package com.astro.backend.apiResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ApiResponse<T> {
  T response;
  private Boolean status;
  private String error_code;
  private String message;
}