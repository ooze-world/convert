package me.nullicorn.ooze.convert.region.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Nullicorn
 */
class NibbleArrayTests {

  @ParameterizedTest
  @ValueSource(ints = {-1, -2, -3, -10, -20, -50, -100, -1000})
  void constructor_shouldThrowIfLengthIsNegative(int length) {
    assertThrows(NegativeArraySizeException.class, () -> new NibbleArray(length));
  }

  @ParameterizedTest
  @MethodSource("provider_validArrayLengths")
  void constructor_shouldThrowIfWordsArrayHasIncorrectLength(int length) {
    int correctLengthInBytes = (int) Math.ceil(length / 2d);

    // Check for  500 lengths above & below the expected one.
    // 500 is just arbitrary.
    int range = 500;
    int min = Math.max(0, -range);
    int max = length + range - 1;

    for (int badLength = min; badLength < max; badLength++) {
      if (badLength != correctLengthInBytes) {
        byte[] badWordArray = new byte[badLength];
        assertThrows(IllegalArgumentException.class, () -> new NibbleArray(length, badWordArray));
      }
    }
  }

  @ParameterizedTest
  @MethodSource("provider_validArrayLengths")
  void length_shouldMatchConstructorInput(int length) {
    int correctLengthInBytes = (int) Math.ceil(length / 2d);

    NibbleArray withoutBytes = new NibbleArray(length);
    NibbleArray withBytes = new NibbleArray(length, new byte[correctLengthInBytes]);

    assertEquals(length, withoutBytes.length());
    assertEquals(length, withBytes.length());
  }

  @ParameterizedTest
  @MethodSource("provider_validArrayLengths")
  void set_shouldThrowIfIndexIsOutOfBounds(int length) {
    NibbleArray array = new NibbleArray(length);

    int range = 500;
    int min = -range;
    int max = length + range - 1;

    for (int i = min; i < max; i++) {
      if (i < 0 || i >= array.length()) {
        int badIndex = i;
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.set(badIndex, 0));
      }
    }
  }

  @ParameterizedTest
  @MethodSource("provider_valuesThatMayBeMoreThanFourBits")
  void set_shouldFlipCorrectBits(int[] values) {
    NibbleArray array = new NibbleArray(values.length);
    byte[] expectedWords = new byte[(int) Math.ceil(values.length / 2d)];

    for (int i = 0; i < values.length; i++) {
      int value = values[i];
      array.set(i, value);
      // Mirror the change on our byte array.
      setNibbleInByteArray(expectedWords, i, value);
      // Check that it works immediately after inserting.
      assertArrayEquals(expectedWords, array.toByteArray());
    }

    // Check that it works after all values have been inserted.
    assertArrayEquals(expectedWords, array.toByteArray());
  }

  @ParameterizedTest
  @MethodSource("provider_validArrayLengths")
  void get_shouldThrowIfIndexIsOutOfBounds(int length) {
    NibbleArray array = new NibbleArray(length);

    int range = 500;
    int min = -range;
    int max = length + range - 1;

    for (int i = min; i < max; i++) {
      if (i < 0 || i >= array.length()) {
        int badIndex = i;
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(badIndex));
      }
    }
  }

  @ParameterizedTest
  @MethodSource("provider_valuesThatMayBeMoreThanFourBits")
  void get_shouldReturnLowestFourBitsOfSetValue(int[] values) {
    NibbleArray array = new NibbleArray(values.length);

    for (int i = 0; i < values.length; i++) {
      int value = values[i];
      array.set(i, value);
      // Check that it works immediately after inserting.
      assertEquals(value & 0b1111, array.get(i));
    }

    for (int i = 0; i < values.length; i++) {
      int value = array.get(i);
      // Check that it also works after all values have been inserted.
      assertEquals(value & 0b1111, array.get(i));
    }
  }

  @ParameterizedTest
  @MethodSource("provider_valuesThatMayBeMoreThanFourBits")
  void toByteArray_shouldConstructEqualArrayWhenUsedInConstructor(int[] values) {
    NibbleArray array = new NibbleArray(values.length);
    for (int i = 0; i < values.length; i++) {
      array.set(i, values[i]);
    }

    int length = array.length();
    byte[] words = array.toByteArray();
    NibbleArray recreation = new NibbleArray(length, words);

    assertEquals(array, recreation);
    assertEquals(array.length(), recreation.length());
    assertArrayEquals(words, recreation.toByteArray());
  }

  @Test
  void equals_hashCode_shouldFollowContract() {
    EqualsVerifier
        .forClass(NibbleArray.class)
        .usingGetClass()
        .verify();
  }

  /**
   * Replicates the expected change to a {@code NibbleArray}'s {@link NibbleArray#toByteArray()
   * words} after using {@link NibbleArray#set(int, int) NibbleArray.set()}.
   */
  private static void setNibbleInByteArray(byte[] words, int index, int replacement) {
    int wordIndex = index / 2;
    replacement &= 0b1111;

    if (index % 2 == 0) {
      words[wordIndex] &= ~0b1111;
      words[wordIndex] |= replacement;
    } else {
      words[wordIndex] &= 0b1111;
      words[wordIndex] |= (replacement << 4);
    }
  }

  /**
   * A MethodSource provider for integers that can be used in the {@code length} field of {@link
   * NibbleArray}'s constructors.
   */
  private static int[] provider_validArrayLengths() {
    return new int[]{0, 2, 10, 15, 35, 2021, 2048, 4096, 42000};
  }

  /**
   * A MethodSource provider for integer arrays, which can be used as sample values for a {@link
   * NibbleArray}. The values in the returned arrays may be larger than a nibble (4 bits), so do
   * {@code value & 0b1111} to get the expected value as a nibble.
   */
  private static int[][] provider_valuesThatMayBeMoreThanFourBits() {
    return new int[][]{
        {15, 0, 14, 1, 13, 2, 12, 3, 11, 4, 10, 5, 9, 6, 8, 7},
        {16, 32, 42, 100, 1234, 2048, 4096, 42000},
        {1900, 1800, 1700, 1600, 1500, 1400, 1300},
        {22, 33, 44, 55, 66, 77, 88, 99, 88, 77}
    };
  }
}
