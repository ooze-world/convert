package me.nullicorn.ooze.convert.region.array;

import me.nullicorn.ooze.level.BitHelper;

/**
 * A representation of Minecraft's format for storing blocks in chunk sections.
 *
 * @author Nullicorn
 * @see <a href="https://wiki.vg/Chunk_Format#Example_(New)">Encoding documentation</a>
 */
class BitStorage extends RegionUIntArray {

  /**
   * @param magnitude The number of bits used to represent each uint.
   * @return the number of uints that can be stored in a single 64-bit word ({@code long})..
   */
  private static int valuesPerWord(int magnitude) {
    return Long.SIZE / magnitude;
  }

  /**
   * A bitmask over however many least-significant bits are specified by the {@link #magnitude()
   * magnitude}.
   */
  private final int valueMask;

  /**
   * The number of uints that can be held within each 64-bit {@link #words() words}. Equivalent to
   * {@code Long.SIZE / magnitude}.
   */
  private final int valuesPerWord;

  /**
   * See the {@link RegionUIntArray#RegionUIntArray(int, int, long...) superclass constructor} for
   * details.
   */
  public BitStorage(int length, int magnitude, long... words) {
    super(length, magnitude, words);

    valueMask = BitHelper.createBitMask(magnitude);
    valuesPerWord = valuesPerWord(magnitude);
  }

  @Override
  protected int getWordsNeeded(int length, int magnitude) {
    return (int) Math.ceil(length * magnitude / (double) valuesPerWord(magnitude));
  }

  @Override
  protected int getOrReplace(int index, boolean doReplace, int replacement) {
    // Only call the getters once.
    final int length = length();
    final int magnitude = magnitude();
    final long[] words = words();

    // Validate args.
    if (index < 0 || index >= length) {
      throw new ArrayIndexOutOfBoundsException(index);

    } else if (doReplace && BitHelper.widthInBits(replacement) > magnitude) {
      throw new IllegalArgumentException(replacement + " is not valid for magnitude: " + magnitude);
    }

    // Determine which word the value is in, and how many bits from the right (LSB) it is.
    int wordIndex = index / valuesPerWord;
    int bitOffset = index % valuesPerWord * magnitude;

    // Get (and replace, if necessary) the value.
    long existingValue = valueMask & (words[wordIndex] >>> bitOffset);
    if (doReplace) {
      words[wordIndex] &= ~(valueMask << bitOffset) | (replacement << bitOffset);
    }

    return (int) existingValue;
  }
}
