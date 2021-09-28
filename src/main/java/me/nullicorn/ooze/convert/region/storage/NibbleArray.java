package me.nullicorn.ooze.convert.region.storage;

import java.util.Arrays;
import java.util.Objects;
import me.nullicorn.ooze.level.BitHelper;

/**
 * An array of unsigned 4-bit integers called "nibbles". This type of array was used by older
 * versions of Minecraft to store block data and light, among other things.
 *
 * @author Nullicorn
 */
public class NibbleArray {

  private static final int NIBBLE_SIZE = Byte.SIZE / 2;
  private static final int NIBBLE_MASK = BitHelper.createBitMask(NIBBLE_SIZE);

  /**
   * Internal validator that prevents equals() and hashCode() from breaking if the last byte has set
   * bits that aren't used by the array.
   */
  private static void clearExtraneousBits(byte[] words, int length) {
    // If length is odd...  (meaning the last nibble is alone in a byte)
    if ((length % 2) != 0) {
      // Clear the upper 4 bits of the last byte.  If they were set for whatever reason, it  would
      // mess up equals() and hashCode().
      words[words.length - 1] &= NIBBLE_MASK;
    }
  }

  private final byte[] words;
  private final int    length;

  public NibbleArray(int length) {
    this(length, (byte[]) null);
  }

  /**
   * @param length the number of elements that the array will have.
   * @param words  (optional) the array's initial contents, using the format described {@link
   *               #toByteArray() here}. If present, this array must contain {@code ceil(length /
   *               2)} bytes, where {@code length} is the value of the first parameter.
   * @throws NegativeArraySizeException if the supplied {@code length} is less than {@code 0}.
   * @throws IllegalArgumentException   if a {@code words} array is supplied, but does not contain
   *                                    the correct number of bytes.
   */
  public NibbleArray(int length, byte... words) {
    if (length < 0) {
      throw new NegativeArraySizeException("length cannot be negative: " + length);
    }

    int bytesNeeded = (int) Math.ceil(length / 2d);

    if (words == null) {
      words = new byte[bytesNeeded];
    } else if (words.length != bytesNeeded) {
      throw new IllegalArgumentException("words array has wrong length: " + words.length);
    } else {
      words = words.clone();
      clearExtraneousBits(words, length);
    }

    this.length = length;
    this.words = words;
  }

  /**
   * The number of values in the array.
   *
   * @return the array's length.
   */
  public int length() {
    return length;
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

    byte octet = words[index / 2];
    if ((index & 1) == 1) {
      octet >>>= NIBBLE_SIZE;
    }
    return octet & NIBBLE_MASK;
  }

  /**
   * Replaces a value in the array at a given index.
   *
   * @param index The zero-based offset of the value to replace, from the start of the array.
   * @param value the value to replace with. Only the lowest 4 bits of this value will be used
   *              ({@code value & 0b1111}).
   * @return the previous value at the index (aka the value that was replaced).
   * @throws ArrayIndexOutOfBoundsException if the {@code index} is less than {@code 0} or if it
   *                                        exceeds the array's highest index ({@code length() -
   *                                        1}).
   */
  public int set(int index, int value) {
    if (index < 0 || index >= length) {
      throw new ArrayIndexOutOfBoundsException(index);
    }

    int prevValue;
    int wordIndex = index / 2;
    value &= NIBBLE_MASK;

    byte word = words[wordIndex];
    if ((index & 1) == 0) {
      // 1. Get the old value.
      prevValue = word & NIBBLE_MASK;
      // 2. Clear the old value's bits.
      word &= ~NIBBLE_MASK;
      // 3. Insert the new value's bits.
      word |= value;
    } else {
      // 1.
      prevValue = word >>> NIBBLE_SIZE;
      // 2.
      word &= NIBBLE_MASK;
      // 3.
      word |= value << NIBBLE_SIZE;
    }
    words[wordIndex] = word;

    return prevValue;
  }

  /**
   * Returns a copy of the array's 8-bit representation, where each octet ({@code byte}) contains
   * two 4-bit nibbles.
   * <p><br>
   * <b>Notes</b>
   * <ul>
   *   <li>
   *     Given a nibble's index "i" in this {@code NibbleArray}, the index of its octet in the
   *     returned array is {@code i / 2}.
   *   </li>
   *   <li>
   *     If {@code i} is an even number ({@code (i & 1) == 0}), then the nibble can be retrieved by
   *     masking the octet's 4 least significant bits: {@code bytes[i] & 0b1111}
   *   </li>
   *   <li>
   *     Otherwise, the nibble is the octet's 4 most significant bits, and can be retrieved via:
   *     {@code bytes[i] >>> 4}
   *   </li>
   * </ul>
   *
   * @return the array's binary format.
   */
  public byte[] toByteArray() {
    return words.clone();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NibbleArray that = (NibbleArray) o;
    return length == that.length &&
           Arrays.equals(words, that.words);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(length);
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
