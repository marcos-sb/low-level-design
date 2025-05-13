// Problem Statement
// Design and implement a Vending Machine system that allows users to select products,
// insert coins/notes, dispense products, and return change. The system should manage
// inventory, handle payments, and use the State design pattern for its operations.

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
        context.insertMoney(note.getValue())
    }
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

    void transitionTo(State state) {
        this.state = state;
    }
}

abstract class State {
    protected final Context context;

    abstract void selectProduct(int productCode);
    abstract void insertMoney(Value amount);
}

class ReadyState extends State {

    @Override
    void selectProduct(int productCode) {
        final var vm = context.getVendingMachine();
        final var p = vm.getProductByCode(productCode);
        vm.setDisplayText(p.getValue().toString());
    }

    @Override
    void insertMoney(Value amount) {
        context.transitionTo(new InsertingMoneyState(context, amount));
    }
}

class InsertingMoneyState extends State {
    private final Value amount;

}
class DispensingProductState extends State {}
class ReturningChangeState extends State {}

public public class InventoryManager {

    private final ConcurrentMap<Integer, Product> code2product;
    private final ConcurrentMap<Integer, Integer> code2stock;

    public InventoryManager() {
        this.code2product = new ConcurrentHashMap<>();
        this.code2stock = new ConcurrentHashMap<>();
    }

    public Map<Integer, Product> getAllAvailableProducts() {
        return code2stock
            .stream()
            .filter(e -> 0 < e.getValue())
            .collectToMap(e -> e.getKey(), e -> e.getValue());
    }

    public synchronized void add(int code, Product product, int stock) {
        code2product.put(code, product);
        code2stock.put(code, code2stock.getOrDefault(code, 0) + stock);
    }

    public synchronized Product remove(int code) {}
}

public class Product {

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
