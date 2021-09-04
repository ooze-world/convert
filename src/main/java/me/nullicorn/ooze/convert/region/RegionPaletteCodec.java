package me.nullicorn.ooze.convert.region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.nedit.type.TagType;
import me.nullicorn.ooze.convert.VersionedCodec;
import me.nullicorn.ooze.level.BlockState;
import me.nullicorn.ooze.level.Palette;

/**
 * Provides serialization to and from lists of NBT block states, also known as a palette.
 *
 * @author Nullicorn
 */
public class RegionPaletteCodec extends VersionedCodec<Palette, NBTList> {

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
   * The resulting list will have a {@link NBTList#getContentType() content-type} of {@link
   * TagType#COMPOUND COMPOUND}, where each entry in the list is an {@link RegionBlockStateCodec
   * NBT-encoded block state}. The order of block states is preserved during encoding.
   *
   * @param palette The palette whose states should be encoded.
   * @return an NBT-encoded palette containing all of the palette's states.
   * @throws IllegalArgumentException if the input palette is {@code null}.
   * @see RegionBlockStateCodec
   */
  @Override
  public NBTList encode(Palette palette) {
    if (palette == null) {
      throw new IllegalArgumentException("null cannot be encoded as a palette");
    }

    NBTList encodedPalette = new NBTList(TagType.COMPOUND);
    palette.forEach(state ->
        encodedPalette.add(blockStateCodec.encode(state))
    );
    return encodedPalette;
  }

  /**
   * Creates a new palette with all of the block states indicated by a list of NBT compounds. The
   * expected format is described {@link #encode(Palette) here}.
   *
   * @param palette a list of NBT-encoded block states.
   * @return the palette defined by the input list.
   * @throws IllegalArgumentException if the input list is {@code null}.
   * @throws IOException              if the list's {@link NBTList#getContentType() content-type} is
   *                                  not {@link TagType#COMPOUND COMPOUND}, or if any state in the
   *                                  list has no {@code Name} tag.
   * @see RegionBlockStateCodec
   */
  @Override
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
