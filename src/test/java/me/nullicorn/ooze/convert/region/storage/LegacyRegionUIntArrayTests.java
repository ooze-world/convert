package me.nullicorn.ooze.convert.region.storage;

import me.nullicorn.ooze.level.BitHelper;

/**
 * Tests for the older encoding of RegionUIntArrays. Implemented by the package-private class {@link
 * LegacyBitStorage}.
 *
 * @author Nullicorn
 */
class LegacyRegionUIntArrayTests extends RegionUIntArrayTests {

  @Override
  protected int getValidDataVersion() {
    return 2526;
  }

  @Override
  protected long[] createEmptyWords(int length, int magnitude) {
    int bitsNeeded = length * magnitude;
    int longsNeeded = (int) Math.ceil((double) bitsNeeded / Long.SIZE);
    return new long[longsNeeded];
  }

  @Override
  protected void set(int index, int value, long[] words, int magnitude) {
    long bitIndex = index * magnitude;
    int wordIndex = (int) (bitIndex / Long.SIZE);
    int bitOffset = (int) (bitIndex % Long.SIZE);
    long valueMask = BitHelper.createBitMask(magnitude);

    int bitsRead = 0;
    while (bitsRead < magnitude) {
      // Get only the bits being replaced for the current word.
      long bitsForWord = (value >>> bitsRead) & valueMask;

      // Clear & replace any existing value.
      words[wordIndex] &= ~(valueMask << bitOffset);
      words[wordIndex] |= bitsForWord << bitOffset;

      int bitsJustRead = Math.min(magnitude - bitsRead, Long.SIZE - bitOffset);
      bitsRead += bitsJustRead;
      valueMask >>>= bitsJustRead;

      bitOffset = 0;
      wordIndex++;
    }
  }
}
