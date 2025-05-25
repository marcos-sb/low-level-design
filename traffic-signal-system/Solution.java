// Multiple Directions: The intersection supports multiple directions (e.g., NORTH, SOUTH, EAST, WEST).
// Traffic Light States: Each direction has a traffic light with states: GREEN, YELLOW, RED.
// Configurable Durations: Each direction and state can have its own configurable duration.
// Automatic Cycling: The system automatically cycles through the states for each direction in a round-robin fashion.
// Manual Override: The system allows manual override to set a specific direction to GREEN at any time.
// Extensibility: Easy to add new directions or states if needed.
// State Pattern: Use the State design pattern to encapsulate state-specific behavior and transitions.

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrafficLight implements Subject {

    private final Context context;
    private final Direction direction;
    private final List<Observer> observers;

    public void setState(State state) {
        context.setState(state);
    }

    @Override
    public void notifyAll() {
        for (var obs : observers) {
            obs.updated(this);
        }
    }
}

public interface Subject {
    void notifyAll();
}

public class Context {

    private volatile State state;
    private final TrafficLight trafficLight;
    private final IntersectionManager intersectionManager;

    public void scheduleTransition(Duration delay, State newState) {
        intersectionManager.scheduleTransition(delay, trafficLight, newState);
    }

    void setState(State state) {
        this.state = state;
        state.onStateSet();
        trafficLight.notifyAll();
    }

    Yellow getYellow();
}

public abstract class State {

    private final Context context;
    protected final Duration duration;

    public abstract void open();

    public abstract void close();

    public abstract boolean isOpen();

    public abstract boolean isClosed();

    abstract void onStateSet();
}

public class Green implements State {

    @Override
    public void open() {}

    @Override
    public void close() {
        final var context = getContext();
        context.setState(context.getRed());
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    void onStateSet() {
        context.scheduleTransition(duration, context.getYellow());
    }
}

public class Yellow implements State {

    public Yellow(Duration duration, Context context) {
        super(duration, context);
    }

    @Override
    void onStateSet() {
        context.scheduleTransition(duration, context.getRed());
    }
}

public class Red implements State {

    @Override
    public void open() {
        final var context = getContext();
        final var newState = context.getGreen();
        context.setState(newState);
    }

    @Override
    void onStateSet() {}
}

public enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST,
}

public class IntersectionManager implements Observer {

    private final EnumMap<Direction, TrafficLight> trafficLights;
    private Strategy strategy;
    private final ScheduledExecutorService executor;

    public IntersectionManager() {
        trafficLights = new EnumHashMap<>(Direction.class);
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void scheduleTransition(
        Duration delay,
        TrafficLight trafficLight,
        State newState
    ) {
        executor.schedule(
            () -> {
                trafficLight.setState(newState);
            },
            delay.toSeconds(),
            TimeUnit.SECONDS
        );
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

    private final List<Direction> directions;
    private final EnumMap<Direction, Direction> next;

    public RoundRobin(List<Direction> available) {
        this.directions = List.copyOf(available);
        this.next = new EnumHashMap<>(Direction.class);
        if (directions.size() == 1) {
            final var uniqueDirection = directions.get(0);
            next.put(uniqueDirection, uniqueDirection);
            return;
        }
        for (var i = 1; i < directions.size(); ++i) {
            next.put(directions.get(i - 1), directions.get(i));
        }
        next.put(directions.size() - 1, directions.get(0));
    }

    @Override
    public Direction next(Direction closingDirection) {
        return next.get(closingDirection);
    }
}
