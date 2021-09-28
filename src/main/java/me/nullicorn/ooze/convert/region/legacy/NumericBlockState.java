package me.nullicorn.ooze.convert.region.legacy;

import me.nullicorn.ooze.level.BitHelper;
import me.nullicorn.ooze.level.BlockState;

/**
 * Represents the state of a block generated before Minecraft's flattening update (data version
 * 1451).
 *
 * @author Nullicorn
 */
class NumericBlockState {

  private final int type;
  private final int variant;

  /**
   * @param type    See {@link #getType()}. (may be 8-bit or 12-bit)
   * @param variant See {@link #getVariant()}.
   */
  NumericBlockState(int type, int variant) {
    this.type = type & 0xfff;
    this.variant = variant & 0b1111;
  }

  /**
   * The blocks's main numeric identifier.
   * <p>
   * The returned value is an unsigned 8-bit integer. For states that use 12-bit identifiers, see
   * {@link #getTypeExtension()}. That only applies if {@link #isTypeExtended()} returns {@code
   * true}.
   *
   * @return the block type of the state.
   * @apiNote Sometimes referred to as the block's "ID".
   */
  byte getType() {
    // Return the type's lower 8 bits (bits 0 through 7)
    return (byte) (type & 0xff);
  }

  /**
   * 4 extra most-significant bits that can be combined with the state's {@link #getType() type} to
   * get a full 12-bit type.
   *
   * @return extra bits for the block's {@code type}, or {@code 0} if the block only uses an 8-bit
   * type.
   * @see #getType()
   * @see #isTypeExtended()
   */
  byte getTypeExtension() {
    // Return the type's upper 4 bits (bits 8 through 11).
    return (byte) (type >>> Byte.SIZE & 0xf);
  }

  /**
   * Whether or not the state's {@code type} uses 12 bits instead of 8.
   * <p>
   * If {@code true}, the values returned by {@link #getType()} and {@link #getTypeExtension()}
   * should be combined like so:
   * <pre>{@code type | (typeExtension << 8)}</pre>
   * If {@code false} is returned, then only using {@code type} is acceptable.
   *
   * @return whether or not the state uses a 12-bit type.
   */
  boolean isTypeExtended() {
    return BitHelper.widthInBits(type) > Byte.SIZE;
  }

  /**
   * A 4-bit integer representing a variant of block off the state's main {@link #getType() type}.
   * <p>
   * Similar to a standard block states' {@link BlockState#getProperties() properties} field.
   *
   * @return the state's variant, or {@code 0} if the state uses the {@link #isVariant() default
   * variant}.
   * @see #isVariant()
   */
  int getVariant() {
    return variant;
  }

  /**
   * Whether or not the state uses a non-default {@code variant} of the main {@link #getType()
   * type}.
   *
   * @return whether or not {@link #getVariant()} will always return {@code 0}.
   * @see #getVariant()
   * @see #getType()
   */
  boolean isVariant() {
    return variant != 0;
  }

  /**
   * Equivalent of {@link BlockState#isEmpty()}.
   */
  boolean isEmpty() {
    return type == 0;
  }
}
