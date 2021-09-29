package me.nullicorn.ooze.convert.region.storage;

import java.util.Arrays;
import java.util.Objects;
import me.nullicorn.ooze.level.BitHelper;

/**
 * An array of unsigned integers, packed into 64-bit words (stored as a {@code long[]}). The array
 * can only store integers up to a certain magnitude (width in bits), determined on construction.
 *
 * @author Nullicorn
 * @see LegacyBitStorage Current encoding
 * @see BitStorage Old encoding
 */
public abstract class RegionUIntArray {

  /**
   * The data version when values were no longer able to be split across multiple longs.
   */
  private static final int MODERN_ENCODING_VERSION = 2527;

  /**
   * A factory for creating {@code RegionUIntArray}s compatible with a specific version of
   * Minecraft's level format.
   *
   * @param length      The number of uints that the resulting array will be able to hold.
   * @param magnitude   The number of bits that will be used to represent each uint.
   * @param dataVersion The Minecraft world version that the returned array should be compatible
   *                    with.
   * @return a region-compatible array.
   * @throws NegativeArraySizeException if the supplied {@code length} is a negative number.
   * @throws IllegalArgumentException   if the supplied {@code magnitude} is a negative number, or
   *                                    if {@code magnitude >=} {@link Integer#SIZE 32}.
   */
  public static RegionUIntArray from(int length, int magnitude, int dataVersion) {
    return dataVersion >= MODERN_ENCODING_VERSION
        ? new BitStorage(length, magnitude, null)
        : new LegacyBitStorage(length, magnitude, null);
  }

  /**
   * A factory for creating pre-populated {@code RegionUIntArray}s compatible with a specific
   * version of Minecraft's level format.
   *
   * @param length      The number of uints that the resulting array will be able to hold.
   * @param magnitude   The number of bits that will be used to represent each uint.
   * @param words       The array's binary contents, packed into 64-bit words ({@code long}s).
   * @param dataVersion The Minecraft world version that the returned array should be compatible
   *                    with.
   * @return a region-compatible array.
   * @throws NegativeArraySizeException if the supplied {@code length} is a negative number.
   * @throws IllegalArgumentException   if the supplied {@code magnitude} is a negative number, or
   *                                    if {@code magnitude >=} {@link Integer#SIZE 32}.
   * @throws IllegalArgumentException   if the supplied {@code magnitude} is greater than or equal
   *                                    to {@link Integer#SIZE 32}.
   * @throws IllegalArgumentException   if the supplied {@code words} array is null.
   * @throws IllegalArgumentException   if the supplied {@code words} array is the incorrect length
   *                                    based on the other supplied arguments.
   */
  public static RegionUIntArray from(int length, int magnitude, long[] words, int dataVersion) {
    if (words == null) {
      throw new IllegalArgumentException("words array cannot be null");
    }
    return dataVersion >= MODERN_ENCODING_VERSION
        ? new BitStorage(length, magnitude, words)
        : new LegacyBitStorage(length, magnitude, words);
  }

  protected final long[] words;
  protected final int    length;
  protected final int    magnitude;

  /**
   * Creates an array with the provided {@code length} and {@code magnitude}, as well as all of the
   * packed values from the {@code words} array (order preserved). If the {@code words} array is
   * empty ({@code words.length == 0}), then the resulting {@code RegionUIntArray} will have all
   * values initialized to {@code 0}. Otherwise, it must be non-null and the correct length, given
   * the {@code length} and {@code magnitude}.
   *
   * @param length    The number of packed, unsigned values within the {@code words} array (not the
   *                  number of {@code longs}).
   * @param magnitude The number of bits used to represent each value in the array.
   * @param words     An array of 64-bit words, each containing at least {@code 1} packed uint,
   *                  represented using however many bits are specified via {@code magnitude}.
   * @throws NegativeArraySizeException if {@code length} is a negative number.
   * @throws IllegalArgumentException   if {@code magnitude} is less than {@code 1}.
   * @throws IllegalArgumentException   if it is {@code magnitude} is grater than or equal to {@link
   *                                    Integer#SIZE 32}.
   * @throws IllegalArgumentException   if the supplied {@code words} array has an unexpected
   *                                    length, given the other arguments provided.
   */
  RegionUIntArray(int length, int magnitude, long[] words) {
    if (length < 0) {
      throw new NegativeArraySizeException("length cannot be negative: " + length);
    } else if (magnitude < 0 || magnitude >= Integer.SIZE) {
      throw new IllegalArgumentException("magnitude must be in range [0, 32): " + magnitude);
    }

    int wordsNeeded = getWordsNeeded(length, magnitude);
    if (words == null) {
      words = new long[wordsNeeded];
    } else if (words.length != wordsNeeded) {
      throw new IllegalArgumentException(wordsNeeded + " words required, not " + words.length);
    }

    this.words = words.clone();
    this.length = length;
    this.magnitude = magnitude;
  }

