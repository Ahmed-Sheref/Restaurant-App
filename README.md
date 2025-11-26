# Restaurant Ordering System 🍕🍔

A console-based **Restaurant Management System** implemented in **Java** demonstrating the use of **multiple design patterns** including:

- **Abstract Factory** – Different menu types (Vegetarian, Non-Vegetarian, Kids)
- **Decorator** – Dynamic add-ons (Extra Cheese, Barbecue Sauce)
- **Strategy** – Multiple payment methods (Cash, Mobile Wallet, Credit Card)
- **Observer** – Kitchen notification system (Chef & Waiter)
- **Facade** – Simplified interface for placing orders
- **Strategy (Discount)** – Per-item automatic discount logic

---

### Features

- Browse 3 different menus: Vegetarian, Non-Vegetarian, and Kids
- Add multiple items to a single order
- Customize each item with add-ons (stackable)
- Automatic per-item discounts:
  - 10% off all Pizzas
  - 8% off Meat items (e.g., Burgers)
  - 6% off Chicken items (e.g., Nuggets)
- Choose from 3 payment methods
- Real-time kitchen notifications using Observer pattern
- Clean bill generation with subtotal, discount, tax (14%), and final total

---

### Design Patterns Used

| Pattern             | Usage                                      |
|---------------------|--------------------------------------------|
| Abstract Factory    | Creates different menu types               |
| Decorator           | Adds Extra Cheese / Barbecue Sauce dynamically |
| Strategy            | Interchangeable payment & discount strategies |
| Observer            | Notifies Chef and Waiter when order starts |
| Facade              | Simplifies complex ordering process       |

---

Sample Output
textWelcome to the Restaurant System!

Choose Menu Type:
1) Vegetarian Menu
2) Non-Vegetarian Menu
3) Kids Menu
0) Exit
Your choice: 1

=== Vegetarian Menu ===
1) Italian Pizza : 80.00 EGP
2) Eastern Pizza : 75.00 EGP

Choose item number: 1

Add-ons for this item (choose number):
1) Extra Cheese (+10)
2) Barbecue Sauce (+7)
0) Done
Your choice: 1
Added Extra Cheese.

--- Bill ---
* Italian Pizza + Extra Cheese : 90.00 EGP
Subtotal: 90.00 EGP
Discount (per-item): -9.00 EGP
Tax (14%): 11.34 EGP
Total: 92.34 EGP

Paid 92.34 EGP by Credit Card.
Chef received order. Preparing food...
Waiter received order. Will serve soon...

### Project Structure (Key Classes)
