package me.nullicorn.ooze.convert.region.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import me.nullicorn.ooze.level.BitHelper;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Nullicorn
 */
public abstract class RegionUIntArrayTests {

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
  void factory_shouldThrowIfLengthIsNegative(int length) {
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
  @ValueSource(ints = {-33, -32, -31, -2, -1, 32, 33})
  void factory_shouldThrowIfMagnitudeIsOutOfBounds(int magnitude) {
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
  @MethodSource("provider_lengthsAndMagnitudes")
  void factory_shouldThrowIfWordsArrayIsNull(int length, int magnitude) {
    assertThrows(IllegalArgumentException.class,
        () -> RegionUIntArray.from(length, magnitude, null, dataVersion)
    );
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void factory_shouldThrowIfWordsArrayIsWrongLength(int length, int magnitude) {
    int expectedLength = createEmptyWords(length, magnitude).length;

    int range = 500;
    int min = Math.max(-range, 0);
    int max = expectedLength + range - 1;

    for (int badLength = min; badLength < max; badLength++) {
      if (badLength != expectedLength) {
        long[] wordsWithWrongLength = new long[badLength];

        assertThrows(IllegalArgumentException.class,
            () -> RegionUIntArray.from(length, magnitude, wordsWithWrongLength, dataVersion),
            String.format("Bad length didn't throw: %d (should be %d)", badLength, expectedLength)
        );
      }
    }
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void length_shouldMatchFactoryInput(int length, int magnitude) {
    RegionUIntArray actual = RegionUIntArray.from(length, magnitude, dataVersion);
    assertEquals(length, actual.length(), "Incorrect array length");
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void magnitude_shouldMatchFactoryInput(int length, int magnitude) {
    RegionUIntArray actual = RegionUIntArray.from(length, magnitude, dataVersion);
    assertEquals(magnitude, actual.magnitude(), "Incorrect array magnitude");
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void words_shouldHaveExpectedLength(int length, int magnitude) {
    long[] expected = createEmptyWords(length, magnitude);
    long[] actual = RegionUIntArray.from(length, magnitude, dataVersion).words();

    assertEquals(expected.length, actual.length, "Incorrect number of words");
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void get_shouldThrowIfIndexIsOutOfBounds(int length, int magnitude) {
    RegionUIntArray array = RegionUIntArray.from(length, magnitude, dataVersion);

    int range = 500;
    int min = -range;
    int max = array.length() + range - 1;

    for (int i = min; i < max; i++) {
      if (i < 0 || i >= array.length()) {
        int badIndex = i; // Required so index can be used inside the lambda.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(badIndex));
      }
    }
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void get_shouldReturnInputValues(int length, int magnitude) {
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
  void set_shouldFlipCorrectBits(int length, int magnitude) {
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
  void set_shouldReplaceExistingValues(int length, int magnitude) {
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

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void set_shouldThrowIfIndexIsOutOfBounds(int length, int magnitude) {
    RegionUIntArray array = RegionUIntArray.from(length, magnitude, dataVersion);

    int range = 500;
    int min = -range;
    int max = array.length() + range - 1;

    for (int i = min; i < max; i++) {
      if (i < 0 || i >= array.length()) {
        int badIndex = i; // Required so index can be used inside the lambda.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.set(badIndex, 0));
      }
    }
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void set_shouldThrowIfValueIsWiderThanMagnitude(int length, int magnitude) {
    RegionUIntArray array = RegionUIntArray.from(length, magnitude, dataVersion);

    int interval = Math.max(1, length / 100);
    for (int i = 0; i < array.length(); i += interval) {
      // Test all magnitudes greater than the expected one.
      for (int m = array.magnitude() + 1; m < Integer.SIZE; m++) {
        int index = i;
        int valueThatIsTooWide = BitHelper.createBitMask(m);

        assertThrows(IllegalArgumentException.class,
            () -> array.set(index, valueThatIsTooWide),
            String.format("Nothing thrown when magnitude=%1d, i=%2d", m, index)
        );
      }
    }
  }

  @Test
  void equals_hashCode_shouldFollowContract() {
    RegionUIntArray sample = RegionUIntArray.from(length, magnitude, dataVersion);
    EqualsVerifier
        .forClass(sample.getClass())
        .withOnlyTheseFields("words", "length", "magnitude")
        .usingGetClass()
        .verify();
  }

  @ParameterizedTest
  @MethodSource("provider_lengthsAndMagnitudes")
  void toString_shouldNotThrow(int length, int magnitude) {
    RegionUIntArray array = RegionUIntArray.from(length, magnitude, dataVersion);

    assertDoesNotThrow(array::toString);
  }


  /**
   * @see RegionUIntArrayTestHelper#provider_arrayValues(int, int)
   */
  static int[] provider_arrayValues(int length, int magnitude) {
    return RegionUIntArrayTestHelper.provider_arrayValues(length, magnitude);
  }

  /**
   * @see RegionUIntArrayTestHelper#provider_lengthsAndMagnitudes()
   */
  static Stream<Arguments> provider_lengthsAndMagnitudes() {
    return RegionUIntArrayTestHelper.provider_lengthsAndMagnitudes();
  }
}