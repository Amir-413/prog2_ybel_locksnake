package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.*;
import java.util.List;

public final class GameState {

  private final Level level;
  private final Snake snake;
  private final List<Pin> pins;
  private final Status status;
  private final Direction pendingDirection;

  public GameState(
      Level level, Snake snake, List<Pin> pins, Status status, Direction pendingDirection) {
    this.level = level;
    this.snake = snake;
    this.pins = List.copyOf(pins);
    this.status = status;
    this.pendingDirection = pendingDirection;
  }

  public Level level() {
    return level;
  }

  public Snake snake() {
    return snake;
  }

  public List<Pin> pins() {
    return pins;
  }

  public Status status() {
    return status;
  }

  public Direction pendingDirection() {
    return pendingDirection;
  }

  public GameState tick() {
    if (!status.isRunning() || pendingDirection == Direction.NONE) {
      return this;
    }

    var nextHead = snake.nextHead(pendingDirection);

    if (!level.isInside(nextHead)) {
      return new GameState(level, snake, pins, Status.LOST_OUT_OF_BOUNDS, Direction.NONE);
    }

    if (level.cellAt(nextHead) == CellType.WALL) {
      return new GameState(level, snake, pins, status, Direction.NONE);
    }

    if (snake.occupies(nextHead)) {
      return new GameState(level, snake, pins, Status.LOST_SELF_COLLISION, Direction.NONE);
    }

    // check if there's a pin at the next position
    for (var pin : pins) {
      if (pin.position().equals(nextHead)) {
        // wrong direction or pin already set → blocked
        if (pin.state().isSet() || pin.activationDirection() != pendingDirection) {
          return new GameState(level, snake, pins, status, Direction.NONE);
        }
        // activate the pin, snake stays put
        var newPins = pins.stream()
            .map(p -> p.position().equals(nextHead) ? p.withState(Pin.State.HIGH) : p)
            .toList();
        var allSet = newPins.stream().allMatch(p -> p.state().isSet());
        var newStatus = allSet ? Status.WON : status;
        return new GameState(level, snake, newPins, newStatus, Direction.NONE);
      }
    }

    return new GameState(level, snake.grow(pendingDirection), pins, status, pendingDirection);
  }

  public enum Status {
    RUNNING,
    WON,
    LOST_SELF_COLLISION,
    LOST_OUT_OF_BOUNDS;

    public boolean isRunning() {
      return this == RUNNING;
    }
  }
}
