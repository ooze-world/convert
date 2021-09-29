package me.nullicorn.ooze.convert.region.storage;

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
   * See the {@link RegionUIntArray#RegionUIntArray(int, int, long[]) superclass constructor} for
   * details.
   */
  LegacyBitStorage(int length, int magnitude, long[] words) {
    super(length, magnitude, words);

    valueMask = BitHelper.createBitMask(magnitude);
  }

  @Override
  protected int getWordsNeeded(int length, int magnitude) {
    if (magnitude == 0) {
      return 0;
    }
    int bytesNeeded = BitHelper.bytesNeeded(length * magnitude);
    return (int) Math.ceil((double) bytesNeeded / Long.BYTES);
  }

  @Override
  protected int getOrReplace(int index, boolean doReplace, int replacement) {
    // If magnitude == 0, every value is also 0.
    if (magnitude == 0) {
      return 0;
    }

    // Only bitIndex needs to be a long because it's value could potentially overflow an int.
    long bitIndex = (long) index * magnitude;
    int bitOffset = (int) (bitIndex % Long.SIZE);
    int wordIndex = (int) (bitIndex / Long.SIZE);

    int prevValue = 0;
    long mask = valueMask;

    // Keep taking longs until we've seen/replaced each bit in the value.
    int bitsSeen = 0;
    while (bitsSeen < magnitude) {
      long word = words[wordIndex];

      // Get the value's bits in the current word.
      prevValue |= (mask & (word >>> bitOffset)) << bitsSeen;

      if (doReplace) {
        // Clear the existing value's bits, then insert the new one's.
        word &= ~(mask << bitOffset);
        word |= ((long) replacement << bitOffset);
        words[wordIndex] = word;
      }

      // Determine how many of the value's bits we just read/replaced from the current word.
      //  - magnitude - bitsSeen  == How many bits we still *needed* to see.
      //  - Long.SIZE - bitOffset == How many bits we could've possibly seen from the word.
      // Whichever is lower is the correct amount.
      int bitsJustSeen = Math.min(magnitude - bitsSeen, Long.SIZE - bitOffset);
      bitsSeen += bitsJustSeen;

      // Move onto the next word (and start from the rightmost bit, aka the LSB).
      wordIndex++;
      bitOffset = 0;

      // Shrink the mask & replacement so that they only cover unread/un-replaced bits.
      mask >>>= bitsJustSeen;
      replacement >>>= bitsJustSeen;
    }

    return prevValue;
  }
}
