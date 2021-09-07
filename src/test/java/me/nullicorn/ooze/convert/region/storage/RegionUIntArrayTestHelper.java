package me.nullicorn.ooze.convert.region.storage;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import me.nullicorn.ooze.level.BitHelper;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Helper functions for testing {@link RegionUIntArray} and related classes.
 *
 * @author Nullicorn
 */
public final class RegionUIntArrayTestHelper {

  /**
   * Provides valid {@link RegionUIntArray#length() length} values for use in testing.
   */
  public static int[] provider_lengths() {
    return new int[]{0, 1, 2, 5, 10, 32, 1024, 4096};
  }

  /**
   * Provides valid {@link RegionUIntArray#magnitude() magnitude} values for use in testing.
   */
  public static int[] provider_magnitudes() {
    // Fill an array with magnitudes 0 through 31.
    int[] magnitudes = new int[Integer.SIZE - 1];
    for (int m = 0; m < Integer.SIZE - 1; m++) {
      magnitudes[m] = m;
    }
    return magnitudes;
  }

  /**
   * Provides sample uints that can be used to populate test arrays. The returned array will have
   * {@code length} number of elements, each using up {@code magnitude} number of bits at most.
   */
  public static int[] provider_arrayValues(int length, int magnitude) {
    int mask = BitHelper.createBitMask(magnitude);
    MessageDigest md5;

    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      // If this is thrown during a test, always fail.
      // No reason to put it in method signature.
      throw new RuntimeException(e);
    }

    int[] output = new int[length];

    for (int i = 0; i < length; i++) {
      // Generate some bytes.
      byte[] bytes = new byte[128];
      for (int b = 0; b < bytes.length; b++) {
        bytes[b] = (byte) (i * b * 255 % 100 + magnitude);
      }

      // Hash the byte array.
      byte[] hashedBytes = md5.digest(bytes);

      // Combine the hash's bytes into a single int.
      int value = 0;
      for (int b = 0, j = 0; b < magnitude; b += Byte.SIZE, j++) {
        value |= (hashedBytes[j] << b);
      }

      // Remove extra bits.
      output[i] = value & mask;
    }

    return output;
  }

  /**
   * Combines {@link #provider_lengths()} and {@link #provider_magnitudes()} into an output with all
   * possible combinations of both. In the resulting arguments, the {@code length} comes first,
   * followed by the {@code magnitude}.
   */
  public static Stream<Arguments> provider_lengthsAndMagnitudes() {
    Stream.Builder<Arguments> builder = Stream.builder();

    // Combine each valid length and magnitude as an argument pair.
    for (int length : provider_lengths()) {
      for (int magnitude : provider_magnitudes()) {
        builder.accept(arguments(length, magnitude));
      }
    }

    return builder.build();
  }

  private RegionUIntArrayTestHelper() {
    throw new UnsupportedOperationException(getClass() + " should not be instantiated");
  }

  /* ================================================
   * ======= COPY/PASTE FOR DEPENDENT CLASSES =======
   * ==== (avoids fully-qualifying method names) ====
   * ================================================

    static int[] provider_lengths() {
      return RegionUIntArrayTestHelper.provider_lengths();
    }

    static int[] provider_magnitudes() {
      return RegionUIntArrayTestHelper.provider_magnitudes();
    }

    static int[] provider_arrayValues(int length, int magnitude) {
      return RegionUIntArrayTestHelper.provider_arrayValues(length, magnitude);
    }

    static Stream<Arguments> provider_lengthsAndMagnitudes() {
      return RegionUIntArrayTestHelper.provider_lengthsAndMagnitudes();
    }

   */
}

