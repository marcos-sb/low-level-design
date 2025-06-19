
public class System {
    private final List<Elevator> elevators;
    private Scheduler scheduler;

    private System() {
        this.elevators = Collections.synchronizedList(new ArrayList<>());
        this.scheduler = new LeastDistanceScheduler(this);
    }

    public add(Elevator elevator) {
        elevators.add(elevator);
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void gotoFloor(int floor) {
        final var elevator = scheduler.getElevator(floor);
        elevator.accept(new Request(floor));
    }
}

public interface Scheduler {
    Elevator getElevator(int floor);
}

public class LeastDistanceScheduler implements Scheduler {
    private System system;

    public LeastDistanceScheduler(System system) {
        this.system = system;
    }

    @Override
    public Elevator getElevator(int floor) {
        final var elevators = system.getElevators();
        final var candidates = new ArrayList<Elevator>();
        final var idle = new ArrayList<Elevator>();
        final var other = new ArrayList<Elevator>();

        for (var elevator : elevators) {
            final var destinationDirection = elevator.getCurrentFloor() - floor < 0
                ? Direction.DOWN
                : Direction.UP;
            if (elevator.getDirection() == destinationDirection) {
                candidates.add(elevator);
            } else if (elevator.getDirection() == IDLE) {
                idle.add(elevator);
            } else {
                other.add(elevator);
            }
        }

        var minResult = getMinima(candidates);
        if (minResult != null) {
            return minResult;
        }

        minResult = getMinima(idle);
        if (minResult != null) {
            return minResult;
        }

        return getMinima(other);
    }

    private static record MinResult {
        int minDistance;
        Elevator minDistanceElevator;
    }

    private MinResult getMinima(List<Elevator> candidates) {
        var minDistance = Integer.MAX_VALUE;
        var minDistanceElevator = (Elevator) null;
        for (var elevator : candidates) {
            final var d = Math.abs(elevator.getDestinationFloor() - floor);
            if (d < minDistance) {
                minDistance = d;
                minDistanceElevator = elevator;
            }
        }
        if (minDistanceElevator != null) {
            return new MinResult(minDistance);
        }
        return null;
    }
}

public record Elevator {
    private volatile int currentFloor;
    private volatile int destinationFloor;
    private Direction direction;
    private final ExecutorService executorService;

    public Elevator(int bottomFloor, int topFloor) {
        this.currentFloor = 0;
        this.destinationFloor = 0;
        this.direction = Direction.IDLE;
        this.executorService = Executors.singleThreadExecutorService();
        this.pendingFloorStops = boolean[topFloor - bottomFloor + 1];
    }

    public void accept(Request request) {
        final var _floor = request.getFloor();
        CompletableFuture.runAsync(() -> {
            if (direction == IDLE) {
                destinationFloor = _floor;
                if (_floor < currentFloor) {
                    moveDown();
                }
            }
        }, executorService).exceptionally(exc -> {
            logger.error(exc);
            return null;
        });
    }
}

public enum Direction {
    UP, DOWN, IDLE
}

public class Request {
    public void gotoFloor(int floor) {

    }
}
