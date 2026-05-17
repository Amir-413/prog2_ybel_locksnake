package de.hsbi.lockgame.logic;

import de.hsbi.lockgame.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameState logic.
 *
 * <p>Covers: initial state, movement, wall collision, out-of-bounds,
 * self-collision, pin activation/blocking, win condition, game-over freeze.
 *
 * <p>Pattern: given – when – then.
 */
class GameStateTest {

    /** 7x7 open level: walls only on the border, no pins. Snake starts at (3,3). */
    private Level openLevel;
    private Position center;

    @BeforeEach
    void setUp() {
        center = new Position(3, 3);
        openLevel = buildLevel(7, 7, List.of(), center);
    }

    // -----------------------------------------------------------------------
    // Helper fixtures
    // -----------------------------------------------------------------------

    /** Build a level with border walls and the given pins. */
    private Level buildLevel(int width, int height, List<Pin> pins, Position start) {
        var cells = new CellType[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cells[x][y] = (x == 0 || x == width - 1 || y == 0 || y == height - 1)
                        ? CellType.WALL
                        : CellType.EMPTY;
            }
        }
        for (var pin : pins) {
            cells[pin.position().x()][pin.position().y()] = CellType.PIN_SLOT;
        }
        return new Level(width, height, cells, List.copyOf(pins), start);
    }

    /** Create the canonical initial GameState for a given level. */
    private GameState initialState(Level level) {
        return new GameState(
                level,
                new Snake(List.of(level.snakeStart())),
                new ArrayList<>(level.pins()),
                GameState.Status.RUNNING,
                Direction.NONE);
    }

    /** Shortcut: create a running state with a direction already set. */
    private GameState withDirection(GameState base, Direction d) {
        return new GameState(base.level(), base.snake(), base.pins(), base.status(), d);
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /** Test 1 – initial state is RUNNING. */
    @Test
    void testInitialStateIsRunning() {
        // given
        var state = initialState(openLevel);

        // then
        assertEquals(GameState.Status.RUNNING, state.status());
        assertTrue(state.status().isRunning());
    }

    /** Test 2 – snake head starts exactly at the level's start position. */
    @Test
    void testInitialSnakeAtStartPosition() {
        // given
        var state = initialState(openLevel);

        // when
        var head = state.snake().head();

        // then
        assertEquals(center, head);
        assertEquals(1, state.snake().body().size());
    }

    /** Test 3 – tick without a pending direction leaves the snake in place. */
    @Test
    void testNoMovementWithoutPendingDirection() {
        // given
        var state = initialState(openLevel); // pendingDirection = NONE

        // when
        var after = state.tick();

        // then
        assertSame(state, after, "Same object returned when nothing changes");
        assertEquals(center, after.snake().head());
    }

    /** Test 4 – snake moves right and its body grows by one cell. */
    @Test
    void testSnakeMovesRightAndGrows() {
        // given
        var state = withDirection(initialState(openLevel), Direction.RIGHT);

        // when
        var after = state.tick();

        // then
        assertEquals(new Position(4, 3), after.snake().head());
        assertEquals(2, after.snake().body().size());
    }

    /** Test 5 – wall directly to the left blocks the snake and clears the direction. */
    @Test
    void testWallBlocksMovementAndClearsDirection() {
        // given: snake at (1,3), one step from the left border wall
        var start = new Position(1, 3);
        var level = buildLevel(7, 7, List.of(), start);
        var state = new GameState(level, new Snake(List.of(start)),
                new ArrayList<>(), GameState.Status.RUNNING, Direction.LEFT);

        // when
        var after = state.tick();

        // then
        assertEquals(start, after.snake().head());           // didn't move
        assertEquals(Direction.NONE, after.pendingDirection()); // direction cleared
        assertEquals(GameState.Status.RUNNING, after.status());
    }

    /** Test 6 – moving out of the grid bounds results in LOST_OUT_OF_BOUNDS. */
    @Test
    void testOutOfBoundsResultsInLoss() {
        // given: 3x3 level with NO border walls, snake at top edge (1,0) moving UP
        var cells = new CellType[3][3];
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 3; y++)
                cells[x][y] = CellType.EMPTY;
        var noWallLevel = new Level(3, 3, cells, List.of(), new Position(1, 0));
        var state = new GameState(noWallLevel, new Snake(List.of(new Position(1, 0))),
                new ArrayList<>(), GameState.Status.RUNNING, Direction.UP);

        // when – nextHead = (1,-1), outside the grid
        var after = state.tick();

        // then
        assertEquals(GameState.Status.LOST_OUT_OF_BOUNDS, after.status());
    }

    /** Test 7 – snake biting its own body causes LOST_SELF_COLLISION. */
    @Test
    void testSelfCollisionResultsInLoss() {
        // given: snake coiled so that moving LEFT brings head onto its own body
        // body: (3,3)→head, (4,3), (4,4), (3,4), (2,4), (2,3)
        var body = List.of(
                new Position(3, 3),
                new Position(4, 3),
                new Position(4, 4),
                new Position(3, 4),
                new Position(2, 4),
                new Position(2, 3)  // nextHead when moving LEFT = (2,3) → self-collision
        );
        var state = new GameState(openLevel, new Snake(body),
                new ArrayList<>(), GameState.Status.RUNNING, Direction.LEFT);

        // when
        var after = state.tick();

        // then
        assertEquals(GameState.Status.LOST_SELF_COLLISION, after.status());
    }

    /** Test 8 – pin is activated when the snake approaches from the correct direction. */
    @Test
    void testPinActivatedFromCorrectDirection() {
        // given: pin at (3,2) with activationDirection=DOWN; snake at (3,1) moving DOWN
        var pinPos = new Position(3, 2);
        var pin = new Pin(pinPos, Pin.State.LOW, Direction.DOWN);
        var start = new Position(3, 1);
        var level = buildLevel(7, 7, List.of(pin), start);
        var state = new GameState(level, new Snake(List.of(start)),
                new ArrayList<>(List.of(pin)), GameState.Status.RUNNING, Direction.DOWN);

        // when
        var after = state.tick();

        // then: pin set HIGH, snake did NOT move, direction cleared
        assertEquals(start, after.snake().head());
        assertEquals(Pin.State.HIGH, after.pins().get(0).state());
        assertEquals(Direction.NONE, after.pendingDirection());
        assertTrue(after.status().isRunning());
    }

    /** Test 9 – pin with wrong approach direction acts as a wall (blocked). */
    @Test
    void testPinBlocksFromWrongDirection() {
        // given: pin at (3,2) needs DOWN, snake at (3,3) moves UP → wrong direction
        var pinPos = new Position(3, 2);
        var pin = new Pin(pinPos, Pin.State.LOW, Direction.DOWN);
        var start = new Position(3, 3);
        var level = buildLevel(7, 7, List.of(pin), start);
        var state = new GameState(level, new Snake(List.of(start)),
                new ArrayList<>(List.of(pin)), GameState.Status.RUNNING, Direction.UP);

        // when
        var after = state.tick();

        // then: pin untouched, snake blocked, direction cleared
        assertEquals(start, after.snake().head());
        assertEquals(Pin.State.LOW, after.pins().get(0).state());
        assertEquals(Direction.NONE, after.pendingDirection());
    }

    /** Test 10 – a pin already in HIGH state blocks movement like a wall. */
    @Test
    void testPinAlreadySetBlocksMovement() {
        // given: pin at (3,2) already HIGH, snake at (3,1) moving DOWN
        var pinPos = new Position(3, 2);
        var pin = new Pin(pinPos, Pin.State.HIGH, Direction.DOWN);
        var start = new Position(3, 1);
        var level = buildLevel(7, 7, List.of(pin), start);
        var state = new GameState(level, new Snake(List.of(start)),
                new ArrayList<>(List.of(pin)), GameState.Status.RUNNING, Direction.DOWN);

        // when
        var after = state.tick();

        // then: snake blocked, direction cleared
        assertEquals(start, after.snake().head());
        assertEquals(Direction.NONE, after.pendingDirection());
    }

    /** Test 11 – activating the last LOW pin triggers the WON status. */
    @Test
    void testWinConditionWhenLastPinSet() {
        // given: single LOW pin; activating it sets all pins → win
        var pinPos = new Position(3, 2);
        var pin = new Pin(pinPos, Pin.State.LOW, Direction.DOWN);
        var start = new Position(3, 1);
        var level = buildLevel(7, 7, List.of(pin), start);
        var state = new GameState(level, new Snake(List.of(start)),
                new ArrayList<>(List.of(pin)), GameState.Status.RUNNING, Direction.DOWN);

        // when
        var after = state.tick();

        // then
        assertEquals(GameState.Status.WON, after.status());
        assertTrue(after.pins().stream().allMatch(p -> p.state().isSet()));
    }

    /** Test 12 – tick is a no-op once the game has ended (WON). */
    @Test
    void testNoTickAfterGameWon() {
        // given
        var state = new GameState(openLevel, new Snake(List.of(center)),
                new ArrayList<>(), GameState.Status.WON, Direction.RIGHT);

        // when
        var after = state.tick();

        // then: exact same object returned
        assertSame(state, after);
    }

    /** Test 13 – direction is preserved across a normal move (snake keeps going). */
    @Test
    void testDirectionPreservedAfterNormalMove() {
        // given
        var state = withDirection(initialState(openLevel), Direction.RIGHT);

        // when: two ticks in the same direction
        var after1 = state.tick();
        var after2 = after1.tick();

        // then: snake moved two steps right, direction still RIGHT
        assertEquals(new Position(5, 3), after2.snake().head());
        assertEquals(Direction.RIGHT, after2.pendingDirection());
        assertEquals(3, after2.snake().body().size());
    }
}
