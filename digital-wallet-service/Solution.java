import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class UserManager {

    private final ConcurrentHashMap<String, User> usersByEmail;

    public synchronized User registerUser(
        String email,
        String name,
        String password
    ) {
        final var user = new User(name, email, password);
        usersByEmail.put(email, user);
        return user;
    }

    public synchronized User getUserByEmail(String email, String password) {
        if (!usersByEmail.containsKey(email)) {
            throw new IllegalArgumentException("Incorrect email or password");
        }
        final var user = usersByEmail.get(email);
        if (!user.clearPassword().equals(password)) {
            throw new IllegalArgumentException("Incorrect email or password");
        }
        return user;
    }
}

public class WalletSystem {

    private final ConcurrentMap<
        User,
        ConcurrentSkipListSet<Account>
    > accountsByUser;
    private final ConcurrentMap<
        User,
        ConcurrentSkipListSet<PaymentMethod>
    > paymentMethodsByUser;
    private final ConcurrentMap<
        User,
        ConcurrentSkipListSet<Transaction>
    > transactionsByUser;

    public Account openAccount(User user, Money initialBalance) {
        final var account = new Account(user, initialBalance);
        accountsByUser
            .computeIfAbsent(user, k -> new ConcurrentSkipListSet<>())
            .add(account);
        return account;
    }

    public void addPaymentMethod(User user, PaymentMethod paymentMethod) {
        final var accounts = accountsByUser.get(user);
        if (!accounts.contains(paymentMethod.source())) {
            throw new IllegalArgumentException(
                "Payment method source account does not belong to the user"
            );
        }
        paymentMethodsByUser
            .computeIfAbsent(user, k -> new ConcurrentSkipListSet<>())
            .add(paymentMethod);
    }

    public void transfer(
        User user,
        PaymentMethod paymentMethod,
        Money amount,
        Account destination
    ) {
        final var paymentMethods = paymentMethodsByUser.get(user);
        if (!paymentMethods.contains(paymentMethod)) {
            throw new IllegalArgumentException(
                "Payment method does not belong to the user"
            );
        }
        final var sourceAccount = paymentMethod.source();
        final var transaction = new Transaction(
            paymentMethod,
            destination,
            amount,
            Instant.now(TimeZone.getDefault().toZoneId())
        );

        final var processedTransaction =
            TransactionProcessor.getInstance().process(transaction);

        transactionsByUser
            .computeIfAbsent(user, k ->
                new ConcurrentSkipListSet<Transaction>()
            )
            .add(processedTransaction);
    }
}

public class Account {

    private final User owner;
    private final Money balance;
}

public class User {

    private final String name;
    private final String email;
    private final String clearPassword;
}

public abstract class PaymentMethod {

    private final User owner;
    private final Account source;
}

public class CreditCard extends PaymentMethod {

    private final String cardNumber;
    private final String cardHolderName;
    private final LocalDate expiryDate;
    private final String cvv;
}

public class BankAccount extends PaymentMethod {}

interface Transaction {
    PaymentMethod paymentMethod();
    Account destination();
    Money amount();
    Instant timestamp();

    default boolean isFailed() {
        return false;
    }
}

public class SimpleTransaction implements Transaction {

    private final PaymentMethod paymentMethod;
    private final Account destination;
    private final Money amount;
    private final Instant timestamp;
}

public class FailedTransaction implements Transaction {

    private final SimpleTransaction transaction;
    private final Exception reason;

    public FailedTransaction(SimpleTransaction transaction, Exception reason) {
        this.transaction = transaction;
        this.reason = reason;
    }

    @Override
    public boolean isFailed() {
        return true;
    }
}

public class TransactionProcessor {

    public static class HOLDER {

        private static final TransactionProcessor INSTANCE =
            new TransactionProcessor();
    }

    public static TransactionProcessor getInstance() {
        return HOLDER.INSTANCE;
    }

    public Transaction process(Transaction transaction) {
        var loggeableTransaction = transaction;
        try {
            doProcess(walletSystem, transaction);
        } catch (Exception e) {
            loggeableTransaction = new FailedTransaction(transaction, e);
        }
        return loggeableTransaction;
    }

    private void doProcess(Transaction transaction) {
        final var sourceAccount = transaction.paymentMethod().source();
        final var destinationAccount = transaction.destination();
        final var amount = transaction.amount();

        synchronized (sourceAccount) {
            if (sourceAccount.balance().compareTo(amount) < 0) {
                throw new IllegalArgumentException(
                    "Insufficient funds in source account"
                );
            }

            sourceAccount.withdraw(amount);
            destinationAccount.deposit(amount);
        }
    }
}
