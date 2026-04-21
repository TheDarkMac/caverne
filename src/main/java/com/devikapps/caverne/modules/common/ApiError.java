package com.devikapps.caverne.modules.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int code, String message, String traceId, Instant timestamp) {

  public ApiError(int code, String message) {
    this(code, message, null, null);
  }
}
