import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Topic {

    private final String name;
    private final List<Message> messages;
    private List<Subscriber> subscribers;

    public void add(Subscriber subscriber) {
        subscribers.add(subscriber);
    }

    public void notifySubscribers() {
        for (var subscriber : subscribers) {
            subscriber.notify(this);
        }
    }

    public void receive(List<Message> messages) {
        this.messages.addAll(messages);
        notifySubscribers();
    }

    public synchronized List<Message> fetchMessages(start offset, int size) {
        if (messages)
    }
}

public class Message {

    private final String text;
}

public interface Subscriber {
    void subscribe(List<Topic> topics);
    void unsubscribe(List<Topic> topics);
    void notify(Topic topic);
}

public class Publisher {

    private final String name;

    public void publish(List<Message> message, Topic topic) {
        topic.receive(messages);
    }
}

public class AsynchConcurrentSubscriber implements Subscriber {

    private final ConcurrentSkipListSet<Subscription> subscriptions;
    private final ConcurrentHashMap<Topic, Subscription> subscriptionsByTopic;
    private final ExecutorService executorService;
    private final MessageProcessor messageProcessor;
    private int maxFetchSize;

    public SimpleSubscriber(MessageProcessor messageProcessor, int maxFetchSize) {
        this.subscriptions = new ConcurrentSkipListSet<>();
        this.subscriptionsByTopic = new ConcurrentHashMap<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.messageProcessor = messageProcessor;
        this.maxFetchSize = maxFetchSize;
    }

    @Override
    public synchronized void subscribe(List<Topic> topics) {
        for (var topic : topics) {
            topic.add(this);
            final var subscription = new Subscription(topic);
            subscriptions.add(subscription);
            subscriptionsByTopic.put(topic, subscription);
        }
    }

    @Override
    public synchronized void unsubscribe(List<Topic> topics) {}

    @Override
    public void notify(Topic topic) {
        final var future = executorService.submit(() -> {
            final var subs = subscriptionsByTopic.get(topic);
            final var lastReadOffset = subs.getOffset();
            final var newMessages = topic.fetchMessages(lastReadOffset.get(), maxFetchSize);
            lastReadOffset.incrementAndGet(newMessages.size());
            processMessages(messages);
        });
    }

    private void processMessages(List<Message> messages) {
        messageProcessor.process(messages);
    }
}

class Subscription {

    private final Topic topic;
    private final AtomicInteger offset;

    public Subscription(Topic topic) {
        this.topic = topic;
        this.offset = new AtomicInteger(-1);
    }
}

public interface MessageProcessor {
    void process(List<Message> messages);
}
