import java.util.List;

public class Parking {
    private final List<Floor> floors;

    public Parking(List<Floor> floors) {
        this.floors = List.copyOf(floors);
    }

    public Optional<Ticket> enterParking(Vehicle vehicle) {
        final var now = Instant.now();
        final var spot = findSpot(vehicle);
        if (spot.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Ticket(now, vehicle, Fee.getDefault()));
    }

    public void park(Vehicle vehicle, Spot spot) {
        vehicle.setSpot(spot);
    }

    public void unpark(Vehicle vehicle) {
        final var spot = vehicle.unsetSpot();
        final var floor = spot.getFloor();
        floors.get(floor.getNumber()).addSpot(spot);
    }

    public BigDecimal calculateFee(Ticket ticket) {
        final var now = Instant.now();
        final var parkedTime = Duration.between(vehicle.getEntryTime(), now);
        final var fee = ticket.getFee().calculateFee(parkedTime);
        return fee;
    }

    private Optional<Spot> findSpot(Vehicle vehicle) {
        for (var floor : floors) {
            final var spot = floor.getEmptySpotByType(vehicle.getType());
            if (spot.isPresent()) {
                return spot;
            }
        }
        return Optional.empty()
    }
}

public class Floor {
    private final int number;
    private final ConcurrentMap<SpotType, Set<Spot>> emptySpotsByType;
    public Floor(int number, List<Spot> spots) {
        this.emptySpotsByType = new ConcurrentHashMap<>();
        for (var spot : spots) {
            addSpot(spot);
        }
    }

    public Optional<Spot> getEmptySpotByType(SpotType type) {
        final var emptySpots = emptySpotsByType.get(type);
        if (emptySpots.isEmpty()) {
            return Optional.empty();
        }
        try {
            return emptySpots.remove(emptySpots.stream().findFirst().get());
        } catch NoSuchElementException {
            logger.warn("No empty spot found for type: {}", type);
        }
        return Optional.empty();
    }

    public void addSpot(Spot spot) {
        emptySpotsByType
            .computeIfAbsent(spot.getType(), k -> new ConcurrentSkipListSet<>())
            .add(spot);
    }
}

public class Spot {
    private final int id;
    private final SpotType type;
    private final Floor floor;
    public Spot(int id, SpotType type, Floor floor) {
        this.id = id;
        this.type = type;
        this.floor = floor;
    }
}

public class SpotType {
    CAR, MOTORCYCLE, VAN
}

public abstract class Vehicle {

    private Optional<Spot> spot;

    public abstract SpotType getType();

    public void setSpot(Spot spot) {
        this.spot = Optional.of(spot);
    }

    public void unsetSpot() {
        final var spot = this.spot;
        this.spot = Optional.empty();
        return spot;
    }
}

public class Car extends Vehicle {

    @Override
    public SpotType getType() {
        return SpotType.CAR;
    }
}

public class MotorCycle extends Vehicle {

    @Override
    public SpotType getType() {
        return SpotType.MOTORCYCLE;
    }
}

public class Van extends Vehicle {

    @Override
    public SpotType getType() {
        return SpotType.VAN;
    }
}

public class Ticket {

    private final Instant entryTime;
    private final Vehicle vehicle;
    private final Fee Fee;

    public Ticket(Instant entryTime, Vehicle vehicle, Fee Fee) {
        this.entryTime = entryTime;
        this.vehicle = vehicle;
        this.Fee = Fee;
    }
}

public abstract class Fee {

    public static Fee getDefault() {
        return new HourlyFee();
    }

    public abstract BigDecimal calculateFee(Duration parkedTime);
}

public class HourlyFee extends Fee {

    private final BigDecimal feePerHour;

    public HourlyFee(BigDecimal feePerHour) {
        this.feePerHour = feePerHour;
    }

    @Override
    public BigDecimal calculateFee(Duration parkedTime) {
        final var minutes = parkedTime.toMinutes();
        return new BigDecimal(minutes)
            .divide(60, RoundingMode.CEILING)
            .multiply(feePerHour);
    }
}

public class DailyFee extends Fee {

    private final BigDecimal feePerDay;

    public DailyFee(BigDecimal feePerDay) {
        this.feePerDay = feePerDay;
    }

    @Override
    public BigDecimal calculateFee(Duration parkedTime) {
        final var minutes = parkedTime.toMinutes();
        return new BigDecimal(minutes)
            .divide(1440, RoundingMode.CEILING)
            .multiply(feePerDay);
    }
}
