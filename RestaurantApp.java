import java.util.*;
import java.util.function.Function;

public class RestaurantApp
{
    public static void main(String[] args)
    {
        // read user input from console
        Scanner in = new Scanner(System.in);

        // build kitchen notification system and register observers
        KitchenSystem kitchenSystem = new KitchenSystem();
        kitchenSystem.addObserver(new Chef());
        kitchenSystem.addObserver(new Waiter());

        // facade will coordinate ordering + notifying kitchen + paying
        RestaurantSystemFacade facade = new RestaurantSystemFacade(kitchenSystem);

        System.out.println("Welcome to the Restaurant System!");

        boolean running = true;
        while (running)
        {
            // 1) user chooses which menu to browse
            System.out.println("\nChoose Menu Type:");
            System.out.println("1) Vegetarian Menu");
            System.out.println("2) Non-Vegetarian Menu");
            System.out.println("3) Kids Menu");
            System.out.println("0) Exit");
            System.out.print("Your choice: ");

            int menuChoice = readInt(in);
            if (menuChoice == 0)
            {
                running = false;
                continue;
            }

            // 2) create the correct MenuFactory based on the choice (Abstract Factory)
            MenuFactory factory = null;
            switch (menuChoice)
            {
                case 1: factory = new VegetarianMenuFactory(); break;
                case 2: factory = new NonVegetarianMenuFactory(); break;
                case 3: factory = new KidsMenuFactory(); break;
                default:
                    System.out.println("Invalid menu choice.");
                    continue;
            }

            // 3) factory creates a menu that contains items for that category
            Menu menu = factory.createMenu();

            // 4) multi-item order: user can add several items from the same menu
            List<MenuItem> chosenItems = new ArrayList<>();
            boolean addingItems = true;

            while (addingItems)
            {
                // show menu items with numbers
                menu.showItems();
                System.out.print("\nChoose item number: ");
                int itemChoice = readInt(in);

                List<MenuItem> items = menu.getItems();
                if (itemChoice < 1 || itemChoice > items.size())
                {
                    System.out.println("Invalid item choice.");
                    continue;
                }

                // base item before add-ons
                MenuItem baseItem = items.get(itemChoice - 1);

                // 5) add-ons for THIS item only (Decorator chain)
                List<Function<MenuItem, MenuItem>> addOns = new ArrayList<>();
                boolean addingAddOns = true;

                while (addingAddOns)
                {
                    System.out.println("\nAdd-ons for this item (choose number):");
                    System.out.println("1) Extra Cheese (+10)");
                    System.out.println("2) Barbecue Sauce (+7)");
                    System.out.println("0) Done");
                    System.out.print("Your choice: ");

                    int addOnChoice = readInt(in);
                    switch (addOnChoice)
                    {
                        case 1:
                            addOns.add(ExtraCheese::new);
                            System.out.println("Added Extra Cheese.");
                            break;
                        case 2:
                            addOns.add(BarbecueSauce::new);
                            System.out.println("Added Barbecue Sauce.");
                            break;
                        case 0:
                            addingAddOns = false;
                            break;
                        default:
                            System.out.println("Invalid add-on choice.");
                            break;
                    }
                }

                // apply decorators and store final item in the order list
                MenuItem finalItem = facade.applyAddOnsPublic(baseItem, addOns);
                chosenItems.add(finalItem);

                // ask if user wants another item from the same menu
                System.out.println("\nAdd another item from the same menu?");
                System.out.println("1) Yes");
                System.out.println("0) No");
                System.out.print("Your choice: ");
                addingItems = (readInt(in) == 1);
            }

            // 6) choose payment method (Strategy)
            System.out.println("\nChoose Payment Method:");
            System.out.println("1) Cash");
            System.out.println("2) Mobile Wallet");
            System.out.println("3) Credit Card");
            System.out.print("Your choice: ");

            int payChoice = readInt(in);
            PaymentMethod payment = null;
            switch (payChoice)
            {
                case 1: payment = new CashPayment(); break;
                case 2: payment = new MobileWalletPayment(); break;
                case 3: payment = new CreditCardPayment(); break;
                default:
                    System.out.println("Invalid payment choice.");
                    continue;
            }

            // 7) place order through facade
            //    discount is NOT chosen by user; Order.pay() computes per-item discounts automatically
            facade.placeOrder(factory, chosenItems, payment);

            // 8) repeat or exit
            System.out.println("\nOrder completed. Want another order? (1=Yes, 0=No)");
            System.out.print("Your choice: ");
            running = (readInt(in) == 1);
        }

        System.out.println("Goodbye!");
        in.close();
    }


