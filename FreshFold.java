import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class FreshFold {
    static HashMap<String, Integer> inventory = new HashMap<>();
    static HashMap<String, Boolean> machines = new HashMap<>();
    static ArrayList<String> orderList = new ArrayList<>();
    static double totalRevenue = 0.0;
    static JTextArea historyArea;
    static JLabel revenueLabel;
    static JLabel totalOrdersLabel;
    static JLabel mostUsedServiceLabel;
    static HashMap<String, Integer> serviceCounts = new HashMap<>();

    public static void main(String[] args) {
        loadInventory();
        loadOrders();

        machines.put("Washer 1", true);
        machines.put("Dryer 1", true);
        machines.put("Washer 2", true);

        JFrame frame = new JFrame("FreshFold - Laundry Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLayout(new BorderLayout());

        JPanel sidebar = new JPanel(new GridLayout(3, 1));
        sidebar.setPreferredSize(new Dimension(150, 600));
        sidebar.setBackground(new Color(60, 90, 150));

        JButton ordersBtn = new JButton("Orders");
        JButton inventoryBtn = new JButton("Inventory");
        JButton dashboardBtn = new JButton("Dashboard");

        sidebar.add(ordersBtn);
        sidebar.add(inventoryBtn);
        sidebar.add(dashboardBtn);

        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);

        // Orders panel
        JPanel ordersPanel = new JPanel(new BorderLayout());
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("New Order"));

        JTextField customerField = new JTextField();
        JTextField weightField = new JTextField();
        JComboBox<String> serviceType = new JComboBox<>(new String[]{"Wash", "Dry", "Fold", "Wash & Dry", "Full Service"});
        JTextField priceField = new JTextField();
        priceField.setEditable(false);
        JButton submitBtn = new JButton("Submit Order");

        formPanel.add(new JLabel("Customer Name:"));
        formPanel.add(customerField);
        formPanel.add(new JLabel("Laundry Weight (kg):"));
        formPanel.add(weightField);
        formPanel.add(new JLabel("Service Type:"));
        formPanel.add(serviceType);
        formPanel.add(new JLabel("Total Price:"));
        formPanel.add(priceField);
        formPanel.add(new JLabel(""));
        formPanel.add(submitBtn);

        historyArea = new JTextArea();
        historyArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(historyArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Transaction History"));
        updateHistoryArea();

        ordersPanel.add(formPanel, BorderLayout.NORTH);
        ordersPanel.add(scrollPane, BorderLayout.CENTER);

        // Dashboard panel
        JPanel dashboardPanel = new JPanel();
        dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
        dashboardPanel.setBorder(BorderFactory.createTitledBorder("Dashboard Overview"));

        totalOrdersLabel = new JLabel("Total Orders: " + orderList.size());
        revenueLabel = new JLabel(String.format("Total Revenue: ₱%.2f", totalRevenue));
        mostUsedServiceLabel = new JLabel("Most Used Service: N/A");

        dashboardPanel.add(totalOrdersLabel);
        dashboardPanel.add(revenueLabel);
        dashboardPanel.add(mostUsedServiceLabel);

        updateDashboard();

        // Inventory panel
        JPanel inventoryPanel = new JPanel();
        inventoryPanel.setLayout(new BoxLayout(inventoryPanel, BoxLayout.Y_AXIS));
        inventoryPanel.setBorder(BorderFactory.createTitledBorder("Inventory & Machines"));

        updateInventoryPanel(inventoryPanel);
        updateMachinePanel(inventoryPanel);

        // Logic
        weightField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                try {
                    double weight = Double.parseDouble(weightField.getText());
                    String service = (String) serviceType.getSelectedItem();
                    double rate = switch (service) {
                        case "Wash" -> 25;
                        case "Dry" -> 20;
                        case "Fold" -> 15;
                        case "Wash & Dry" -> 40;
                        case "Full Service" -> 60;
                        default -> 0;
                    };
                    priceField.setText(String.format("%.2f", weight * rate));
                } catch (Exception ignored) {
                    priceField.setText("");
                }
            }
        });

        submitBtn.addActionListener(e -> {
            String name = customerField.getText().trim();
            String weight = weightField.getText().trim();
            String service = (String) serviceType.getSelectedItem();
            String price = priceField.getText().trim();

            if (name.isEmpty() || weight.isEmpty() || price.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please fill out all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            useSupplies(service);
            double orderPrice = Double.parseDouble(price);
            totalRevenue += orderPrice;
            serviceCounts.put(service, serviceCounts.getOrDefault(service, 0) + 1);

            String summary = "Customer: " + name + ", Weight: " + weight + "kg, Service: " + service + ", ₱" + price;
            orderList.add(summary);
            updateHistoryArea();
            saveOrders();
            saveInventory();

            customerField.setText("");
            weightField.setText("");
            priceField.setText("");

            updateDashboard();
        });

        // Navigation
        cardPanel.add(ordersPanel, "Orders");
        cardPanel.add(inventoryPanel, "Inventory");
        cardPanel.add(dashboardPanel, "Dashboard");

        ordersBtn.addActionListener(e -> cardLayout.show(cardPanel, "Orders"));
        inventoryBtn.addActionListener(e -> {
            updateInventoryPanel(inventoryPanel);
            updateMachinePanel(inventoryPanel);
            cardLayout.show(cardPanel, "Inventory");
        });
        dashboardBtn.addActionListener(e -> {
            updateDashboard();
            cardLayout.show(cardPanel, "Dashboard");
        });

        frame.add(sidebar, BorderLayout.WEST);
        frame.add(cardPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    static void updateHistoryArea() {
        historyArea.setText("");
        for (String order : orderList) {
            historyArea.append(order + "\n");
        }
    }

    static void updateDashboard() {
        totalOrdersLabel.setText("Total Orders: " + orderList.size());
        revenueLabel.setText(String.format("Total Revenue: ₱%.2f", totalRevenue));
        String mostUsed = "N/A";
        int max = 0;
        for (String key : serviceCounts.keySet()) {
            if (serviceCounts.get(key) > max) {
                max = serviceCounts.get(key);
                mostUsed = key;
            }
        }
        mostUsedServiceLabel.setText("Most Used Service: " + mostUsed);
    }

    static void useSupplies(String service) {
        if (service.contains("Wash")) inventory.put("Detergent", Math.max(0, inventory.get("Detergent") - 1));
        if (service.contains("Dry")) inventory.put("Fabric Softener", Math.max(0, inventory.get("Fabric Softener") - 1));
        inventory.put("Laundry Bags", Math.max(0, inventory.get("Laundry Bags") - 1));
    }

    static void updateInventoryPanel(JPanel panel) {
        panel.removeAll();
        panel.add(new JLabel("---- Supplies ----"));
        for (String item : inventory.keySet()) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel name = new JLabel(item + ": ");
            JLabel count = new JLabel(String.valueOf(inventory.get(item)));
            if (inventory.get(item) < 5) count.setForeground(Color.RED);
            JButton addBtn = new JButton("+");
            JButton subBtn = new JButton("-");

            addBtn.addActionListener(e -> {
                inventory.put(item, inventory.get(item) + 1);
                updateInventoryPanel(panel);
                updateMachinePanel(panel);
                saveInventory();
            });
            subBtn.addActionListener(e -> {
                if (inventory.get(item) > 0) {
                    inventory.put(item, inventory.get(item) - 1);
                    updateInventoryPanel(panel);
                    updateMachinePanel(panel);
                    saveInventory();
                }
            });

            row.add(name);
            row.add(count);
            row.add(addBtn);
            row.add(subBtn);
            panel.add(row);
        }
    }

    static void updateMachinePanel(JPanel panel) {
        panel.add(new JLabel("---- Machine Status ----"));
        for (String machine : machines.keySet()) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel label = new JLabel(machine + ": ");
            JLabel status = new JLabel(machines.get(machine) ? "Operational" : "OUT OF ORDER");
            status.setForeground(machines.get(machine) ? Color.GREEN : Color.RED);
            JButton toggle = new JButton("Toggle");
            toggle.addActionListener(e -> {
                machines.put(machine, !machines.get(machine));
                updateInventoryPanel(panel);
                updateMachinePanel(panel);
            });
            row.add(label);
            row.add(status);
            row.add(toggle);
            panel.add(row);
        }
        panel.revalidate();
        panel.repaint();
    }

    static void saveOrders() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("orders.txt"))) {
            for (String order : orderList) pw.println(order);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void loadOrders() {
        try (BufferedReader br = new BufferedReader(new FileReader("orders.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                orderList.add(line);
                try {
                    if (line.contains("₱")) {
                        String[] parts = line.split("₱");
                        totalRevenue += Double.parseDouble(parts[1].trim());
                    }
                    if (line.contains("Service: ")) {
                        String service = line.split("Service: ")[1].split(",")[0].trim();
                        serviceCounts.put(service, serviceCounts.getOrDefault(service, 0) + 1);
                    }
                } catch (Exception ignored) {}
            }
        } catch (IOException ignored) {}
    }

    static void saveInventory() {
        try (PrintWriter pw = new PrintWriter(new FileWriter("inventory.txt"))) {
            for (String item : inventory.keySet()) {
                pw.println(item + "," + inventory.get(item));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void loadInventory() {
        try (BufferedReader br = new BufferedReader(new FileReader("inventory.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                inventory.put(parts[0], Integer.parseInt(parts[1]));
            }
        } catch (IOException e) {
            inventory.put("Detergent", 10);
            inventory.put("Fabric Softener", 5);
            inventory.put("Laundry Bags", 20);
            inventory.put("Bleach", 3);
        }
    }
}
