package me.nullicorn.ooze.convert;

import java.io.IOException;

/**
 * Thrown by a {@link Codec} to indicate that an input value was malformed, corrupted, or otherwise
 * unusable.
 *
 * @author Nullicorn
 */
public class MalformedInputException extends IOException {

  public MalformedInputException() {
    super();
  }

  public MalformedInputException(String inputType) {
    super(String.format("Malformed %1s", inputType));
  }

  public MalformedInputException(String inputType, String details) {
    super(String.format("Malformed %1s (%2s)", inputType, details));
  }

  public MalformedInputException(String inputType, Throwable cause) {
    super(String.format("Malformed %1s", inputType), cause);
  }

  public MalformedInputException(String inputType, String details, Throwable cause) {
    super(String.format("Malformed %1s (%2s)", inputType, details), cause);
  }
}