    private static int readInt(Scanner in)
    {
        while (true)
        {
            try
            {
                return Integer.parseInt(in.nextLine().trim());
            }
            catch (Exception e)
            {
                System.out.print("Enter a valid number: ");
            }
        }
    }

    // ===================== Facade =====================
    public static class RestaurantSystemFacade
    {
        private final KitchenSystem kitchenSystem;

        public RestaurantSystemFacade(KitchenSystem kitchenSystem)
        {
            this.kitchenSystem = kitchenSystem;
        }

        public void placeOrder(MenuFactory factory,
                               List<MenuItem> items,
                               PaymentMethod payment)
        {
            Order order = new Order();

            for (MenuItem item : items)
            {
                order.addItem(item);
            }

            order.setPaymentMethod(payment);

            kitchenSystem.startOrder(order);
            order.pay();
        }

        public MenuItem applyAddOnsPublic(MenuItem item, List<Function<MenuItem, MenuItem>> addOnsList)
        {
            return applyAddOns(item, addOnsList);
        }

        private MenuItem applyAddOns(MenuItem item, List<Function<MenuItem, MenuItem>> addOnsList)
        {
            MenuItem result = item;
            for (Function<MenuItem, MenuItem> addOn : addOnsList)
            {
                result = addOn.apply(result);
            }
            return result;
        }
    }

    // ===================== Order =====================
    public static class Order
    {
        private final List<MenuItem> items;
        private PaymentMethod paymentMethod;
        private double taxRate;

        public Order()
        {
            this.items = new ArrayList<>();
            this.taxRate = 0.14;
        }

        public void addItem(MenuItem item)
        {
            items.add(item);
        }

        public void setPaymentMethod(PaymentMethod paymentMethod)
        {
            this.paymentMethod = paymentMethod;
        }

        public void pay()
        {
            double subtotal = 0.0;
            double discountValue = 0.0;

            // discount is calculated per item (logical restaurant behavior)
            for (MenuItem item : items)
            {
                double price = item.getPrice();
                subtotal += price;

                DiscountHandling d = DiscountSelector.fromItem(item);
                discountValue += d.applyDiscount(price);
            }

            double afterDiscount = subtotal - discountValue;
            double tax = afterDiscount * taxRate;
            double total = afterDiscount + tax;

            System.out.println("\n--- Bill ---");
            for (MenuItem item : items)
            {
                System.out.printf("* %s : %.2f EGP%n", item.getName(), item.getPrice());
            }
            System.out.printf("Subtotal: %.2f EGP%n", subtotal);
            System.out.printf("Discount (per-item): -%.2f EGP%n", discountValue);
            System.out.printf("Tax (%.0f%%): %.2f EGP%n", taxRate * 100, tax);
            System.out.printf("Total: %.2f EGP%n", total);

            paymentMethod.payment(total);
        }
    }

    // ===================== Menu / Abstract Factory =====================
    public interface MenuItem
    {
        String getName();
        double getPrice();
        ItemKind getKind();
    }

    public enum ItemKind
    {
        PIZZA,
        MEAT,
        CHICKEN,
        OTHER
    }

    public static abstract class Pizza implements MenuItem
    {
        @Override
        public ItemKind getKind()
        {
            return ItemKind.PIZZA;
        }
    }

