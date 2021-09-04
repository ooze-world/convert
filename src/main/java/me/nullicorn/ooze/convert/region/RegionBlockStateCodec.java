package me.nullicorn.ooze.convert.region;

import java.io.IOException;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.ooze.convert.VersionedCodec;
import me.nullicorn.ooze.convert.VersionedTag;
import me.nullicorn.ooze.level.BlockState;

/**
 * Provides serialization to and from NBT block states stored in palettes.
 *
 * @author Nullicorn
 */
public class RegionBlockStateCodec extends VersionedCodec<BlockState, NBTCompound> {

  private static final VersionedTag NAME_TAG       = RegionTag.BLOCK_NAME;
  private static final VersionedTag PROPERTIES_TAG = RegionTag.BLOCK_PROPERTIES;

  /**
   * Creates a codec compatible with a specific Minecraft {@code dataVersion}.
   *
   * @throws IllegalArgumentException if the {@code dataVersion} does not support compound block
   *                                  states.
   */
  public RegionBlockStateCodec(int dataVersion) {
    super(dataVersion, NAME_TAG, PROPERTIES_TAG);
  }

  /**
   * Creates a new compound tag containing the {@code state}'s {@link BlockState#getName() name} and
   * {@link BlockState#getProperties() properties}, if it has any.
   * <p>
   * The state's name can be found under a string tag, {@code Name}, within the compound. If the
   * state {@link BlockState#hasProperties() has any properties}, those are also copied into the new
   * compound under a compound tag, {@code Properties}.
   *
   * @param state The block state to NBT-encode.
   * @return an NBT compound resembling the inputted block state.
   * @throws IllegalArgumentException if the input state is {@code null}.
   */
  @Override
  public NBTCompound encode(BlockState state) {
    if (state == null) {
      throw new IllegalArgumentException("null cannot be encoded as a block state");
    }

    NBTCompound encoded = new NBTCompound();

    NAME_TAG.setValueIn(encoded, state.getName());
    if (state.hasProperties()) {
      // Copy the properties to a mutable compound so
      // that the caller can modify them if needed.
      NBTCompound properties = new NBTCompound();
      properties.putAll(state.getProperties());

      PROPERTIES_TAG.setValueIn(encoded, properties);
    }

    return encoded;
  }

  /**
   * Creates a new block state with the name and properties defined by an NBT compound tag. The
   * expected format is described {@link #encode(BlockState) here}.
   *
   * @param state An NBT compound representing a Minecraft block state.
   * @return the block state defined by the compound's tags.
   * @throws IllegalArgumentException if the input compound is null.
   * @throws IOException              if the compound has no {@code Name} tag.
   */
  @Override
  public BlockState decode(NBTCompound state) throws IOException {
    if (state == null) {
      throw new IllegalArgumentException("null cannot be decoded as a block state");
    }

    return NAME_TAG
        .valueIn(state, String.class)
        .map(name -> {
          // Use an empty compound if the state
          // has no properties.
          NBTCompound properties = PROPERTIES_TAG
              .valueIn(state, NBTCompound.class)
              .orElseGet(NBTCompound::new);

          // Create the final state.
          return new BlockState(name, properties);
        }).orElseThrow(() -> new IOException("State has no name: " + state));
  }
}
