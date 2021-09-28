package me.nullicorn.ooze.convert.region;

import java.io.IOException;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.ooze.convert.MalformedInputException;
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

    NBTCompound output = new NBTCompound();
    setTagValue(NAME_TAG, state.getName(), output);

    if (state.hasProperties()) {
      // Copy the properties to a mutable compound so that the caller can modify them if needed.
      NBTCompound properties = new NBTCompound();
      properties.putAll(state.getProperties());

      setTagValue(PROPERTIES_TAG, properties, output);
    }

    return output;
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

    String name = getTagValue(NAME_TAG, state)
        .map(String.class::cast)
        .orElseThrow(() -> new MalformedInputException("block state", "has no name"));

    NBTCompound properties = getTagValue(PROPERTIES_TAG, state)
        .map(NBTCompound.class::cast)
        .orElseGet(NBTCompound::new);

    // TODO: 9/27/21 Provide `isEmpty` value if state is air.
    return new BlockState(name, properties);
  }
}
