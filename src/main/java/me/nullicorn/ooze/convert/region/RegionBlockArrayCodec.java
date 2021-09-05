package me.nullicorn.ooze.convert.region;

import me.nullicorn.ooze.convert.VersionedCodec;
import me.nullicorn.ooze.convert.region.array.RegionUIntArray;
import me.nullicorn.ooze.level.PackedUIntArray;

/**
 * Provides translation between integer arrays and Minecraft's <a href=https://wiki.vg/Chunk_Format#Compacted_data_array>packed
 * array format</a>
 *
 * @author Nullicorn
 */
public class RegionBlockArrayCodec extends VersionedCodec<PackedUIntArray, RegionUIntArray> {

  /**
   * Creates a codec compatible with a specific Minecraft {@code dataVersion}.
   *
   * @throws IllegalArgumentException if the {@code dataVersion} does not support 64-bit compact
   *                                  arrays.
   */
  public RegionBlockArrayCodec(int dataVersion) {
    super(dataVersion, RegionTag.BLOCKS);
  }

  /**
   * Packs an {@code array} into 64-bit words.
   * <p>
   * See the link below for the encoded format. If {@link #getCompatibility() version} {@code >=
   * 2527}, the newer encoding is used. Otherwise the older one is used.
   *
   * @param array An array of the values to be packed into 64-bit words
   * @return 64-bit words containing the input values, with order preserved.
   * @throws IllegalArgumentException if the {@code array} is {@code null}.
   * @see <a href=https://wiki.vg/Chunk_Format#Compacted_data_array>Compact data array format</a>
   */
  public RegionUIntArray encode(PackedUIntArray array) {
    if (array == null) {
      throw new IllegalArgumentException("null array cannot be encoded");
    }

    RegionUIntArray dataArray = RegionUIntArray.from(array.size(), array.magnitude(), dataVersion);
    for (int i = 0; i < array.size(); i++) {
      dataArray.set(i, array.get(i));
    }
    return dataArray;
  }

  /**
   * Unpacks an array of ints from 64-bit words. More information {@link #encode(PackedUIntArray)
   * here}.
   *
   * @param array An array of 64-bit words, each containing uint values.
   * @return the unpacked values stored in the words.
   * @throws IllegalArgumentException if the {@code array} is null.
   * @see #encode(PackedUIntArray) encode()
   */
  public PackedUIntArray decode(RegionUIntArray array) {
    if (array == null) {
      throw new IllegalArgumentException("null words array cannot be decoded");
    }

    int[] values = new int[array.length()];
    for (int i = 0; i < values.length; i++) {
      values[i] = array.get(i);
    }

    return new PackedUIntArray(values);
  }
}
