package me.nullicorn.ooze.convert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;

/**
 * @author Nullicorn
 */
public abstract class VersionedCodecTests {

  protected static final int MIN_VERSION               = 99;
  private static final   int TRAILING_VERSIONS_TO_TEST = 1000;

  /**
   * Provides a function that acts as the codec's constructor, given an {@code int} data-version.
   *
   * @return a function that calls the codec's constructor with the specified data-version.
   */
  protected abstract IntConsumer getVersionedConstructor();

  /**
   * Indicates which data-versions the codec's {@link #getVersionedConstructor() constructor} should
   * accept without throwing an exception.
   * <p><br>
   * If the minimum version is less than the earliest one possible ({@code 99}), then it is
   * automatically rounded up to that version. This allows {@code 0} to be used as a placeholder for
   * tags that have always existed.
   * <p>
   * If the tag is presently in use, {@link Integer#MAX_VALUE} can be used as a placeholder for the
   * maximum value. In that case, only some versions will be tested beyond the minimum, rather than
   * testing the whole {@code int} range.
   *
   * @return an array of two {@code int}s. The first integer indicates the minimum allowed version,
   * and the second integer indicates the maximum allowed version, both being inclusive.
   */
  protected abstract int[] getAcceptableVersionRange();

  @Test
  void constructor_acceptsOnlyValidVersions() {
    // Fetch & validate test parameters.
    IntConsumer constructor = getVersionedConstructor();
    int[] acceptedVersionRange = getAcceptableVersionRange();
    if (constructor == null) {
      throw new IllegalArgumentException("getVersionedConstructor() returned null");
    } else if (acceptedVersionRange == null || acceptedVersionRange.length != 2) {
      throw new IllegalArgumentException("Version range must be two integers, not " +
                                         Arrays.toString(acceptedVersionRange));
    }

    int minVersion = Math.max(acceptedVersionRange[0], MIN_VERSION);
    int maxVersion = acceptedVersionRange[1];
    if (maxVersion < minVersion) {
      throw new IllegalArgumentException("max < min version: " + maxVersion + ", " + minVersion);
    }

    // Test all in-between versions to make sure they work,
    // unless maxVersion is as high as possible. In that case,
    // only test *some* in-between version.
    int maxVersionToTest = (maxVersion == Integer.MAX_VALUE)
        ? minVersion + TRAILING_VERSIONS_TO_TEST
        : maxVersion;
    for (int v = minVersion; v <= maxVersionToTest; v++) {
      assertValidVersionSucceeds(v, constructor);
    }

    // Test lower versions (if applicable) to make sure they fail.
    for (int v = MIN_VERSION; v < minVersion; v++) {
      assertInvalidVersionFails(v, constructor);
    }

    // Test higher versions (if applicable) to make sure they fail.
    if (maxVersion != Integer.MAX_VALUE) {
      for (int v = maxVersion + 1; v <= maxVersion + TRAILING_VERSIONS_TO_TEST; v++) {
        assertInvalidVersionFails(v, constructor);
      }
    }
  }

  /**
   * Asserts that the provided {@code constructor} does not throw an exception when provided with a
   * specific {@code dataVersion}.
   */
  private static void assertValidVersionSucceeds(int dataVersion, IntConsumer constructor) {
    assertDoesNotThrow(
        () -> constructor.accept(dataVersion),
        "Constructor rejects valid version: " + dataVersion);
  }

  /**
   * Asserts that the provided {@code constructor} throws an {@link IllegalArgumentException} when
   * provided with a specific {@code dataVersion}.
   */
  private static void assertInvalidVersionFails(int dataVersion, IntConsumer constructor) {
    assertThrows(
        IllegalArgumentException.class,
        () -> constructor.accept(dataVersion),
        "Constructor accepted invalid version: " + dataVersion);
  }
}
