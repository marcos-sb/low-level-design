import java.time.LocalDate;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public class Room {

    private String number;
    private Set<Feature> features;
    private Type type;
    private Money rate;

    public void add(Feature feature) {
        featutes.add(feature);
    }

    public void setRate(Money rate) {
        this.rate = rate;
    }

    public static enum Type {
        STANDARD,
        DELUXE,
        SUITE,
    }
}

public record Feature(String name, String description) {}

public class System {

    private final ConcurrentSkipListSet<Reservation> reservations;
    private final ConcurrentMap<Guest.ID, Guest> guests;
    private final ConcurrentMap<
        Guest.ID,
        ConcurrentSkipListSet<Reservation>
    > historyByGuest;
    private final Inventory inventory;

    public Guest newGuest(String name, String email, String phone) {
        final var guest = new Guest(name, email, phone);
        guests.put(guest.id(), guest);
        return guest;
    }

    public Reservation createReservation(
        Guest guest,
        Room room,
        LocalDate checkin,
        LocalDate checkout
    ) {
        if (!room.available) {
            throw new IllegalStateException("Room is not available");
        }
        if (!guests.containsKey(guest)) {
            throw new IllegalArgumentException("Guest not registered");
        }

        final var reservation = new Reservation(guest, room, checkin, checkout);
        synchronized (this) {
            reservations.add(reservation);
            historyByGuest.get(guest).add(reservation);
            reservation.room().status(Room.Status.BOOKED);
        }

        return reservation;
    }

    public void cancelReservation(Reservation reservation) {
        if (reservation.status() != Reservation.Status.CONFIRMED) {
            throw new IllegalStateException("Reservation cannot be cancelled");
        }
        reservation.status(Reservation.Status.CANCELLED);
        inventory.makeAvailable(
            reservation.room(),
            reservation.checkin(),
            reservation.checkout()
        );
    }

    public void checkIn(Reservation reservation) {
        reservation.status(Reservation.Status.CHECKED_IN);
    }

    public void checkOut(Reservation reservation) {
        reservation.status(Reservation.Status.CHECKED_OUT);
    }

    public void takeRoomForMaintenance(Room room) {
        inventory.takeRoomForMaintenance(room);
    }
}

public record Reservation(
    Guest guest,
    Room room,
    LocalDate checkin,
    LocalDate checkout,
    Status status
) {
    public static enum Status {
        CONFIRMED,
        CANCELLED,
        CHECKED_IN,
        CHECKED_OUT,
    }
}

public class Inventory {

    private final ConcurrentMap<
        LocalDate,
        ConcurrentSkipListSet<Room>
    > availableRoomsByDate;
    private LocalDate latestDateInAvailableRoomsByDate;
    private final ConcurrentSkipListSet<Room> allRooms;
    private final ConcurrentSkipListSet<Room> roomsInMaintenance;

    public synchronized void addRoom(Room room) {
        allRooms.add(room);
        for (var entry : availableRoomsByDate.entrySet()) {
            entry.getValue().add(room);
        }
    }

    public synchronized void bookRoom(
        Room room,
        LocalDate checkin,
        LocalDate checkout
    ) {
        if (roomsInMaintenance.contains(room)) {
            throw new IllegalStateException("Room is under maintenance");
        }
        for (
            var currDate = checkin;
            currDate.before(checkout);
            currDate.plus(TimeUnit.DAYS)
        ) {
            availableRoomsByDate.get(currDate).remove(room);
        }
    }

    public synchronized void makeAvailable(
        Room room,
        LocalDate checkin,
        LocalDate checkout
    ) {
        if (!allRooms.contains(room)) {
            throw new NoSuchElement("Room not found in inventory");
        }
        for (
            var currDate = checkin;
            currDate.before(checkout);
            currDate.plus(TimeUnit.DAYS)
        ) {
            availableRoomsByDate.get(currDate).add(room);
        }
    }

    public synchronized void takeRoomForMaintenance(Room room) {
        if (!allRooms.contains(room)) {
            throw new NoSuchElement("Room not found in inventory");
        }
        if (roomsInMaintenance.contains(room)) return;
        for (var entry : availableRoomsByDate.entrySet()) {
            entry.getValue().remove(room);
        }
        roomsInMaintenance.add(room);
    }

    public synchronized void extendInventory(LocalDate endDate) {
        for (
            var currDate = latestDateInAvailableRoomsByDate;
            currDate.before(endDate);
            currDate.plus(TimeUnit.DAYS)
        ) {
            availableRoomsByDate.putIfAbsent(
                currDate,
                new ConcurrentSkipListSet<>()
            );
        }
        latestDateInAvailableRoomsByDate = endDate;
    }

    public synchronized void pruneInventory(LocalDate startDate) {
        if (startDate.isAfter(latestDateInAvailableRoomsByDate)) {
            availableRoomsByDate.clear();
            return;
        }
        for (var date : availableRoomsByDate.keySet()) {
            if (date.isBefore(startDate)) {
                availableRoomsByDate.remove(date);
            }
        }
        latestDateInAvailableRoomsByDate = startDate;
    }
}

public record Guest(String name, String email, String phone) {
    public record ID(String value) {}
}

public class PaymentProcessor {

    public Invoice settlePayment(
        Reservation reservation,
        PaymentMethod paymentMethod
    ) {
        final var totalAmount = calculateTotal(reservation);
        final var issuedAt = Instant.now();
        final var invoice = new Invoice(
            reservation.guest(),
            reservation,
            totalAmount,
            issuedAt,
            paymentMethod
        );

        try {
            paymentMethod.charge(totalAmount);
        } catch (PaymentException pe) {
            throw new IllegalStateException("Payment failed", pe);
        }

        return invoice;
    }

    private Money calculateTotal(Reservation reservation) {
        final var days = ChronoUnit.DAYS.between(
            reservation.checkin(),
            reservation.checkout()
        );
        return reservation.room().rate().multiply(days);
    }
}

public record Invoice(
    Guest guest,
    Reservation reservation,
    Money totalAmount,
    Instant issuedAt,
    PaymentMethod paymentMethod
) {}

public interface PaymentMethod {
    void charge(Money amount);
}

public class CreditCardPayment implements PaymentMethod {

    private final String cardNumber;
    private final String cardHolderName;
    private final String expiryDate;
    private final String cvv;

    @Override
    public void charge(Money amount) throws PaymentException {}
}

public class BankTransferPayment implements PaymentMethod {

    private final String bankAccountNumber;
    private final String bankName;

    @Override
    public void charge(Money amount) throws PaymentException {}
}

public class PaymentException extends Exception {

    public PaymentException(String message) {
        super(message);
    }
}
