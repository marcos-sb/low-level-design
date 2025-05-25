// Multiple Directions: The intersection supports multiple directions (e.g., NORTH, SOUTH, EAST, WEST).
// Traffic Light States: Each direction has a traffic light with states: GREEN, YELLOW, RED.
// Configurable Durations: Each direction and state can have its own configurable duration.
// Automatic Cycling: The system automatically cycles through the states for each direction in a round-robin fashion.
// Manual Override: The system allows manual override to set a specific direction to GREEN at any time.
// Extensibility: Easy to add new directions or states if needed.
// State Pattern: Use the State design pattern to encapsulate state-specific behavior and transitions.

import java.util.EnumMap;

public class TrafficLight implements Subject {

    private final Context context;
    private final Direction direction;
    private final List<Observer> observers;

    @Override
    public void notifyAll() {}
}

public interface Subject {
    void notifyAll();
}

public class Context {

    private State state;
    private final TrafficLight trafficLight;

    void setState(State state) {
        this.state = state;
    }
}

public abstract class State {

    private final Context context;
    protected final Duration duration;

    public abstract void open();

    public abstract void close();

    public abstract boolean isOpen();

    public abstract boolean isClosed();
}

public class Green implements State {
    @Override
    public void open() {
        final var
    }

    @Override
    public void close() {
        final var context = getContext();
        context.setState(context.getYellow());
    }

    @Override
    public boolean isClose() {
        return false;
    }
}

public class Yellow implements State {
    public Yellow(Duration duration, Context context) {
        super(duration, context);
    }
}

public class Red implements State {}

public enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST,
}

public class IntersectionManager implements Observer {

    private final EnumMap<Direction, TrafficLight> trafficLights;
    private Strategy strategy;

    public IntersectionManager() {
        trafficLights = new EnumHashMap<>(Direction.class);
    }

    public void setNorth(TrafficLight trafficLight) {}

    public void start() {
        trafficLights.get(strategy.next()).open();
    }

    @Override
    void updated(TrafficLight trafficLight) {
        if (trafficLight.isClosed()) {
            final var nextToOpen = strategy.next(trafficLight);
            nextToOpen.open();
        }
    }
}

public interface Observer {
    void updated(TrafficLight trafficLight);
}

public interface Strategy {
    Direction next();
    Direction next(Direction closingDirection);
}

public class RoundRobin implements Strategy {

    public RoundRobin(List<Direction> available) {}

    @Override
    public Direction next(Direction closingDirection) {
        return switch (closingDirection) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }
}