  /**
   * Called on construction to determine how long the {@link #words() words} array should be.
   *
   * @param length    A positive integer indicating the array's required capacity.
   * @param magnitude A positive integer from 0 to 31
   * @return the number of {@code long}s needed for an array with the given {@code length} and
   * {@code magnitude}. If {@code magnitude == 0}, the return value must be {@code 0}.
   * @see #length()
   * @see #magnitude()
   */
  protected abstract int getWordsNeeded(int length, int magnitude);

  /**
   * Called by the default implementation of {@link #get(int) get} and {@link #set(int, int) set}.
   *
   * @param index       The zero-based offset of the value to return (and possibly replace).
   * @param doReplace   Whether or not the replace operation should be performed.
   * @param replacement The value to replace the existing one with. This is ignored if {@code
   *                    doReplace == false}.
   * @return the value at the index. If {@code doReplace == true}, then the previous value is
   * returned.
   * @throws ArrayIndexOutOfBoundsException if the {@code index} is negative or exceeds the array's
   *                                        highest index ({@code length() - 1}).
   * @throws IllegalArgumentException       if {@code doReplace == true} but the {@code
   *                                        replacement}'s width (in bits) exceeds the array's
   *                                        {@link #magnitude() magnitude}.
   */
  protected abstract int getOrReplace(int index, boolean doReplace, int replacement);

  /**
   * @return the number of values in the array.
   */
  public final int length() {
    return length;
  }

  /**
   * @return the number of bits used to hold each value in the array.
   */
  public final int magnitude() {
    return magnitude;
  }

  /**
   * @return a copy of the array's internal storage container. {@code long}s in the returned array
   * may not correspond to values in the array itself, as multiple values may be packed into a
   * single word.
   */
  public final long[] words() {
    return words.clone();
  }

  /**
   * Retrieves a value from the array given its index.
   *
   * @param index The zero-based offset of the value, from the start of the array.
   * @return the value at the index.
   * @throws ArrayIndexOutOfBoundsException if the {@code index} is less than {@code 0} or if it
   *                                        exceeds the array's highest index ({@code length() -
   *                                        1}).
   */
  public int get(int index) {
    if (index < 0 || index >= length) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    return getOrReplace(index, false, -1);
  }

  /**
   * Replaces a value in the array at a given index.
   *
   * @param index The zero-based offset of the value to replace, from the start of the array.
   * @param value the value to replace with.
   * @return the previous value at the index (aka the value that was replaced).
   * @throws ArrayIndexOutOfBoundsException if the {@code index} is less than {@code 0} or if it
   *                                        exceeds the array's highest index ({@code length() -
   *                                        1}).
   * @throws IllegalArgumentException       if the {@code value}'s width (in bits) exceeds the
   *                                        array's {@link #magnitude() magnitude}.
   */
  public int set(int index, int value) {
    if (index < 0 || index >= length) {
      throw new ArrayIndexOutOfBoundsException(index);
    }

    int valueWidth = BitHelper.widthInBits(value);
    if (valueWidth > magnitude) {
      throw new IllegalArgumentException("value " + value + " exceeds magnitude " + magnitude);
    }

    return getOrReplace(index, true, value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RegionUIntArray that = (RegionUIntArray) o;
    return length == that.length &&
           magnitude == that.magnitude &&
           Arrays.equals(words, that.words);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(length, magnitude);
    result = 31 * result + Arrays.hashCode(words);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < length; i++) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append(get(i));
    }
    return sb.append(']').toString();
  }
}
