import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;

public class User {
    private final String name;
    private final String address;
    private final String phoneNumber;
    private final DriversLicenseInfo dli;
}

public class DriversLicenseInfo {

}

public class System {
    private final ConcurrentMap<User, Reservation> reservationsByUser;
    private final Inventory inventory;

    public System() {
        this.reservationsByUser = new ConcurrentHashMap<>();
        this.inventory = new Inventory();
    }

    public List<Car> search(List<Filter> filters, int limit) {
        return search(filters, limit);
    }

    public Reservation reserve(User user, Car car, Date start, Date end) {
        synchronized (this) {
            if (!inventory.available(start, end, car)) {
                throw new IllegalStateException();
            }
            final var days = end - start;
            final var totalPrice =
            new Reservation(user, car, start, end, totalPrice);
        }
    }

    public void modify(Reservation reservation) {}

    public void cancel(Reservation reservation) {}
}

public class Inventory {
    private final ConcurrentSkipListSet<Car> allCars;
    private final ConcurrentMap<Date, Set<Car>> reservedCarsByDate;

    public Inventory() {
        this.allCars = new ConcurrentSkipListSet<>();
        this.reservedCarsByDate = new ConcurrentHashMap<>();
    }

    public List<Car> search(List<Filter> filters, int limit) {
        final var filtered = new ArrayList<Car>(limit);
        OUTER:
        for (var c : inventory.getAllCars()) {
            for (var p : predicates) {
                if (!p.test(c)) continue OUTER;
            }
            filtered.add(c);
            if (limit <= filtered.size()) break;
        }
        return filtered;
    }

    public boolean available(Date start, Date end, Car car) {
        for (var d : between(start, end)) {
            if (reservedCarsByDate.getOrDefault(d, Set.of()).contains(car))
                return false;
        }
        return true;
    }

    private List<Date> between(Date start, Date end) {}
}

public class Car {

    private final Type type;
    private final String make;
    private final String model;
    private final int year;
    private final String licensePlate;
    private final boolean available;

    private Money pricePerDay;
}

public enum Type {

}

public record Money(int wholePart, final int decimalPart) {}

public interface Filter implements Predicate<Car> {
    @Override
    boolean test(Car car);
}

public class CarTypeFilter implements Filter {
    public CarTypeFilter(Type type) {

    }
    @Override
    boolean test(Car car) {

    }
}

public class PriceRangeFilter implements Filter {
    public PriceRangeFilter(Money from, Money to) {

    }
    @Override
    boolean test(Car car) {

    }
}

public class AvailableFilter implements Filter {
    public AvailableFilter(Date start, Date end, Inventory inventory) {

    }

    @Override
    boolean test(Car car) {
        return inventory.available(start, end, car);
    }
}

public record Reservation(
    private final User user,
    private final Car car,
    private final Date startDate,
    private final Date endDate,
    private final Money totalPrice) {

}
