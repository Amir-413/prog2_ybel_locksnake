package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.*;
import de.hsbi.lockgame.ui.GamePanel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class GameEngine {

  private GameState gameState;
  private final List<Consumer<GameState>> stateObservers = new ArrayList<>();

  public GameEngine(Level level) {
    var snake = new Snake(List.of(level.snakeStart()));
    gameState = new GameState(
        level,
        snake,
        new ArrayList<>(level.pins()),
        GameState.Status.RUNNING,
        Direction.NONE);
  }

  public GameState state() {
    return gameState;
  }

  // register GamePanel so it gets notified when state changes
  public void setGamePanel(GamePanel panel) {
    stateObservers.add(panel::update);
  }

  // called by GamePanel when a key is pressed
  public void update(Direction d) {
    gameState = new GameState(
        gameState.level(),
        gameState.snake(),
        gameState.pins(),
        gameState.status(),
        d);
    notifyObservers();
  }

  // called by the timer every tick
  public void tick() {
    gameState = gameState.tick();
    notifyObservers();
  }

  private void notifyObservers() {
    stateObservers.forEach(observer -> observer.accept(gameState));
  }
}
