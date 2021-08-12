package me.nullicorn.ooze.convert.region;

import me.nullicorn.ooze.level.BitHelper;
import me.nullicorn.ooze.convert.VersionedCodec;

/**
 * Provides translation between integer arrays and Minecraft's <a href=https://wiki.vg/Chunk_Format#Compacted_data_array>packed
 * array format</a>
 *
 * @author Nullicorn
 */
public class RegionCompactArrayCodec extends VersionedCodec {

  /**
   * The data version when values were no longer able to be split across multiple longs.
   */
  private static final int USE_PADDING_VERSION = 2527;

  /**
   * @throws IllegalArgumentException if the magnitude is invalid for a 32-bit integer.
   */
  private static void checkMagnitude(int magnitude) {
    if (magnitude <= 0 || magnitude > 32) {
      throw new IllegalArgumentException("magnitude must be in range (0, 32] bits");
    }
  }

  /**
   * Helper function that encodes arrays using the new format (padded).
   */
  private static long[] encodePadded(int[] values, int magnitude) {
    int valuesPerWord = Long.SIZE / magnitude;
    int wordsNeeded = (int) Math.ceil((double) values.length / valuesPerWord);
    int valueMask = BitHelper.createBitMask(magnitude);

    long[] words = new long[wordsNeeded];

    int i = 0;
    for (int w = 0; w < words.length; w++) {
      for (int ofst = 0; ofst < Long.SIZE && i < values.length; ofst += magnitude, i++) {
        int value = values[i] & valueMask;
        if (ofst < 0) {
          value >>>= -ofst;
        } else {
          value <<= ofst;
        }

        words[w] |= value;
      }
    }

    return words;
  }

  /**
   * Helper function that encodes arrays using the old format (unpadded).
   */
  private static long[] encodeUnpadded(int[] values, int magnitude) {
    int wordsNeeded = (int) Math.ceil(magnitude * values.length / 64d);

    long[] words = new long[wordsNeeded];

    int valueMask = BitHelper.createBitMask(magnitude);
    for (int i = 0, word = 0, ofst = 0; i < values.length; i++) {
      int value = values[i] & valueMask;
      words[word] |= value << ofst;

      // Check if we ran out of bits in the
      // current word.
      int lastBitOffset = ofst + magnitude - 1;
      if (lastBitOffset > Long.SIZE) {
        // If so, move onto the next one and
        // reset the offset.
        word++;
        ofst = 0;

        // And add whatever bits we couldn't
        // fit onto the next word.
        words[word] = value >> (lastBitOffset - 64);
      }
    }

    return words;
  }

  /**
   * Helper function that decodes arrays using the new format (padded).
   */
  private static int[] decodePadded(long[] words, int magnitude) {
    int valuesPerWord = Long.SIZE / magnitude;
    int valueMask = BitHelper.createBitMask(magnitude);

    int[] values = new int[valuesPerWord * words.length];

    int i = 0;
    for (long word : words) {
      for (int ofst = 0; ofst < Long.SIZE; ofst += magnitude, i++) {
        values[i] = (int) (valueMask & (word >>> ofst));
      }
    }

    return values;
  }

  /**
   * Helper function that decodes arrays using the old format (unpadded).
   */
  private int[] decodeUnpadded(long[] words, int magnitude) {
    int valueMask = BitHelper.createBitMask(magnitude);

    int[] values = new int[words.length * 64 / magnitude];

    // Iterate over the word array, but only as long as
    // values are needed.
    for (int i = 0, w = 0, ofst = 0; i < values.length; i++, ofst += magnitude) {
      // Move onto the next word once the current
      // one is out of bits.
      if (ofst > Long.SIZE) {
        w++;
      }

      long valueBits;
      if (ofst <= Long.SIZE) {
        valueBits = (words[w] >>> ofst) & valueMask;
      } else {
        // This means part of the value came from the
        // previous word, but wasn't completed. We
        // indicate this by making the offset negative
        // by however many bits the value still needs.
        ofst = -(ofst - Long.SIZE);

        // Before &ing the value, shrink the mask so it
        // only covers the needed bits.
        valueBits = (words[w] & (valueMask >>> -ofst)) << -ofst;
      }
      values[i] |= (int) valueBits;
    }

    return values;
  }

  /**
   * Creates a codec compatible with a specific Minecraft {@code dataVersion}.
   *
   * @throws IllegalArgumentException if the {@code dataVersion} does not support 64-bit compact
   *                                  arrays.
   */
  public RegionCompactArrayCodec(int dataVersion) {
    super(dataVersion, RegionTag.BLOCKS);
  }

  /**
   * Packs an array of {@code values} into 64-bit words. Within the words, each value is represented
   * using the number of bits specified by the {@code magnitude}. For values that exceed that
   * magnitude, any higher-order bits will be treated as {@code 0}s.
   * <p>
   * See the link below for the encoded format. If {@link #getCompatibility() version} {@code >=
   * 2527}, the newer encoding is used. Otherwise the older one is used.
   *
   * @param values    A plain array of the values to be encoded.
   * @param magnitude The number of bits used to represent values within the words.
   * @return the input values, packed into 64-bit words.
   * @throws IllegalArgumentException if the magnitude is not in the range {@code (0, 32]}. Also if
   *                                  the {@code values} array is {@code null}.
   * @see <a href=https://wiki.vg/Chunk_Format#Compacted_data_array>Compact data array format</a>
   */
  public long[] encode(int[] values, int magnitude) {
    checkMagnitude(magnitude);
    if (values == null) {
      throw new IllegalArgumentException("null values array cannot be encoded");
    }

    return dataVersion >= USE_PADDING_VERSION
        ? encodePadded(values, magnitude)
        : encodeUnpadded(values, magnitude);
  }

  /**
   * Unpacks an array of ints from 64-bit words. More information {@link #encode(int[], int) here}.
   *
   * @param words     An array of values packed into 64-bit words.
   * @param magnitude The number of bits used to encode each value within the words.
   * @return the unpacked values stored in the words.
   * @apiNote As many values as possible are read, so the decoded array may be larger than the
   * original.
   * @see #encode(int[], int) encode()
   */
  public int[] decode(long[] words, int magnitude) {
    checkMagnitude(magnitude);
    if (words == null) {
      throw new IllegalArgumentException("null words array cannot be decoded");
    }

    return dataVersion >= USE_PADDING_VERSION
        ? decodePadded(words, magnitude)
        : decodeUnpadded(words, magnitude);
  }
}
