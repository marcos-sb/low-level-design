
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

    public void gotoFloor(int currentFloor, int floor) {
        final var elevator = scheduler.getElevator(floor);
        elevator.accept(new Request(currentFloor, floor));
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

        var minResult = getMinimum(candidates);
        if (minResult != null) {
            return minResult.getMinDistanceElevator();
        }

        minResult = getMinimum(idle);
        if (minResult != null) {
            return minResult.getMinDistanceElevator();
        }

        return getMinimum(other);
    }

    private static record MinResult {
        int minDistance;
        Elevator minDistanceElevator;
    }

    private MinResult getMinimum(List<Elevator> candidates) {
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

public record Elevator (
    int currentFloor,
    Direction direction,
    ExecutorService executorService) {

    public Elevator(int bottomFloor, int topFloor) {
        this.currentFloor = 0;
        this.direction = Direction.IDLE;
        this.executorService = Executors.singleThreadExecutorService();
    }

    public void accept(Request request) {
        CompletableFuture.runAsync(new MoveTask(request) , executorService)
            .exceptionally(exc -> {
                exc.printStackTrace();
                return null;
        });
    }

    private Direction fromDelta(int delta) {
        return delta < 0 ? Direction.DOWN : 0 < delta ? Direction.UP : Direction.IDLE;
    }

    private void moveBy(int unit) {
        if (direction.DOWN) {
            --currentFloor;
        } else if (direction.UP) {
            ++currentFloor;
        }
    }

    private static class MoveTask implements Runnable {
        private final Request request;

        private MoveTask(Request request) {
            this.request = request;
        }

        @Override
        public void run() {
            final var deltaToFrom = from - currentFloor;
            this.direction = fromDelta(deltaToFrom);
            while (currentFloor != from) {
                Thread.sleep();
                move();
            }

            final var deltaToTo = to - currentFloor;
            this.direction = fromDelta(deltaToTo);
            while (currentFloor != to) {
                Thread.sleep();
                move();
            }
        }
    }
}

public enum Direction {
    UP, DOWN, IDLE
}

public record Request (int from, int to);
