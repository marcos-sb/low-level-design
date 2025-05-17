/*
Multiple Coffee Types: The machine should support multiple coffee recipes (e.g., Espresso, Latte, Cappuccino).
Ingredient Management: The machine should track and manage ingredient levels, and prevent dispensing if ingredients are insufficient.
Payment Processing: The machine should process payments before dispensing coffee.
Refill Ingredients: The machine should allow refilling of ingredients.
Extensibility: Easy to add new coffee types or payment methods.
*/

public abstract class CoffeeType {

    private final Value price;

    public CoffeeType(Value price) {
        this.price = price;
    }

    public abstract void prepare(Machine machine);
}

public class Espresso implements CoffeeType {

    public Espresso(Value value) {
        super(value);
    }

    @Override
    public void prepare(Machine machine) {
        // use `machine` to prepare the coffee
    }
}

public class Ingredient {

    final String name;
}

public class IngredientLevel {

    private final int level;
}

class IngredientManager {

    private final Map<Ingredient, IngredientLevel> levelsByIngredient;

    IngredientManager() {
        this(new HashMap<>());
    }

    IngredientManager(Map<Ingredient, IngredientLevel> levelsByIngredient) {
        this.levelsByIngredient = levelsByIngredient;
    }

    IngredientLevel getLevel(Ingredient ingredient) {
        return levelsByIngredient.get(ingredient);
    }

    void setLevel(Ingredient ingredient, IngredientLevel level) {
        levelsByIngredient.put(ingredient, level);
    }
}

public record Value(int whole, int decimal) {
    public Value add(int whole, int decimal) {
        return new Value(this.whole + whole, this.decimal + decimal);
    }
}

public class Machine {

    private final IngredientManager ingredientManager;
    private final PaymentProcessor paymentProcessor;
    private final Context context;

    public void selectCoffee(CoffeeType coffeeType) {}

    public void pay(Value amount) {
        context.pay(paymentProcessor, amount);
    }

    public void prepare() throws InsuffientIngredient {
        context.prepare(this);
    }

    @Override
    public void refill(Ingredient ingredient, IngredientLevel level) {
        ingredientManager.setLevel(ingredient, level);
    }
}

public interface PaymentProcessor {
    public boolean processPayment(Value amount);
}

public class CCPaymentProcessor {

    @Override
    public boolean processPayment(Value amount) {
        // Attempt transaction
        return true;
    }
}

public class Context {

    private State state;
    private Optional<CoffeeType> maybeCoffeeType;

    public Context() {
        this.state = new Idle();
        this.maybeCoffeeType = Optional.empty();
    }

    void transitionTo(State state) {
        this.state = state;
    }

    void setCoffeeType(CoffeeType coffeeType) {
        this.coffeeType = Optional.of(coffeeType);
    }

    public void selectCoffee(CoffeeType coffeeType) {
        state.selectCoffee(coffeeType);
    }

    public void pay(PaymentProcessor paymentProcessor, Value amount) {
        state.pay(paymentProcessor, amount);
    }

    public void prepare(Machine machine) {
        state.prepare(maybeCoffeeType, machine);
    }

    public void refill(Ingredient ingredient, IngredientLevel level);
}

public abstract class State {

    private final Context context;

    public State(Context context) {
        this.context = context;
    }

    public abstract void selectCoffee(CoffeeType coffeeType);

    public abstract void prepare(CoffeeType coffeeType, Machine machine);
}

public class Idle extends State {

    public void selectCoffee(CoffeeType coffeeType) {
        setCoffeeType(coffeeType);
        getContext().transitionTo(new AwaitingPayment(coffeeType.getPrice()));
    }
}

public class AwaitingPayment extends State {

    private Value toPay;

    public AwaitingPayment(Value toPay) {
        this.toPay = toPay;
    }

    @Override
    public void pay(PaymentProcessor paymentProcessor, Value amount) {
        if (paymentProcessor.processPayment(amount)) {
            toPay = toPay.subtract(amount);
            if (toPay.zero()) {
                getContext().transitionTo(new ReadyToPrepare());
            }
        }
    }
}

public class ReadyToPrepare extends State {

    @Override
    public void prepare(CoffeeType coffeeType, Machine machine) {
        coffeeType.prepare(machine);
        getContext().transitionTo(new Idle());
    }
}

public class InsuffientIngredient extends Exception {

    private final Ingredient ingredient;
    private final IngredientLevel level;

    public InsuffientIngredient(Ingredient ingredient, IngredientLevel level) {
        this.ingredient = ingredient;
        this.level = level;
        final var message = String.format("", ingredient, level);
        super(message);
    }
}
