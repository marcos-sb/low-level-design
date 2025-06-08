public record Card {
    private final int number;
    private final PIN pin;
}

public record PIN {
    private final List<Integer> pin;
}

public interface Authenticator {
    boolean authenticate(Card card, PIN pin);
}

public interface OperationManager {
    int balanceInquiry(User user);
    boolean cashWithdrawal(Money amount);
    boolean cashDeposit(Money amount);
}

public record Money {
    private final int whole;
    private final int decimal;
    private final Currency currency;
}

public enum Currency {
    EUR, USD, GBP
}

public abstract class Transaction {
    private final Account account;
    private final Money amount;
}

public class DepositTransaction extends Transaction {}

public class WithdrawalTransaction extends Transaction {
    public WithdrawalTransaction(Account account, Money amount) {
        super(account, amount);
    }
}

public interface BankingService implements Authenticator {
    @Override
    void authenticate(Card card, int[] pin) throws AuthenticationException;

    void validAccount(Card card, Account account) throws InvalidAccountException;

    void execute(Transaction transaction) throws ExecutionException;
}

class CashDispenser {
    private final ConcurrentMap<Note, Integer> cashInventory;

    public CashDispenser() {
        this.cashInventory = new ConcurrentHashMap<>();
    }

    boolean dispenseCash(Money amount) {}
}

public record Note {
    private final int value;
    private final Currency currency;
}

public class UI {
    private final ATM atm;
    private final List<Integer> digits;

    public UI(ATM atm) {
        this.atm = atm;
        this.digits = new ArrayList<>();
    }

    private void typeDigit(int digit) {
        // TODO: validate 0 <= digit <= 9
        digits.add(digit);
    }

    private void deleteLastDigit() {
        digits.remove(digits.size()-1);
    }

    private void acknowledgePIN() {
        atm.insertPIN(new PIN(digits));
    }
}

public interface TransactionManager {
    void recordTransaction(Transaction transaction);
}

public class Ledger implements TransactionManager {
    private final List<Transaction> transactions;

    @Override
    public void recordTransaction(Transaction transaction) {
        transactions.add(transaction);
    }
}

public class ATM implements OperationManager {
    private final Ledger ledger;
    private final BankingService bankingService;
    private final CashDispenser cashDispenser;
    private AtomicBoolean authenticated;

    @Setter
    private UI ui;
    private Optional<Card> card;
    private Optional<PIN> pin;

    public ATM(Ledger ledger, BankingService bankingService, CashDispenser cashDispenser) {
        this.ledger = ledger;
        this.bankingService = bankingService;
        this.cashDispenser = cashDispenser;
        this.ui = ui;
        this.authenticated = new AtomicBoolean(false);
        this.card = Optional.empty();
        this.pin = Optional.empty();
    }

    public void insertCard(Card card) {
        this.card = Optional.of(card);
        ui.loadPinScreen();
    }

    public void insertPin(PIN pin) {
        // Validate pin
        this.pin = Optional.of(pin);
    }

    private boolean authenticate(Card card, PIN pin) throws AuthenticationException {
        if (!authenticated.get()) {
            var tryAuth = false;
            try {
                tryAuth = bankingService.authenticate(card, pin);
            } catch (AuthenticationException ex) {
                if (!tryAuth) {
                    throw new AuthenticationException("", ex);
                }
            }
            authenticated.set(true);
        }
        return true;
    }

    public List<Account> listAccounts() {
        authenticate(this.card, this.pin);
        return bankingService.listAccounts(this.card);
    }

    public int balanceInquiry(Account account) throws AuthenticationException {
        authenticate(this.card, this.pin);
        return bankingService.balanceInquiry(Account account);
    }

    public boolean cashWithdrawal(Account account, Money amount) throws Exception {
        authenticate(this.card, this.pin);
        final var tx = new WithdrawalTransaction(account, amount);
        bankingService.validAccount(this.card, account);
        bankingService.execute(tx);
        bankingService.recordTransaction(tx);
        cashDispenser.dispense(amount);
        return true;
    }

    public boolean cashDeposit(Money amount) {}
}
