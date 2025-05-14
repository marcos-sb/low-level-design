// Problem Statement
// Design and implement a Vending Machine system that allows users to select products,
// insert coins/notes, dispense products, and return change. The system should manage
// inventory, handle payments, and use the State design pattern for its operations.

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class VendingMachine {

    private Context context;
    private final Display display;
    private final InventoryManager inventoryManager;

    public VendingMachine(Display display, InventoryManager inventoryManager) {
        this.display = display;
        this.inventoryManager = inventoryManager;
        this.context = new Context(this);
    }

    public void setDisplayText(String text) {}

    public Product getProductByCode(int productCode) {}

    public void selectProduct(int productCode) {
        context.selectProduct(productCode);
    }

    public void insertCoin(Coin coin) {
        context.insertMoney(coin.getValue());
    }

    public void insertNote(Note note) {
        context.insertMoney(note.getValue());
    }

    public Product dispenseProduct() {
        return context.dispenseProduct();
    }

    public List<Coin> returnChange() {}
}

public class Display {

    private String text;
}

class Context {

    private State state;
    private final VendingMachine vendingMachine;

    public Context(VendingMachine vendingMachine) {
        this.vendingMachine = vendingMachine;
    }

    public void selectProduct(int productCode) {
        state.selectProduct(productCode);
    }

    public void insertMoney(Value amount) {
        state.insertMoney(amount);
    }

    public Product dispenseProduct() {
        return state.dispenseProduct();
    }

    public List<Coin> returnChange() {
        return state.returnChange();
    }

    void transitionTo(State state) {
        this.state = state;
    }
}

abstract class State {

    protected final Context context;

    public State(Context context) {
        this.context = context;
    }

    abstract void selectProduct(int productCode);

    abstract void insertMoney(Value amount);

    abstract Product dispenseProduct();

    abstract List<Coin> returnChange();
}

class IdleState extends State {

    @Override
    void selectProduct(int productCode) {
        final var vm = context.getVendingMachine();
        final var p = vm.getProductByCode(productCode);
        vm.setDisplayText(p.getValue().toString());
        context.transitionTo(new ReadyState(context, p));
    }
}

class ReadyState extends State {

    private final Product product;

    public ReadyState(Context context, Product product) {
        super(context);
        this.product = product;
    }

    @Override
    void insertMoney(Value amount) {
        context.transitionTo(new InsertingMoneyState(context, product, amount));
    }
}

class InsertingMoneyState extends State {

    private final Product product;
    private final Value amount;

    public InsertingMoneyState(Context context, Product product, Value amount) {
        super(context);
        this.product = product;
        this.amount = amount;
    }

    @Override
    void insertMoney(Value _amount) {
        amount.add(_amount);
        if (amount.isGreaterThanOrEqualTo(product.getPrice())) {
            context.transitionTo(
                new DispensingProductState(context, product, amount)
            );
        }
    }
}

class DispensingProductState extends State {

    private final Product product;
    private final Value amount;

    public DispensingProductState(
        Context context,
        Product product,
        Value amount
    ) {
        super(context);
        this.product = product;
        this.amount = amount;
    }

    @Override
    Product dispenseProduct() {
        final var vm = context.getVendingMachine();
        final var im = vm.getInventoryManager();
        im.remove(product.getCode());
        amount.subtract(product.getPrice());
        context.transitionTo(new ReturningChangeState(context, amount));
        return product;
    }
}

class ReturningChangeState extends State {

    private final Value amount;

    public ReturningChangeState(Context context, Value amount) {
        super(context);
        this.amount = amount;
    }

    @Override
    List<Coin> returnChange() {
        final var vm = context.getVendingMachine();
        final var im = vm.getInventoryManager();
        final var change = im.getChange(amount);
        context.transitionTo(new IdleState(context));
        return change;
    }
}

public public class InventoryManager {

    private final ConcurrentMap<Integer, Product> code2product;
    private final ConcurrentMap<Integer, Integer> code2stock;
    private final ConcurrentNavigableMap<Coin, Integer> coin2stock;

    public InventoryManager() {
        this.code2product = new ConcurrentHashMap<>();
        this.code2stock = new ConcurrentHashMap<>();
        this.coin2stock = new ConcurrentSkipListMap<>((c1, c2) ->
            Integer.compare(c2.getValue(), c1.getValue())
        );
    }

    public Map<Integer, Product> getAllAvailableProducts() {
        return code2stock
            .stream()
            .filter(e -> 0 < e.getValue())
            .collectToMap(e -> e.getKey(), e -> e.getValue());
    }

    public synchronized void add(int code, Product product, int stock) {
        product.setCode(code);
        code2product.put(code, product);
        code2stock.put(code, code2stock.getOrDefault(code, 0) + stock);
    }

    public synchronized void remove(int code) {
        final var product = code2product.get(code);
        if (product == null) {
            throw new NoSuchElementException(
                String.format("%s", product.toString())
            );
        }
        final var stock = code2stock.get(code);
        if (stock <= 0) {
            throw new IllegalStateException(
                String.format("No stock available for %s", product.toString())
            );
        }
        code2stock.put(code, stock - 1);
        return product;
    }

    public synchronized List<Coin> getChange(Value amount) {
        final var coins = new ArrayList<Coin>();
        for (final var entry : coin2stock.entrySet()) {
            final var coin = entry.getKey();
            final var stock = entry.getValue();
            while (0 < stock && coin.getValue().isLessThanOrEqualTo(amount)) {
                coins.add(coin);
                amount.subtract(coin.getValue());
                if (stock == 1) {
                    coin2stock.remove(coin);
                } else {
                    coin2stock.put(coin, stock - 1);
                }
            }
            if (amount.isZero()) {
                break;
            }
        }
        return coins;
    }
}

public class Product {

    private final Integer code;
    private final String name;
    private final Value price;
}

public record Value(int whole, int decimal) {}

public class Coin {

    private final Value value;
}

public class Note {

    private final Value value;
}