    public static abstract class Burger implements MenuItem
    {
        @Override
        public ItemKind getKind()
        {
            return ItemKind.MEAT;
        }
    }

    public static class ItalianPizza extends Pizza
    {
        @Override
        public String getName()
        {
            return "Italian Pizza";
        }

        @Override
        public double getPrice()
        {
            return 80.0;
        }
    }

    public static class EasternPizza extends Pizza
    {
        @Override
        public String getName()
        {
            return "Eastern Pizza";
        }

        @Override
        public double getPrice()
        {
            return 75.0;
        }
    }

    public static class ClassicBurger extends Burger
    {
        @Override
        public String getName()
        {
            return "Classic Burger";
        }

        @Override
        public double getPrice()
        {
            return 65.0;
        }
    }

    public static class Nuggets implements MenuItem
    {
        @Override
        public String getName()
        {
            return "Nuggets";
        }

        @Override
        public double getPrice()
        {
            return 40.0;
        }

        @Override
        public ItemKind getKind()
        {
            return ItemKind.CHICKEN;
        }
    }

    public interface Menu
    {
        void showItems();
        List<MenuItem> getItems();
    }

    public static class VegetarianMenu implements Menu
    {
        private final List<MenuItem> items;

        public VegetarianMenu()
        {
            items = new ArrayList<>();
            items.add(new ItalianPizza());
            items.add(new EasternPizza());
        }

        @Override
        public void showItems()
        {
            System.out.println("\n=== Vegetarian Menu ===");
            for (int i = 0; i < items.size(); i++)
            {
                MenuItem item = items.get(i);
                System.out.printf("%d) %s : %.2f EGP%n", i + 1, item.getName(), item.getPrice());
            }
        }

        @Override
        public List<MenuItem> getItems()
        {
            return Collections.unmodifiableList(items);
        }
    }

    public static class NonVegetarianMenu implements Menu
    {
        private final List<MenuItem> items;

        public NonVegetarianMenu()
        {
            items = new ArrayList<>();
            items.add(new ClassicBurger());
            items.add(new Nuggets());
        }

        @Override
        public void showItems()
        {
            System.out.println("\n=== Non-Vegetarian Menu ===");
            for (int i = 0; i < items.size(); i++)
            {
                MenuItem item = items.get(i);
                System.out.printf("%d) %s : %.2f EGP%n", i + 1, item.getName(), item.getPrice());
            }
        }

        @Override
        public List<MenuItem> getItems()
        {
            return Collections.unmodifiableList(items);
        }
    }

    public static class KidsMenu implements Menu
    {
        private final List<MenuItem> items;

        public KidsMenu()
        {
            items = new ArrayList<>();
            items.add(new Nuggets());
            items.add(new EasternPizza());
        }

        @Override
        public void showItems()
        {
            System.out.println("\n=== Kids Menu ===");
            for (int i = 0; i < items.size(); i++)
            {
                MenuItem item = items.get(i);
                System.out.printf("%d) %s : %.2f EGP%n", i + 1, item.getName(), item.getPrice());
            }
        }

        @Override
        public List<MenuItem> getItems()
        {
            return Collections.unmodifiableList(items);
        }
    }

    public interface MenuFactory
    {
        Menu createMenu();
    }

    public static class VegetarianMenuFactory implements MenuFactory
    {
        @Override
        public Menu createMenu()
        {
            return new VegetarianMenu();
        }
    }

    public static class NonVegetarianMenuFactory implements MenuFactory
    {
        @Override
        public Menu createMenu()
        {
            return new NonVegetarianMenu();
        }
    }

    public static class KidsMenuFactory implements MenuFactory
    {
        @Override
        public Menu createMenu()
        {
            return new KidsMenu();
        }
    }

    // ===================== Decorator (Add-ons) =====================
    public static abstract class BaseDecorator implements MenuItem
    {
        protected final MenuItem wrapper;

