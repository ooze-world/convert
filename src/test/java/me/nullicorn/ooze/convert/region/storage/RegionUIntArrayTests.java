package me.nullicorn.ooze.convert.region.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import me.nullicorn.ooze.level.BitHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Nullicorn
 */
abstract class RegionUIntArrayTests {

  // Fall-back values used if not specified in any of the test methods.
  // Initialized in setUp().
  protected int length;
  protected int magnitude;
  protected int dataVersion;

  /**
   * @return a data version that can be passed to one of the {@link RegionUIntArray#from(int, int,
   * int) RegionUIntArray.from()} factories to create a {@code RegionUIntArray} for the target
   * encoding.
   */
  protected abstract int getValidDataVersion();

  /**
   * @return an array of 64-bit words that should match the one produced by the target
   * RegionUIntArray implementation if every array value were initialized to {@code 0}.
   */
  protected abstract long[] createEmptyWords(int length, int magnitude);

  /**
   * Performs the "set" operation on the provided {@code words} array, as if it were a
   * RegionUIntArray using the target encoding.
   *
   * @param magnitude The number of bits that should be used to store the value in its word(s).
   */
  protected abstract void set(int index, int value, long[] words, int magnitude);

  @BeforeEach
  void setUp() {
    length = 4096;
    magnitude = 5;
    dataVersion = getValidDataVersion();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, -8, -64, -128, -4096})
  void factory_shouldRejectNegativeLengths(int length) {
    // Creates the word array using abs(length), otherwise this might throw prematurely.
    long[] words = createEmptyWords(Math.abs(length), magnitude);

    // Test with & without words provided.
    assertThrows(
        NegativeArraySizeException.class,
        () -> RegionUIntArray.from(length, magnitude, dataVersion),
        "Array factory did not throw exception for negative length"
    );
    assertThrows(
        NegativeArraySizeException.class,
        () -> RegionUIntArray.from(length, magnitude, words, dataVersion),
        "Array factory (words provided) did not throw exception for negative length"
    );
  }

  @ParameterizedTest
  @ValueSource(ints = {-33, -32, -31, -2, -1, 0, 32, 33})
  void factory_shouldRejectInvalidMagnitudes(int magnitude) {
    // Creates the word array using abs(magnitude), otherwise this might throw prematurely.
    long[] words = createEmptyWords(length, Math.abs(magnitude));

    // Test with & without words provided.
    assertThrows(
        IllegalArgumentException.class,
        () -> RegionUIntArray.from(length, magnitude, dataVersion),
        "Array factory did not throw exception for invalid magnitude"
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> RegionUIntArray.from(length, magnitude, words, dataVersion),
        "Array factory (words provided) did not throw exception for invalid magnitude"
    );
  }

  @ParameterizedTest
  @MethodSource("provider_lengths")
  void factory_shouldAcceptValidLengths(int length) {
    long[] words = createEmptyWords(length, magnitude);

    assertDoesNotThrow(
        () -> RegionUIntArray.from(length, magnitude, dataVersion),
        "Array factory threw exception when given a valid length"
    );
    assertDoesNotThrow(
        () -> RegionUIntArray.from(length, magnitude, words, dataVersion),
        "Array factory (words provided) threw exception when given a valid length"
    );
  }

  @ParameterizedTest
  @MethodSource("provider_magnitudes")
  void factory_shouldAcceptValidMagnitudes(int magnitude) {
    long[] words = createEmptyWords(length, magnitude);

    assertDoesNotThrow(
        () -> RegionUIntArray.from(length, magnitude, dataVersion),
        "Array factory threw exception when given a valid magnitude"
    );
    assertDoesNotThrow(
        () -> RegionUIntArray.from(length, magnitude, words, dataVersion),
        "Array factory (words provided) threw exception when given a valid magnitude"
    );
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void length_matchesFactoryInput(int length, int magnitude) {
    RegionUIntArray actual = RegionUIntArray.from(length, magnitude, dataVersion);
    assertEquals(length, actual.length(), "Incorrect array length");
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void magnitude_matchesFactoryInput(int length, int magnitude) {
    RegionUIntArray actual = RegionUIntArray.from(length, magnitude, dataVersion);
    assertEquals(magnitude, actual.magnitude(), "Incorrect array magnitude");
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void words_hasExpectedLength(int length, int magnitude) {
    long[] expected = createEmptyWords(length, magnitude);
    long[] actual = RegionUIntArray.from(length, magnitude, dataVersion).words();

    assertEquals(expected.length, actual.length, "Incorrect number of words");
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void get_returnsSetValue(int length, int magnitude) throws NoSuchAlgorithmException {
    long[] words = createEmptyWords(length, magnitude);

    int[] values = provider_arrayValues(length, magnitude);
    for (int i = 0; i < values.length; i++) {
      set(i, values[i], words, magnitude);
    }

    RegionUIntArray actual = RegionUIntArray.from(length, magnitude, words, dataVersion);
    for (int i = 0; i < values.length; i++) {
      assertEquals(values[i], actual.get(i), "Incorrect value at index " + i);
    }
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void set_flipsCorrectBits(int length, int magnitude) throws NoSuchAlgorithmException {
    long[] expected = createEmptyWords(length, magnitude);
    RegionUIntArray actual = RegionUIntArray.from(length, magnitude, dataVersion);

    int[] values = provider_arrayValues(length, magnitude);
    for (int i = 0; i < values.length; i++) {
      set(i, values[i], expected, magnitude);
      actual.set(i, values[i]);
    }

    assertArrayEquals(expected, actual.words(), "Array set() flipped incorrect bits");
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void set_replacesExistingValues(int length, int magnitude) throws NoSuchAlgorithmException {
    RegionUIntArray actual = RegionUIntArray.from(length, magnitude, dataVersion);
    int[] values = provider_arrayValues(length, magnitude);

    // Fill the region array with the plain array's values.
    for (int i = 0; i < values.length; i++) {
      actual.set(i, values[i]);
    }

    // Reverse the values in the plain array.
    for (int i = 0; i < values.length / 2; i++) {
      int temp = values[i];
      int oppositeIndex = values.length - 1 - i;

      values[i] = values[oppositeIndex];
      values[oppositeIndex] = temp;
    }

    // Replace each value in the region array with the reversed plain array value.
    for (int i = 0; i < values.length; i++) {
      actual.set(i, values[i]);
    }

    // Assert that the replacement succeeded.
    for (int i = 0; i < values.length; i++) {
      assertEquals(values[i], actual.get(i));
    }
  }

  /**
   * Provides valid {@link RegionUIntArray#length() length} values for use in testing.
   */
  static int[] provider_lengths() {
    return new int[]{0, 1, 2, 5, 10, 32, 1024, 4096};
  }

  /**
   * Provides valid {@link RegionUIntArray#magnitude() magnitude} values for use in testing.
   */
  static int[] provider_magnitudes() {
    // Fill an array with magnitudes 1 through 30.
    int[] magnitudes = new int[Integer.SIZE - 2];
    for (int m = 1; m < Integer.SIZE - 1; m++) {
      magnitudes[m - 1] = m;
    }
    return magnitudes;
  }

  /**
   * Provides sample uints that can be used to populate test arrays. The returned array will have
   * {@code length} number of elements, each using up {@code magnitude} number of bits at most.
   */
  static int[] provider_arrayValues(int length, int magnitude) throws NoSuchAlgorithmException {
    int mask = BitHelper.createBitMask(magnitude);
    MessageDigest md5 = MessageDigest.getInstance("MD5");

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
  static Stream<Arguments> provider_lengthsAndMagnitudes() {
    Stream.Builder<Arguments> builder = Stream.builder();

    // Combine each valid length and magnitude as an argument pair.
    for (int length : provider_lengths()) {
      for (int magnitude : provider_magnitudes()) {
        builder.accept(arguments(length, magnitude));
      }
    }

    return builder.build();
  }
}