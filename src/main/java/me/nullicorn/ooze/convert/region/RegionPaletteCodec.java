package me.nullicorn.ooze.convert.region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.nedit.type.TagType;
import me.nullicorn.ooze.level.BlockState;
import me.nullicorn.ooze.level.Palette;
import me.nullicorn.ooze.convert.ArrayUIntMap;
import me.nullicorn.ooze.convert.VersionedCodec;

/**
 * Provides serialization to and from lists of NBT block states, also known as a palette.
 *
 * @author Nullicorn
 */
public class RegionPaletteCodec extends VersionedCodec {

  private final RegionBlockStateCodec blockStateCodec;

  /**
   * Creates a codec compatible with a specific Minecraft {@code dataVersion}.
   *
   * @throws IllegalArgumentException if the {@code dataVersion} does not support palettes.
   */
  public RegionPaletteCodec(int dataVersion) {
    super(dataVersion, RegionTag.PALETTE);

    this.blockStateCodec = new RegionBlockStateCodec(dataVersion);
  }

  /**
   * Converts a palette of blocks to a vanilla-compatible list of NBT block states.
   * <p><br>
   * The returned palette will only include the states whose indices are set in {@code statesInUse}.
   * Doing this may cause indices to change, and any that do change will be added to {@code
   * indexRemap} (where the original index is the key, and the encoded index is the value). The map
   * should be used to convert any encoded data that depends on the palette, such as arrays of
   * blocks.
   * <p><br>
   * The resulting list will have a {@link NBTList#getContentType() content-type} of {@link
   * TagType#COMPOUND COMPOUND}, where each entry in the list is an {@link RegionBlockStateCodec
   * NBT-encoded block state}.
   *
   * @param palette     The palette whose states should be encoded.
   * @param statesInUse A bit field indicating which states in the palette should be included in the
   *                    output. Indices in the bit field correspond to those of the palette.
   * @param indexRemap  An empty map that changed indices will be added to.
   * @return An NBT-encoded palette containing only states that are used.
   * @throws IllegalArgumentException if any of the arguments are {@code null}.
   * @see RegionBlockStateCodec
   */
  public NBTList encode(Palette palette, BitSet statesInUse, ArrayUIntMap indexRemap) {
    if (palette == null) {
      throw new IllegalArgumentException("null cannot be encoded as a palette");
    } else if (statesInUse == null) {
      throw new IllegalArgumentException("statesInUse bitfield cannot be null");
    } else if (indexRemap == null) {
      throw new IllegalArgumentException("indexRemap cannot be null");
    }

    NBTList encodedPalette = new NBTList(TagType.COMPOUND);

    int numberOfStatesUsed = statesInUse.cardinality();
    if (numberOfStatesUsed == palette.size()) {
      // Include all states.
      for (BlockState state : palette) {
        encodedPalette.add(blockStateCodec.encode(state));
      }
    } else {
      // Only include used states.
      for (int i = 0; i < palette.size(); i++) {
        // Only encode the state if it is used.
        if (statesInUse.get(i)) {
          // If the state's index changed, include
          // its new index in the re-mapping.
          int newIndex = encodedPalette.size();
          if (newIndex != i) {
            indexRemap.set(i, newIndex);
          }

          // Convert the state to a compound and add it
          // to the NBT palette.
          BlockState state = palette.get(i);
          encodedPalette.add(blockStateCodec.encode(state));
        }
      }
    }

    return encodedPalette;
  }

  /**
   * Creates a new palette with all of the block states indicated by a list of NBT compounds. The
   * expected format is described {@link #encode(Palette, BitSet, ArrayUIntMap) here}.
   *
   * @param palette a list of NBT-encoded block states.
   * @return the palette defined by the input list.
   * @throws IllegalArgumentException if the input list is {@code null}.
   * @throws IOException              if the list's {@link NBTList#getContentType() content-type} is
   *                                  not {@link TagType#COMPOUND COMPOUND}, or if any state in the
   *                                  list has no {@code Name} tag.
   * @see RegionBlockStateCodec
   */
  public Palette decode(NBTList palette) throws IOException {
    if (palette == null) {
      throw new IllegalArgumentException("null cannot be decoded as a palette");
    } else if (palette.getContentType() != TagType.COMPOUND) {
      throw new IOException("Palette must be a list of compounds, not " + palette.getContentType());
    }

    List<BlockState> states = new ArrayList<>(palette.size());
    for (Object entry : palette) {
      BlockState state = blockStateCodec.decode((NBTCompound) entry);
      states.add(state);
    }

    String name = "ooze:" + UUID.randomUUID().toString();
    return new Palette(name, dataVersion, states);
  }
}