        public BaseDecorator(MenuItem item)
        {
            this.wrapper = item;
        }

        @Override
        public String getName()
        {
            return wrapper.getName();
        }

        @Override
        public double getPrice()
        {
            return wrapper.getPrice();
        }

        @Override
        public ItemKind getKind()
        {
            return wrapper.getKind();
        }
    }

    public static class ExtraCheese extends BaseDecorator
    {
        public ExtraCheese(MenuItem item)
        {
            super(item);
        }

        @Override
        public String getName()
        {
            return wrapper.getName() + " + Extra Cheese";
        }

        @Override
        public double getPrice()
        {
            return super.getPrice() + 10.0;
        }
    }

    public static class BarbecueSauce extends BaseDecorator
    {
        public BarbecueSauce(MenuItem item)
        {
            super(item);
        }

        @Override
        public String getName()
        {
            return wrapper.getName() + " + Barbecue Sauce";
        }

        @Override
        public double getPrice()
        {
            return super.getPrice() + 7.0;
        }
    }

    // ===================== Discount Strategy =====================
    public interface DiscountHandling
    {
        double applyDiscount(double priceOfThisItem);
    }

    public static class PizzaDiscount implements DiscountHandling
    {
        @Override
        public double applyDiscount(double priceOfThisItem)
        {
            return priceOfThisItem * 0.10;
        }
    }

    public static class MeatDiscount implements DiscountHandling
    {
        @Override
        public double applyDiscount(double priceOfThisItem)
        {
            return priceOfThisItem * 0.08;
        }
    }

    public static class ChickenDiscount implements DiscountHandling
    {
        @Override
        public double applyDiscount(double priceOfThisItem)
        {
            return priceOfThisItem * 0.06;
        }
    }

    public static class NoDiscount implements DiscountHandling
    {
        @Override
        public double applyDiscount(double priceOfThisItem)
        {
            return 0.0;
        }
    }

    public static class DiscountSelector
    {
        public static DiscountHandling fromItem(MenuItem item)
        {
            if (item.getKind() == ItemKind.PIZZA) return new PizzaDiscount();
            if (item.getKind() == ItemKind.MEAT) return new MeatDiscount();
            if (item.getKind() == ItemKind.CHICKEN) return new ChickenDiscount();
            return new NoDiscount();
        }
    }

    // ===================== Payment Strategy =====================
    public interface PaymentMethod
    {
        void payment(double amount);
    }

    public static class CashPayment implements PaymentMethod
    {
        @Override
        public void payment(double amount)
        {
            System.out.printf("Paid %.2f EGP by Cash.%n", amount);
        }
    }

    public static class MobileWalletPayment implements PaymentMethod
    {
        @Override
        public void payment(double amount)
        {
            System.out.printf("Paid %.2f EGP by Mobile Wallet.%n", amount);
        }
    }

    public static class CreditCardPayment implements PaymentMethod
    {
        @Override
        public void payment(double amount)
        {
            System.out.printf("Paid %.2f EGP by Credit Card.%n", amount);
        }
    }

    // ===================== Observer (Kitchen Notification) =====================
    public interface Observer
    {
        void update(Order order);
    }

    public static class KitchenSystem
    {
        private final List<Observer> observers;

        public KitchenSystem()
        {
            observers = new ArrayList<>();
        }

        public void addObserver(Observer o)
        {
            observers.add(o);
        }

        public void removeObserver(Observer o)
        {
            observers.remove(o);
        }

        public void startOrder(Order order)
        {
            notifyObservers(order);
        }

        private void notifyObservers(Order order)
        {
            for (Observer o : observers)
            {
                o.update(order);
            }
        }
    }

    public static class Chef implements Observer
    {
        @Override
        public void update(Order order)
        {
            System.out.println("Chef received order. Preparing food...");
        }
    }

    public static class Waiter implements Observer
    {
        @Override
        public void update(Order order)
        {
            System.out.println("Waiter received order. Will serve soon...");
        }
    }
}