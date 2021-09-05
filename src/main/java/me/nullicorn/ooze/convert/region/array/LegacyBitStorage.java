package me.nullicorn.ooze.convert.region.array;

import me.nullicorn.ooze.level.BitHelper;

/**
 * A representation of Minecraft's previous format for storing blocks in chunk sections. For the
 * current format, see {@link BitStorage}.
 *
 * @author Nullicorn
 * @see <a href="https://wiki.vg/Chunk_Format#Example_(Old)">Encoding documentation</a>
 */
class LegacyBitStorage extends RegionUIntArray {

  /**
   * A bitmask over however many least-significant bits are specified by the {@link #magnitude()
   * magnitude}.
   */
  private final int valueMask;

  /**
   * See the {@link RegionUIntArray#RegionUIntArray(int, int, long...) superclass constructor} for
   * details.
   */
  public LegacyBitStorage(int length, int magnitude, long... words) {
    super(length, magnitude, words);

    valueMask = BitHelper.createBitMask(magnitude);
  }

  @Override
  protected int getWordsNeeded(int length, int magnitude) {
    int bytesNeeded = BitHelper.bytesNeeded(length * magnitude);
    return (int) Math.ceil((double) bytesNeeded / Long.BYTES);
  }

  @Override
  public int getOrReplace(int index, boolean doReplace, int replacement) {
    final int length = length();
    final int magnitude = magnitude();
    final long[] words = words();

    if (index < 0 || index >= length) {
      throw new ArrayIndexOutOfBoundsException(index);

    } else if (doReplace && (replacement < 0 || replacement > valueMask)) {
      throw new IllegalArgumentException(replacement + " is invalid for magnitude " + magnitude);
    }

    int bitIndex = index * magnitude;
    int bitOffset = bitIndex % Long.SIZE;
    int wordIndex = bitIndex / Long.SIZE;

    int existingValue = 0;
    int mask = valueMask;

    // Keep taking longs until we've read/replaced each bit in the value.
    int totalBitsSeen = 0;
    while (totalBitsSeen < magnitude) {
      // Get the value's bits in the current word.
      existingValue |= (mask & (words[wordIndex] >>> bitOffset)) << totalBitsSeen;

      if (doReplace) {
        // Replace the existing bits for the value with the replacement's. This is done by negating
        // the mask (so no other values are affected) and combining it with the value itself.
        words[wordIndex] &= ~(mask << bitOffset) | (replacement << bitOffset);
      }

      // Determine how many of the value's bits we read/replaced from the current word.
      int bitsSeen = Math.min(magnitude - totalBitsSeen, Long.SIZE - bitOffset);
      totalBitsSeen += bitsSeen;

      // If the value has more bits in the next word...
      if (totalBitsSeen < magnitude) {
        wordIndex++;

        // Shrink the mask & replacement so that they only cover unread/un-replaced bits.
        mask >>>= bitsSeen;
        replacement >>>= bitsSeen;
      }
    }

    return existingValue;
  }
}
