public interface Observer {
    void update(Auction auction);
}

public class User implements Observer {

    private final String email;
    private final String password;

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String email() {
        return email;
    }

    public String password() {
        return password;
    }

    @Override
    public void update(Auction auction) {}
}

public class Item {

    private final ID id;

    public ID id() {
        return id;
    }

    public static class ID {

        String value;
    }
}

public class UserManager {

    private final ConcurrentMap<String, User> users;

    public UserManager(ConcurrentHashMap<String, User> users) {
        this.users = users;
    }

    public UserManager() {
        this(new ConcurrentHashMap<>());
    }

    public User register(String email, String password) {
        final var newUser = new User(email, password);
        users.put(newUser.email(), newUser);
        return newUser;
    }

    public User login(String email, String password) {
        if (!users.containsKey(email)) throw new IllegalArgumentException(
            String.format("User not found or wrong password for email", email)
        );

        final var user = users.get(email);
        if (user.password() != password) {
            throw new IllegalArgumentException(
                String.format(
                    "User not found or wrong password for email",
                    email
                )
            );
        }

        return user;
    }
}

public class System {

    private final ConcurrentMap<Item.ID, Item> items;
    private final ConcurrentSkipListSet<Auction> auctions;
    private final ScheduledExecutorService scheduler;

    public System(
        ConcurrentHashMap<Item.ID, Item> items,
        ConcurrentSkipListSet<Auction> auctions,
        ScheduledExecutorService scheduler
    ) {
        this.items = items;
        this.auctions = auctions;
        this.scheduler = scheduler;
    }

    public System() {
        this(
            new ConcurrentHashMap<>(),
            new ConcurrentSkipListSet<>(),
            Executors.newScheduledThreadPool(1)
        );
    }

    public Auction createAuction(Item item, User user) {
        if (items.containsKey(itemId)) {
            throw new IllegalStateException("Item already in auction");
        }

        final var startTime = Instant.now();
        final var endTime = startTime.plusSeconds(3600); // 1 hour auction
        final var startingPrice = "10.00"; // Default starting price
        final var minPrice = "50.00"; // Default minimum price

        final var auction = new Auction(
            item,
            user,
            startTime,
            endTime,
            startingPrice,
            minPrice
        );

        synchronized (this) {
            if (auctions.contains(auction)) {
                throw new IllegalStateException("Auction already exists");
            }
            if (items.containsKey(item)) {
                throw new IllegalStateException("Item already in auction");
            }
            items.put(item.id(), item);
            auctions.add(auction);
            final var delay = Duration.between(Instant.now(), endTime);
            scheduler.schedule(
                Auction::end,
                delay.getSeconds(),
                TimeUnit.Seconds
            );
        }

        return auction;
    }

    public List<Auction> getAuctions() {
        return new ArrayList<>(auctions);
    }

    public void placeBid(Auction auction, User user, String amount) {
        if (!auction.isActive()) {
            throw new IllegalStateException(
                "Cannot place bid on inactive auction"
            );
        }

        // Or add a validate field to Auction class
        if (amount.compareTo(auction.minPrice()) < 0) {
            throw new IllegalArgumentException(
                "Bid amount is below minimum price"
            );
        }

        final var bid = new Bid(user, amount, Instant.now());

        auction.bids().add(bid);
    }

    public follow(Auction auction, User user) {
        auction.follow(user);
    }
}

public class Auction {

    private final Item item;
    private final User owner;
    private final Instant startTime;
    private final Instant endTime;
    private final Auction.Status status;
    private final String startingPrice;
    private final String minPrice;

    private final ConcurrentSkipListSet<Bid> bids;
    private final List<Observer> observers;

    public Auction(
        Item item,
        User owner,
        Instant startTime,
        Instant endTime,
        String startingPrice,
        String minPrice
    ) {
        this.item = item;
        this.owner = owner;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = startTime.isBefore(Instant.now())
            ? Status.ACTIVE
            : Status.NOT_STARTED;

        this.startingPrice = startingPrice;
        this.minPrice = minPrice;
        this.bids = new ConcurrentSkipListSet<>((b1, b2) ->
            b1.amount().compareTo(b2.amount())
        );
        this.observers = Collections.synchronizedList(new ArrayList<>());
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public synchronized void end() {
        if (Instant.now().isBefore(endTime)) {
            throw new IllegalStateException(
                "Auction cannot be ended before its end time"
            );
        }
        this.status = Status.ENDED;
    }

    public void follow(Observer observer) {
        observers.add(observer);
    }

    private void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(this);
        }
    }

    public static enum Status {
        NOT_STARTED,
        ACTIVE,
        ENDED,
    }
}

public class Bid {

    private final User user;
    private final String amount;
    private final Instant timestamp;
}
