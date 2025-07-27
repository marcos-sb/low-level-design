import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

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
        reservation.room().status(Room.Status.AVAILABLE);
    }

    public void checkIn(Reservation reservation) {
        reservation.status(Reservation.Status.CHECKED_IN);
    }

    public void checkOut(Reservation reservation) {
        reservation.status(Reservation.Status.CHECKED_OUT);
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
    private final ConcurrentSkipListSet<Room> allRooms;

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
        for (
            var currDate = checkin;
            currDate.before(checkout);
            currDate.plus(TimeUnit.DAYS)
        ) {
            availableRoomsByDate.get(currDate).add(room);
        }
    }
}

public record Guest(String name, String email, String phone) {
    public record ID(String value) {}
}

public class PaymentProcessor {}

public record Invoice(
    Guest guest,
    Reservation reservation,
    Money totalAmount,
    Instant issuedAt
) {}
