package me.nullicorn.ooze.convert.region.storage;

/**
 * Tests for the modern encoding of RegionUIntArrays. Implemented by the package-private class
 * {@link BitStorage}.
 *
 * @author Nullicorn
 */
class ModernRegionUIntArrayTests extends RegionUIntArrayTests {

  @Override
  protected int getValidDataVersion() {
    return 2527;
  }

  @Override
  protected long[] createEmptyWords(int length, int magnitude) {
    int longsNeeded;

    if (magnitude == 0) {
      longsNeeded = 0;
    } else {
      int valuesPerLong = Long.SIZE / magnitude;
      longsNeeded = (int) Math.ceil((double) length / valuesPerLong);
    }

    return new long[longsNeeded];
  }

  @Override
  protected void set(int index, int value, long[] words, int magnitude) {
    if (magnitude == 0) {
      return;
    }

    long valueMask = (1 << magnitude) - 1;
    int valuesPerLong = Long.SIZE / magnitude;

    // Determine the value's location.
    int wordIndex = index / valuesPerLong;
    int valueOffset = index % valuesPerLong * magnitude;

    // Clear & replace any existing value.
    words[wordIndex] &= ~(valueMask << valueOffset);
    words[wordIndex] |= ((long) value & valueMask) << valueOffset;
  }
}
