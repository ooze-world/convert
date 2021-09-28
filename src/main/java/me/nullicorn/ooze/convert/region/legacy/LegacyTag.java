package me.nullicorn.ooze.convert.region.legacy;

import me.nullicorn.nedit.type.TagType;
import me.nullicorn.ooze.convert.VersionedTag;
import me.nullicorn.ooze.level.BlockState;

/**
 * NBT tags for representing data in older versions of Minecraft.
 * <p>
 * This also includes some internal tags, hence the class being package-private.
 *
 * @author Nullicorn
 */
enum LegacyTag implements VersionedTag {
  /**
   * An array of 4096 block IDs for the chunk section. Blocks appear in YZX order, meaning all
   * blocks with the same Z and Y positions will be adjacent in the array.
   * <p><br>
   * "Block ID" refers to the unique numeric identifiers assigned to blocks and items in older
   * versions of Minecraft.
   */
  BLOCK_TYPES("Blocks", TagType.BYTE_ARRAY, 99, 1450),

  /**
   * An array of 2048 octets, each containing two of the data/damage values for corresponding blocks
   * in the {@link #BLOCK_TYPES block array}.
   * <p><br>
   * Each octet contains two data/damage values. Given an index in the block array, the
   * corresponding data value can be found in the octet at {@code index / 2}. Even-number indices
   * use the octet's lowest four bits, and odd-number indices use the highest 4 bits.
   */
  BLOCK_VARIANTS("Data", TagType.BYTE_ARRAY, 99, 1450),

  /**
   * An optional array of 2048 octets, each containing a pair of 4-bit values that can be used to
   * extend the {@link #BLOCK_TYPES block array}.
   * <p><br>
   * Extension allows block IDs to use up to 12 bits, rather than the usual 8-bit cap imposed by the
   * block array. Extensions are applied by taking the corresponding 4 bits from this array,
   * left-shifting them 8 bits, and then adding the result to the corresponding block ID. The 4-bit
   * groups of this array are indexed exactly the same as the {@link #BLOCK_VARIANTS data array}.
   */
  BLOCK_TYPES_EXTENDED("Add", TagType.BYTE_ARRAY, 99, 1450),

  /**
   * An internal tag used by {@link NumericBlockStateCodec} to store the {@code type} of a {@link
   * NumericBlockState numeric} block state within the properties of a {@link BlockState standard}
   * block state.
   */
  OOZE_STATE_TYPE("type", TagType.BYTE, 99, 1450),

  /**
   * An optional internal tag used by {@link NumericBlockStateCodec} to store the {@code variant} of
   * a {@link NumericBlockState numeric} block state within the properties of a {@link BlockState
   * standard} block state.
   */
  OOZE_STATE_VARIANT("variant", TagType.BYTE, 99, 1450);

  private final String  tagName;
  private final TagType tagType;
  private final int     minVersion;
  private final int     maxVersion;

  LegacyTag(String tagName, TagType tagType, int since, int until) {
    if (tagName == null) {
      throw new IllegalArgumentException("null is not a valid tagName");
    } else if (tagType == null || tagType == TagType.END) {
      throw new IllegalArgumentException("tagType is invalid: " + tagType);
    } else if (since > until) {
      throw new IllegalArgumentException("First version exceeds last: " + since + " > " + until);
    }

    this.tagName = tagName;
    this.tagType = tagType;
    this.minVersion = since;
    this.maxVersion = until;
  }

  @Override
  public String getName() {
    return tagName;
  }

  @Override
  public TagType getType() {
    return tagType;
  }

  @Override
  public TagType getContentType() {
    throw new UnsupportedOperationException("Not a list: " + toString());
  }

  @Override
  public boolean isSupported(int dataVersion) {
    return dataVersion >= minVersion && dataVersion <= maxVersion;
  }

  @Override
  public String toString() {
    return tagName + "(" + tagType + ")";
  }
}
