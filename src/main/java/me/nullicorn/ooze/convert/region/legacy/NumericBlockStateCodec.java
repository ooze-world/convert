package me.nullicorn.ooze.convert.region.legacy;

import java.io.IOException;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.ooze.convert.VersionedCodec;
import me.nullicorn.ooze.convert.VersionedTag;
import me.nullicorn.ooze.level.BlockState;

/**
 * An internal codec for converting {@link BlockState standard} states to and from {@link
 * NumericBlockState numeric} states (aka legacy states).
 *
 * @author Nullicorn
 */
class NumericBlockStateCodec extends VersionedCodec<BlockState, NumericBlockState> {

  /**
   * The name assigned to any {@link BlockState}s decoded using this codec. Encoded states are
   * expected to already have this name.
   */
  private static final String STATE_NAME = "ooze:any_pre_flattening";

  // Internal NBT tags used to persist the type & variant of numeric states.
  private static final VersionedTag TYPE_TAG    = LegacyTag.OOZE_STATE_TYPE;
  private static final VersionedTag VARIANT_TAG = LegacyTag.OOZE_STATE_VARIANT;

  /**
   * Creates a codec compatible with a specific Minecraft {@code dataVersion}.
   *
   * @throws IllegalArgumentException if the {@code dataVersion} is not between {@code 99} and
   *                                  {@code 1450} exclusive.
   */
  NumericBlockStateCodec(int dataVersion) {
    super(dataVersion, TYPE_TAG, VARIANT_TAG);
  }

  // TODO: 9/28/21 Document encoding process.

  @Override
  public NumericBlockState encode(BlockState state) throws IOException {
    if (state == null) {
      throw new IllegalArgumentException("null cannot be encoded as a block state");
    }

    String name = state.getName();
    NBTCompound properties = state.getProperties();

    // Make sure the state is in the correct format.
    if (!name.equalsIgnoreCase(STATE_NAME)) {
      throw new IOException("Numeric states cannot have custom names: " + state.getName());
    } else if (!state.hasProperties()) {
      throw new IOException("Numeric states must have properties");
    }

    // Get the block's main type (8 bits; required).
    int type = getTagValue(TYPE_TAG, properties)
        .map(int.class::cast)
        .orElseThrow(() -> new IOException("Numeric state has no type: " + state));

    // Get the block's variant (4 bits; not required).
    int variant = getTagValue(VARIANT_TAG, properties)
        .map(int.class::cast)
        .orElse(0);

    return new NumericBlockState(type, (byte) variant);
  }

  @Override
  public BlockState decode(NumericBlockState state) {
    if (state == null) {
      throw new IllegalArgumentException("null cannot be decoded as a numeric block state");
    }

    int type = state.getType();
    int variant = state.getVariant();
    boolean isEmpty = state.isEmpty();
    NBTCompound properties = new NBTCompound();

    setTagValue(TYPE_TAG, type, properties);
    if (state.isVariant()) {
      setTagValue(VARIANT_TAG, variant, properties);
    }

    return new BlockState(STATE_NAME, properties, isEmpty);
  }
}
