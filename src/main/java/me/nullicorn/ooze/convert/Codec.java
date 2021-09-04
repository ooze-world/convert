package me.nullicorn.ooze.convert;

import java.io.IOException;

/**
 * A tool for converting objects to a more vague representation of themselves, as well as converting
 * back to the original type.
 *
 * @param <T> The main type (higher-level; more defined).
 * @param <E> The encoded type (lower-level; more ambiguous; e.g. binary representation).
 * @author Nullicorn
 */
public interface Codec<T, E> {

  /**
   * @param value The non-null object to convert.
   * @return a lower-level representation of the input {@code value}.
   * @throws IOException              if the input {@code value} cannot be converted for whatever
   *                                  reason.
   * @throws IllegalArgumentException if the input {@code value} is {@code null}.
   */
  E encode(T value) throws IOException;

  /**
   * @param encoded The non-null value to convert.
   * @return a higher-level representation of the {@code encoded} value.
   * @throws IOException              if the {@code encoded} value does not use the codec's expected
   *                                  format, if it is missing required information, or if it cannot
   *                                  be decoded for some other reason.
   * @throws IllegalArgumentException if the {@code encoded} value is {@code null}.
   */
  T decode(E encoded) throws IOException;
}