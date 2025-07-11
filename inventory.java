import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class InventoryManagementSystem {
    private Connection connection;
    private Scanner scanner;

    public static void main(String[] args) {
        InventoryManagementSystem system = new InventoryManagementSystem();
        system.connectToDatabase();
        system.createTablesIfNotExist();
        system.run();
    }

    public void connectToDatabase() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/inventory_db", "username", "password");
            System.out.println("Connected to database successfully");
        } catch (Exception e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
    }

    public void createTablesIfNotExist() {
        try {
            Statement stmt = connection.createStatement();
            
            // Items table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS items (" +
                "item_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "description VARCHAR(255), " +
                "quantity INT NOT NULL, " +
                "price DECIMAL(10,2) NOT NULL, " +
                "threshold INT NOT NULL)");
            
            // Suppliers table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS suppliers (" +
                "supplier_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "contact VARCHAR(100), " +
                "phone VARCHAR(20), " +
                "email VARCHAR(100))");
            
            // Orders table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS orders (" +
                "order_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "supplier_id INT, " +
                "item_id INT, " +
                "quantity INT NOT NULL, " +
                "order_date DATE NOT NULL, " +
                "status VARCHAR(20) NOT NULL, " +
                "FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id), " +
                "FOREIGN KEY (item_id) REFERENCES items(item_id))");
            
            // Alerts table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS alerts (" +
                "alert_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "item_id INT, " +
                "message VARCHAR(255) NOT NULL, " +
                "alert_date DATE NOT NULL, " +
                "status VARCHAR(20) NOT NULL, " +
                "FOREIGN KEY (item_id) REFERENCES items(item_id))");
            
            System.out.println("Tables created/verified successfully");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    public void run() {
        scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            System.out.println("\nInventory Management System");
            System.out.println("1. Item Management");
            System.out.println("2. Supplier Management");
            System.out.println("3. Order Management");
            System.out.println("4. View Alerts");
            System.out.println("5. Exit");
            System.out.print("Select an option: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline
            
            switch (choice) {
                case 1:
                    itemManagement();
                    break;
                case 2:
                    supplierManagement();
                    break;
                case 3:
                    orderManagement();
                    break;
                case 4:
                    viewAlerts();
                    break;
                case 5:
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
        
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
        scanner.close();
    }

    private void itemManagement() {
        boolean back = false;
        
        while (!back) {
            System.out.println("\nItem Management");
            System.out.println("1. Add New Item");
            System.out.println("2. Update Item");
            System.out.println("3. View All Items");
            System.out.println("4. Check Stock Levels");
            System.out.println("5. Back to Main Menu");
            System.out.print("Select an option: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline
            
            switch (choice) {
                case 1:
                    addItem();
                    break;
                case 2:
                    updateItem();
                    break;
                case 3:
                    viewAllItems();
                    break;
                case 4:
                    checkStockLevels();
                    break;
                case 5:
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void addItem() {
        try {
            System.out.println("\nAdd New Item");
            System.out.print("Enter item name: ");
            String name = scanner.nextLine();
            
            System.out.print("Enter description: ");
            String description = scanner.nextLine();
            
            System.out.print("Enter initial quantity: ");
            int quantity = scanner.nextInt();
            
            System.out.print("Enter price: ");
            double price = scanner.nextDouble();
            
            System.out.print("Enter threshold for alerts: ");
            int threshold = scanner.nextInt();
            
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO items (name, description, quantity, price, threshold) VALUES (?, ?, ?, ?, ?)");
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, quantity);
            stmt.setDouble(4, price);
            stmt.setInt(5, threshold);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Item added successfully");
                checkForLowStock(); // Check if new item is already low
            }
        } catch (SQLException e) {
            System.err.println("Error adding item: " + e.getMessage());
        }
    }

    private void updateItem() {
        try {
            viewAllItems();
            System.out.print("\nEnter item ID to update: ");
            int itemId = scanner.nextInt();
            scanner.nextLine(); // consume newline
            
            System.out.print("Enter new name (leave blank to keep current): ");
            String name = scanner.nextLine();
            
            System.out.print("Enter new description (leave blank to keep current): ");
            String description = scanner.nextLine();
            
            System.out.print("Enter new quantity (-1 to keep current): ");
            int quantity = scanner.nextInt();
            
            System.out.print("Enter new price (-1 to keep current): ");
            double price = scanner.nextDouble();
            
            System.out.print("Enter new threshold (-1 to keep current): ");
            int threshold = scanner.nextInt();
            
            StringBuilder query = new StringBuilder("UPDATE items SET ");
            List<Object> params = new ArrayList<>();
            
            if (!name.isEmpty()) {
                query.append("name = ?, ");
                params.add(name);
            }
            if (!description.isEmpty()) {
                query.append("description = ?, ");
                params.add(description);
            }
            if (quantity != -1) {
                query.append("quantity = ?, ");
                params.add(quantity);
            }
            if (price != -1) {
                query.append("price = ?, ");
                params.add(price);
            }
            if (threshold != -1) {
                query.append("threshold = ?, ");
                params.add(threshold);
            }
            
            // Remove trailing comma and space
            query.delete(query.length() - 2, query.length());
            query.append(" WHERE item_id = ?");
            params.add(itemId);
            
            PreparedStatement stmt = connection.prepareStatement(query.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Item updated successfully");
                checkForLowStock(); // Re-check stock levels after update
            } else {
                System.out.println("No item found with ID: " + itemId);
            }
        } catch (SQLException e) {
            System.err.println("Error updating item: " + e.getMessage());
        }
    }

    private void viewAllItems() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM items");
            
            System.out.println("\nItem List:");
            System.out.printf("%-10s %-20s %-50s %-10s %-10s %-10s%n", 
                "ID", "Name", "Description", "Qty", "Price", "Threshold");
            
            while (rs.next()) {
                System.out.printf("%-10d %-20s %-50s %-10d %-10.2f %-10d%n",
                    rs.getInt("item_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("quantity"),
                    rs.getDouble("price"),
                    rs.getInt("threshold"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving items: " + e.getMessage());
        }
    }

    private void checkStockLevels() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT item_id, name, quantity, threshold FROM items WHERE quantity <= threshold");
            
            if (!rs.isBeforeFirst()) {
                System.out.println("\nAll items have sufficient stock");
                return;
            }
            
            System.out.println("\nLow Stock Items:");
            System.out.printf("%-10s %-20s %-10s %-10s%n", "ID", "Name", "Current", "Threshold");
            
            while (rs.next()) {
                System.out.printf("%-10d %-20s %-10d %-10d%n",
                    rs.getInt("item_id"),
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getInt("threshold"));
            }
        } catch (SQLException e) {
            System.err.println("Error checking stock levels: " + e.getMessage());
        }
    }

    private void supplierManagement() {
        boolean back = false;
        
        while (!back) {
            System.out.println("\nSupplier Management");
            System.out.println("1. Add New Supplier");
            System.out.println("2. Update Supplier");
            System.out.println("3. View All Suppliers");
            System.out.println("4. Back to Main Menu");
            System.out.print("Select an option: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline
            
            switch (choice) {
                case 1:
                    addSupplier();
                    break;
                case 2:
                    updateSupplier();
                    break;
                case 3:
                    viewAllSuppliers();
                    break;
                case 4:
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void addSupplier() {
        try {
            System.out.println("\nAdd New Supplier");
            System.out.print("Enter supplier name: ");
            String name = scanner.nextLine();
            
            System.out.print("Enter contact person: ");
            String contact = scanner.nextLine();
            
            System.out.print("Enter phone number: ");
            String phone = scanner.nextLine();
            
            System.out.print("Enter email: ");
            String email = scanner.nextLine();
            
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO suppliers (name, contact, phone, email) VALUES (?, ?, ?, ?)");
            stmt.setString(1, name);
            stmt.setString(2, contact);
            stmt.setString(3, phone);
            stmt.setString(4, email);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Supplier added successfully");
            }
        } catch (SQLException e) {
            System.err.println("Error adding supplier: " + e.getMessage());
        }
    }

    private void updateSupplier() {
        try {
            viewAllSuppliers();
            System.out.print("\nEnter supplier ID to update: ");
            int supplierId = scanner.nextInt();
            scanner.nextLine(); // consume newline
            
            System.out.print("Enter new name (leave blank to keep current): ");
            String name = scanner.nextLine();
            
            System.out.print("Enter new contact person (leave blank to keep current): ");
            String contact = scanner.nextLine();
            
            System.out.print("Enter new phone number (leave blank to keep current): ");
            String phone = scanner.nextLine();
            
            System.out.print("Enter new email (leave blank to keep current): ");
            String email = scanner.nextLine();
            
            StringBuilder query = new StringBuilder("UPDATE suppliers SET ");
            List<Object> params = new ArrayList<>();
            
            if (!name.isEmpty()) {
                query.append("name = ?, ");
                params.add(name);
            }
            if (!contact.isEmpty()) {
                query.append("contact = ?, ");
                params.add(contact);
            }
            if (!phone.isEmpty()) {
                query.append("phone = ?, ");
                params.add(phone);
            }
            if (!email.isEmpty()) {
                query.append("email = ?, ");
                params.add(email);
            }
            
            // Remove trailing comma and space
            query.delete(query.length() - 2, query.length());
            query.append(" WHERE supplier_id = ?");
            params.add(supplierId);
            
            PreparedStatement stmt = connection.prepareStatement(query.toString());
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Supplier updated successfully");
            } else {
                System.out.println("No supplier found with ID: " + supplierId);
            }
        } catch (SQLException e) {
            System.err.println("Error updating supplier: " + e.getMessage());
        }
    }

    private void viewAllSuppliers() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM suppliers");
            
            System.out.println("\nSupplier List:");
            System.out.printf("%-10s %-20s %-20s %-15s %-20s%n", 
                "ID", "Name", "Contact", "Phone", "Email");
            
            while (rs.next()) {
                System.out.printf("%-10d %-20s %-20s %-15s %-20s%n",
                    rs.getInt("supplier_id"),
                    rs.getString("name"),
                    rs.getString("contact"),
                    rs.getString("phone"),
                    rs.getString("email"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving suppliers: " + e.getMessage());
        }
    }

    private void orderManagement() {
        boolean back = false;
        
        while (!back) {
            System.out.println("\nOrder Management");
            System.out.println("1. Create New Order");
            System.out.println("2. Update Order Status");
            System.out.println("3. View All Orders");
            System.out.println("4. View Orders by Status");
            System.out.println("5. Back to Main Menu");
            System.out.print("Select an option: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline
            
            switch (choice) {
                case 1:
                    createOrder();
                    break;
                case 2:
                    updateOrderStatus();
                    break;
                case 3:
                    viewAllOrders();
                    break;
                case 4:
                    viewOrdersByStatus();
                    break;
                case 5:
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void createOrder() {
        try {
            System.out.println("\nCreate New Order");
            
            viewAllItems();
            System.out.print("Enter item ID to order: ");
            int itemId = scanner.nextInt();
            
            viewAllSuppliers();
            System.out.print("Enter supplier ID: ");
            int supplierId = scanner.nextInt();
            
            System.out.print("Enter quantity to order: ");
            int quantity = scanner.nextInt();
            scanner.nextLine(); // consume newline
            
            System.out.print("Enter order status (Pending/Shipped/Received): ");
            String status = scanner.nextLine();
            
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO orders (supplier_id, item_id, quantity, order_date, status) VALUES (?, ?, ?, ?, ?)");
            stmt.setInt(1, supplierId);
            stmt.setInt(2, itemId);
            stmt.setInt(3, quantity);
            stmt.setDate(4, new java.sql.Date(new Date().getTime()));
            stmt.setString(5, status);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Order created successfully");
                
                // If order is received, update inventory
                if (status.equalsIgnoreCase("Received")) {
                    updateInventoryAfterOrder(itemId, quantity);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating order: " + e.getMessage());
        }
    }

    private void updateOrderStatus() {
        try {
            viewAllOrders();
            System.out.print("\nEnter order ID to update: ");
            int orderId = scanner.nextInt();
            scanner.nextLine(); // consume newline
            
            System.out.print("Enter new status (Pending/Shipped/Received): ");
            String status = scanner.nextLine();
            
            // First get the current status and item details
            PreparedStatement getStmt = connection.prepareStatement(
                "SELECT status, item_id, quantity FROM orders WHERE order_id = ?");
            getStmt.setInt(1, orderId);
            ResultSet rs = getStmt.executeQuery();
            
            if (!rs.next()) {
                System.out.println("No order found with ID: " + orderId);
                return;
            }
            
            String currentStatus = rs.getString("status");
            int itemId = rs.getInt("item_id");
            int quantity = rs.getInt("quantity");
            
            // Update the status
            PreparedStatement updateStmt = connection.prepareStatement(
                "UPDATE orders SET status = ? WHERE order_id = ?");
            updateStmt.setString(1, status);
            updateStmt.setInt(2, orderId);
            
            int rows = updateStmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Order status updated successfully");
                
                // If status changed to Received, update inventory
                if (!currentStatus.equalsIgnoreCase("Received") && status.equalsIgnoreCase("Received")) {
                    updateInventoryAfterOrder(itemId, quantity);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating order status: " + e.getMessage());
        }
    }

    private void updateInventoryAfterOrder(int itemId, int quantity) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE items SET quantity = quantity + ? WHERE item_id = ?");
            stmt.setInt(1, quantity);
            stmt.setInt(2, itemId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Inventory updated after order receipt");
                checkForLowStock(); // Re-check stock levels
            }
        } catch (SQLException e) {
            System.err.println("Error updating inventory: " + e.getMessage());
        }
    }

    private void viewAllOrders() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT o.order_id, o.order_date, o.quantity, o.status, " +
                "i.name as item_name, s.name as supplier_name " +
                "FROM orders o " +
                "JOIN items i ON o.item_id = i.item_id " +
                "JOIN suppliers s ON o.supplier_id = s.supplier_id");
            
            System.out.println("\nOrder List:");
            System.out.printf("%-10s %-15s %-20s %-20s %-10s %-15s%n", 
                "ID", "Date", "Supplier", "Item", "Qty", "Status");
            
            while (rs.next()) {
                System.out.printf("%-10d %-15s %-20s %-20s %-10d %-15s%n",
                    rs.getInt("order_id"),
                    rs.getDate("order_date"),
                    rs.getString("supplier_name"),
                    rs.getString("item_name"),
                    rs.getInt("quantity"),
                    rs.getString("status"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving orders: " + e.getMessage());
        }
    }

    private void viewOrdersByStatus() {
        try {
            System.out.print("\nEnter status to filter (Pending/Shipped/Received): ");
            String status = scanner.nextLine();
            
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT o.order_id, o.order_date, o.quantity, o.status, " +
                "i.name as item_name, s.name as supplier_name " +
                "FROM orders o " +
                "JOIN items i ON o.item_id = i.item_id " +
                "JOIN suppliers s ON o.supplier_id = s.supplier_id " +
                "WHERE o.status = ?");
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            
            if (!rs.isBeforeFirst()) {
                System.out.println("No orders found with status: " + status);
                return;
            }
            
            System.out.println("\nOrders with Status: " + status);
            System.out.printf("%-10s %-15s %-20s %-20s %-10s%n", 
                "ID", "Date", "Supplier", "Item", "Qty");
            
            while (rs.next()) {
                System.out.printf("%-10d %-15s %-20s %-20s %-10d%n",
                    rs.getInt("order_id"),
                    rs.getDate("order_date"),
                    rs.getString("supplier_name"),
                    rs.getString("item_name"),
                    rs.getInt("quantity"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving orders: " + e.getMessage());
        }
    }

    private void viewAlerts() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT a.alert_id, a.message, a.alert_date, a.status, " +
                "i.name as item_name FROM alerts a " +
                "JOIN items i ON a.item_id = i.item_id " +
                "ORDER BY a.alert_date DESC");
            
            System.out.println("\nAlerts:");
            System.out.printf("%-10s %-15s %-50s %-20s %-10s%n", 
                "ID", "Date", "Message", "Item", "Status");
            
            while (rs.next()) {
                System.out.printf("%-10d %-15s %-50s %-20s %-10s%n",
                    rs.getInt("alert_id"),
                    rs.getDate("alert_date"),
                    rs.getString("message"),
                    rs.getString("item_name"),
                    rs.getString("status"));
            }
            
            // Option to mark alerts as resolved
            System.out.print("\nEnter alert ID to mark as resolved (0 to skip): ");
            int alertId = scanner.nextInt();
            scanner.nextLine(); // consume newline
            
            if (alertId > 0) {
                markAlertAsResolved(alertId);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving alerts: " + e.getMessage());
        }
    }

    private void markAlertAsResolved(int alertId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE alerts SET status = 'Resolved' WHERE alert_id = ?");
            stmt.setInt(1, alertId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Alert marked as resolved");
            } else {
                System.out.println("No alert found with ID: " + alertId);
            }
        } catch (SQLException e) {
            System.err.println("Error updating alert: " + e.getMessage());
        }
    }

    private void checkForLowStock() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT item_id, name, quantity, threshold FROM items " +
                "WHERE quantity <= threshold AND item_id NOT IN " +
                "(SELECT item_id FROM alerts WHERE status = 'Pending')");
            
            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                String itemName = rs.getString("name");
                int quantity = rs.getInt("quantity");
                int threshold = rs.getInt("threshold");
                
                String message = String.format(
                    "Low stock alert: %s (Current: %d, Threshold: %d)", 
                    itemName, quantity, threshold);
                
                // Create alert
                PreparedStatement alertStmt = connection.prepareStatement(
                    "INSERT INTO alerts (item_id, message, alert_date, status) VALUES (?, ?, ?, ?)");
                alertStmt.setInt(1, itemId);
                alertStmt.setString(2, message);
                alertStmt.setDate(3, new java.sql.Date(new Date().getTime()));
                alertStmt.setString(4, "Pending");
                alertStmt.executeUpdate();
                
                System.out.println("Alert generated: " + message);
            }
        } catch (SQLException e) {
            System.err.println("Error checking for low stock: " + e.getMessage());
        }
    }
}
