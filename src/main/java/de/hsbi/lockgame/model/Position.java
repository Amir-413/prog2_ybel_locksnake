package de.hsbi.lockgame.model;

import java.util.Objects;

public final class Position {
  private final int x;
  private final int y;

  public Position(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public int x() {
    return x;
  }

  public int y() {
    return y;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Position other)) return false;
    return x == other.x && y == other.y;
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y);
  }
}
