import java.awt.List;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserManager {

    void register(User user, String password) throws RegistrationException {}

    LoggedInUser login(User user) throws LoginException {}
}

public abstract class User {

    private final String name;
    private final String uname;

    protected abstract void receiveConnectionRequest(User sender);

    protected abstract void acceptedConnectionRequest(User user);

    protected abstract void removePendingConnectionRequest(User user);
}

public class LoggedInUser extends User {

    private final Profile profile;
    private final ConcurrentSkipListSet<User> connections;

    @Getter
    private final ConcurrentSkipListSet<
        PendingConnection
    > pendingConnectionRequests;

    @Getter
    private final List<JobPost> jobPosts;

    private final ConcurrentHashMap<User, List<Message>> threadsByUser;
    private final List<Notification> notifications;

    public LoggedInUser() {
        this.pendingConnectionRequests = new ConcurrentSkipListSet<>(
            (pc1, pc2) -> pc1.compareTo(pc2)
        );
        this.jobPosts = Collections.synchronizedList(new ArrayList<>());

        final var pendingNotifications = NotificationsManager.getInstance().getNotifications(this);
        this.notifications = pendingNotifications.isEmpty()
            ? Collections.synchronizedList(new ArrayList<>())
            : Collections.synchronizedList(pendingNotifications);
    }

    public boolean add(Education education) {}

    public boolean remove(Education education) {}

    public boolean add(Experience experience) {}

    public boolean remove(Experience experience) {}

    public boolean add(Skill skill) {}

    public boolean remove(Skill skill) {}

    public void sendConnectionRequest(User user) {
        user.receiveConnectionRequest(this);
    }

    public synchronized void acceptConnectionRequest(User user) {
        if (!pendingConnectionRequests.contains(user)) {
            throw new IllegalStateException();
        }
        user.acceptedConnectionRequest(this);
        user.removePendingConnectionRequest(this);
        this.acceptedConnectionRequest(user);
        this.receiveConnectionRequest(user);
    }

    @Override
    protected void receiveConnectionRequest(User user) {
        pendingConnectionRequests.add(user);
    }

    @Override
    protected void acceptedConnectionRequest(User user) {
        connections.add(user);
    }

    @Override
    protected void removePendingConnectionRequest(User user) {
        if (!pendingConnectionRequests.contains(user)) {
            throw new IllegalStateException();
        }
        pendingConnectionRequests.remove(user);
    }

    public JobPost post(JobPost jobPost) {}

    public void send(Message message, User user) {
        if (!connections.contains(user)) {
            throw new IllegalStateException();
        }
        user.receive(message, this);
        this.receive(message, this);
    }

    @Override
    protected void receive(Message message, User user) {
        if (!connections.contains(user)) {
            throw new IllegalStateException();
        }
        threadsByUser.computeIfAbsent(user, k -> new ArrayList<>())
            .add(message);
    }
}

public class Profile {

    private final List<Education> education;
    private final List<Experience> experience;
    private final Set<Skill> skills;
}

public class Education {}

public class Experience {}

public class Skill {

    private final String name;
}

public class JobPost {

    private final String title;
    private final String description;
    private final User owner;
}

public record Message {
    private final String body;
    private final List<Attachment> attachments;
}

public class Attachment {}

public class NotificationManager {
    private final ConcurrentMap<User, Boolean> listeners;
    private final ConcurrentMap<User, List<Notification>> pendingNotificationsByUser;

    public void newMessageTo(User user) {
        handleNotification(user, new MessageNotification());
    }

    public void newConnectionTo(User user) {
        handleNotification(user, new ConnectionNotification());
    }

    private void handleNotification(User user, Notification notification) {
        if (listeners.containsKey(user)) {
            user.addNotification(notification);
        } else {
            pendingNotificationsByUser.computeIfAbsent(user, k -> new ArrayList<>())
                .add(notification);
        }
    }

    public void addListener(User user) {
        listeners.put(user, true);
    }

    public List<Notification> getPendingNotifications(User user) {
        if (!pendingNotificationsByUser.containsKey(user)) {
            return List.of();
        }
        return pendingNotificationsByUser.remove(user);
    }
}

public abstract class Notification {}

public class MessageNotification {}
public class ConnectionRequest {}
