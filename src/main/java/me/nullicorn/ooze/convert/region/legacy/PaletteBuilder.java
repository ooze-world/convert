package me.nullicorn.ooze.convert.region.legacy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import me.nullicorn.ooze.convert.ArrayUIntMap;
import me.nullicorn.ooze.convert.Codec;
import me.nullicorn.ooze.level.BlockState;
import me.nullicorn.ooze.level.Palette;

/**
 * An internal utility for compiling {@link NumericBlockState numeric block states} into a {@link
 * Palette palette} of {@link BlockState standard block states}.
 *
 * @author Nullicorn
 */
final class PaletteBuilder {

  private final int                                  dataVersion;
  private final Codec<BlockState, NumericBlockState> stateCodec;

  private final List<NumericBlockState> legacyStates;
  private final ArrayUIntMap            hashToIndex;

  /**
   * @param dataVersion The Minecraft world version the states added to this builder should be
   *                    compatible with.
   * @param stateCodec  A codec for converting numeric block states to standard ones. Used when
   *                    {@link #build(String name) building}.
   */
  PaletteBuilder(int dataVersion, Codec<BlockState, NumericBlockState> stateCodec) {
    this.dataVersion = dataVersion;
    this.stateCodec = stateCodec;

    this.legacyStates = new ArrayList<>();
    this.hashToIndex = new ArrayUIntMap();
  }

  /**
   * Same as {@link #add(int, int)}, but combining {@code extension} with {@code type} is done
   * internally.
   * <p>
   * {@code extension} and {@code type} are combined like so:
   * <pre>{@code (type & 0xff) | ((extension & 0xf) << 4)}</pre>
   * The result is a 12-bit integer with {@code type} being the lower 8 bits, and {@code extension}
   * being the upper 4 bits.
   *
   * @param extension Extra bits to prepend to the state's {@code type} (4 bits).
   */
  int add(int typeBase, int extension, int variant) {
    typeBase &= 0xff;
    extension &= 0xf;
    variant &= 0xf;

    int type = typeBase | (extension << 4);
    return add(type, variant);
  }

  /**
   * Adds a state to the palette, so long as it did not already contain that state.
   * <p><br>
   * The returned integer is the state's index in any palette returned via {@link #build(String)
   * build()}. More specifically, it's the input value where {@link Palette#get(int)} will return an
   * equivalent state.
   *
   * @param type    The state's main identifier (8 bits).
   * @param variant The variant of the main block {@code type} used by the state (4 bits).
   * @return the state's index in the palette being built.
   */
  int add(int type, int variant) {
    type &= 0xfff;
    variant &= 0xf;

    int hash = hashState(type, variant);
    int index = hashToIndex.get(hash);
    if (index == -1) {
      legacyStates.add(new NumericBlockState(type, (byte) variant));
      hashToIndex.set(hash, legacyStates.size() - 1);
    }

    return index;
  }

  /**
   * Same as {@link #build(String)}, but the {@code name} parameter is generated arbitrarily in a
   * way that is unique to the builder's current state.
   */
  Palette build() throws IOException {
    String defaultName = "ooze:flattened_" + Integer.toHexString(legacyStates.hashCode());
    return build(defaultName);
  }

  /**
   * Converts each numeric state {@link #add(int, int) added} to the builder into standard {@link
   * BlockState block state}, then creates a {@link Palette palette} containing each of those
   * converted states.
   *
   * @param name The {@code name} to be assigned to the returned palette.
   * @return the built palette.
   * @throws IOException If any of the builder's states could not be converted to a {@link
   *                     BlockState}.
   */
  Palette build(String name) throws IOException {
    List<BlockState> states = new ArrayList<>(legacyStates.size());

    // Convert each numeric state to a regular one.
    for (NumericBlockState legacyState : legacyStates) {
      BlockState state = stateCodec.decode(legacyState);
      states.add(state);
    }

    return new Palette(name, dataVersion, states);
  }

  /**
   * A simple function for "hashing" a state's {@code type} and {@code variant} into a 16-bit
   * integer that can identify the state.
   */
  private static int hashState(int type, int variant) {
    type &= 0xfff;
    variant &= 0xf;

    // Higher 8 bits are the type, lower 4 bits are the variant.
    return (type << 4) | variant;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PaletteBuilder that = (PaletteBuilder) o;
    return legacyStates.equals(that.legacyStates) &&
           hashToIndex.equals(that.hashToIndex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(legacyStates, hashToIndex);
  }
}
