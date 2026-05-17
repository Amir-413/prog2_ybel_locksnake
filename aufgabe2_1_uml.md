# Aufgabe 2.1 – UML-Klassendiagramm

```mermaid
classDiagram
    direction TB

    class Main {
        +main(String[])
    }

    class GameEngine {
        -GameState gameState
        -List~Consumer~ stateObservers
        +GameEngine(Level)
        +state() GameState
        +setGamePanel(GamePanel)
        +update(Direction)
        +tick()
    }

    class GameState {
        -Level level
        -Snake snake
        -List~Pin~ pins
        -Status status
        -Direction pendingDirection
        +tick() GameState
        +level() Level
        +snake() Snake
        +pins() List~Pin~
        +status() Status
        +pendingDirection() Direction
    }

    class GamePanel {
        -GameState state
        -GameRenderer renderer
        -GameEngine gameEngine
        +update(GameState)
        +setGameEngine(GameEngine)
    }

    class GameRenderer {
        <<interface>>
        +render(Graphics2D, GameState, int)
    }

    class Java2DRenderer {
        +render(Graphics2D, GameState, int)
    }

    class Level {
        -int width
        -int height
        -CellType[][] cells
        -List~Pin~ pins
        -Position snakeStart
        +isInside(Position) boolean
        +cellAt(Position) CellType
    }

    class Snake {
        -List~Position~ body
        +head() Position
        +occupies(Position) boolean
        +grow(Direction) Snake
        +nextHead(Direction) Position
    }

    class Pin {
        -Position position
        -State state
        -Direction activationDirection
        +withState(State) Pin
        +position() Position
        +state() State
        +activationDirection() Direction
    }

    class Position {
        -int x
        -int y
        +x() int
        +y() int
        +equals(Object) boolean
    }

    class Direction {
        <<enumeration>>
        UP
        DOWN
        LEFT
        RIGHT
        NONE
        +applyTo(Position) Position
        +oppositeDirection() Direction
    }

    class CellType {
        <<enumeration>>
        EMPTY
        WALL
        PIN_SLOT
    }

    class LevelLoader {
        +loadLevelFromResource(String)$ Level
        +loadLevelFromPath(Path)$ Level
    }

    Main --> GameEngine : creates
    Main --> GamePanel : creates
    GameEngine --> GameState : manages
    GameEngine ..> GamePanel : notifies (Observer)
    GamePanel --> GameEngine : calls update()
    GamePanel --> GameRenderer : uses
    Java2DRenderer ..|> GameRenderer
    GameState --> Level : has
    GameState --> Snake : has
    GameState --> Pin : has (list)
    GameState --> Direction : pendingDirection
    Level --> Pin : has (list)
    Level --> Position : snakeStart
    Level --> CellType : cells
    Snake --> Position : body (list)
    Pin --> Position : position
    Pin --> Direction : activationDirection
    LevelLoader ..> Level : creates
```

## Beschreibung der wichtigsten Beziehungen

- **`GameEngine`** verwaltet den `GameState` und benachrichtigt `GamePanel` bei jeder Änderung (Observer-Pattern).
- **`GamePanel`** sendet Tastatureingaben als `Direction` an `GameEngine.update()` (GameEngine als Observer).
- **`GameState`** ist immutabel – `tick()` gibt immer einen neuen Zustand zurück.
- **`Java2DRenderer`** implementiert das `GameRenderer`-Interface.
- **`LevelLoader`** liest die Level-Datei ein und erzeugt ein `Level`-Objekt.
