# Plan 07 — Client: Java 8 Swing Application

## Output files
```
banking-client/src/main/java/com/legacybank/client/BankingApplication.java
banking-client/src/main/java/com/legacybank/client/LoginPanel.java
banking-client/src/main/java/com/legacybank/client/DashboardFrame.java
banking-client/src/main/java/com/legacybank/client/BalancePanel.java
banking-client/src/main/java/com/legacybank/client/TransferPanel.java
banking-client/src/main/java/com/legacybank/client/HistoryPanel.java
```

---

## Design principles (legacy Swing style)

- **No lambdas** — use anonymous inner class `ActionListener` implementations
- **No generics on UI components** — use raw `DefaultTableModel`, `Vector` for table data
- **`GridBagLayout`** everywhere (authentic 2000s enterprise layout manager)
- **`UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel")`** with fallback
- **Blocking SOAP calls on the Event Dispatch Thread** (not in a `SwingWorker`) — old code was synchronous
- **`JOptionPane.showMessageDialog`** for all feedback — no toasts, no status bars (mostly)
- **Hardcoded sizes** via `setPreferredSize(new Dimension(w, h))` — no responsive layout
- **Global stub instance** created once in `BankingApplication`, passed to all panels
- **`System.out.println`** for logging

---

## 1. `BankingApplication.java` — Entry point

```
package com.legacybank.client;

public class BankingApplication {

    // Shared SOAP stub — created once at application startup
    private static BankingService soapPort;

    public static void main(String[] args) {
        // 1. Set Windows Look and Feel (with fallback to system default)
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            // Ignore — run with default L&F
        }

        // 2. Initialise SOAP stub
        try {
            BankingService_Service service = new BankingService_Service();
            soapPort = service.getBankingServicePort();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Cannot connect to banking service:\n" + e.getMessage(),
                "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // 3. Show login frame on the EDT
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame loginFrame = new JFrame("Legacy Banking System — Login");
                loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                loginFrame.setContentPane(new LoginPanel(loginFrame, soapPort));
                loginFrame.pack();
                loginFrame.setLocationRelativeTo(null);
                loginFrame.setVisible(true);
            }
        });
    }
}
```

---

## 2. `LoginPanel.java`

### Layout
`GridBagLayout` with two rows:
```
Row 0: [Label: "Account Number:"] [JTextField(22)]
Row 1: [Button: "Login" (colspan 2)]
```

### Fields
- `JTextField accountNumberField` — 22 columns (IBAN length)
- `JLabel titleLabel` — "LEGACY BANKING SYSTEM" in large bold font (`new Font("Serif", Font.BOLD, 18)`)
- `JButton loginButton`

### Logic
```
ActionListener on loginButton:
  1. String accountNumber = accountNumberField.getText().trim()
  2. If blank: JOptionPane.showMessageDialog(parent, "Please enter your account number.")
  3. Call soapPort.getAccountInfo(accountNumber)
  4. If result.getOwnerName().equals("NOT FOUND"):
       JOptionPane.showMessageDialog(parent, "Account not found.")
  5. Else:
       loginFrame.setVisible(false)
       loginFrame.dispose()
       DashboardFrame dash = new DashboardFrame(accountNumber, result.getOwnerName(), soapPort)
       dash.setVisible(true)
```

### Title panel at the top
```
JPanel titlePanel with grey background (Color.LIGHT_GRAY)
Contains: titleLabel centred
Border: EmptyBorder(10, 10, 10, 10)
```

---

## 3. `DashboardFrame.java`

### Fields
- `String currentAccountNumber`
- `String currentOwnerName`
- `BankingService soapPort`

### Layout
```
JFrame
  ├── JPanel (BorderLayout.NORTH) — banner with "Welcome, <ownerName>" and account number
  └── JTabbedPane (BorderLayout.CENTER)
        ├── Tab "Balance"   → new BalancePanel(...)
        ├── Tab "Transfer"  → new TransferPanel(...)
        └── Tab "History"   → new HistoryPanel(...)
```

### Constructor
```java
public DashboardFrame(String accountNumber, String ownerName, BankingService soapPort) {
    super("Legacy Banking System — " + ownerName);
    this.currentAccountNumber = accountNumber;
    this.currentOwnerName = ownerName;
    this.soapPort = soapPort;

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(700, 500);   // hardcoded size
    setLayout(new BorderLayout());

    // Banner panel
    JPanel banner = new JPanel(new GridLayout(2, 1));
    banner.setBackground(new Color(0, 70, 127));  // old-school dark blue
    JLabel welcomeLabel = new JLabel("  Welcome, " + ownerName, JLabel.LEFT);
    welcomeLabel.setForeground(Color.WHITE);
    welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    JLabel accountLabel = new JLabel("  Account: " + accountNumber, JLabel.LEFT);
    accountLabel.setForeground(Color.LIGHT_GRAY);
    accountLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
    banner.add(welcomeLabel);
    banner.add(accountLabel);
    add(banner, BorderLayout.NORTH);

    // Tabs
    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Balance",  new BalancePanel(accountNumber, soapPort));
    tabs.addTab("Transfer", new TransferPanel(accountNumber, soapPort));
    tabs.addTab("History",  new HistoryPanel(accountNumber, soapPort));
    add(tabs, BorderLayout.CENTER);

    setLocationRelativeTo(null);
}
```

---

## 4. `BalancePanel.java`

### Layout
```
GridBagLayout:
  Row 0: [Label "Current Balance"] (centred, spanning 2 cols)
  Row 1: [balanceValueLabel: "Loading..."] (large bold font, centred)
  Row 2: [Button "Refresh Balance"]
```

### Logic
```
On construction: call refreshBalance()

refreshBalance():
  double balance = soapPort.getBalance(accountNumber)
  balanceValueLabel.setText(String.format("EUR %.2f", balance))

ActionListener on refreshButton:
  refreshBalance()
```

### Style
- `balanceValueLabel` font: `new Font("Monospaced", Font.BOLD, 28)`
- `balanceValueLabel` foreground: `new Color(0, 128, 0)` (green)

---

## 5. `TransferPanel.java`

### Layout
```
GridBagLayout:
  Row 0: [Label "To Account Number:"] [JTextField(22) toAccountField]
  Row 1: [Label "Amount (EUR):"]       [JTextField(10) amountField]
  Row 2: [Label "Description:"]        [JTextField(30) descriptionField]
  Row 3: [Button "Send Money"] (colspan 2, centred)
  Row 4: [statusLabel] (colspan 2)
```

### Logic
```
ActionListener on sendButton:
  1. Parse toAccount = toAccountField.getText().trim()
  2. Parse amount from amountField — catch NumberFormatException, show error dialog
  3. Reject if amount <= 0
  4. description = descriptionField.getText().trim() || "Transfer"
  5. boolean success = soapPort.transfer(currentAccountNumber, toAccount, amount, description)
  6. If success:
       statusLabel.setText("Transfer successful!")
       statusLabel.setForeground(new Color(0, 128, 0))
       JOptionPane.showMessageDialog(this, "EUR " + amount + " sent to " + toAccount + " successfully.")
       Clear fields
  7. Else:
       statusLabel.setText("Transfer failed.")
       statusLabel.setForeground(Color.RED)
       JOptionPane.showMessageDialog(this, "Transfer failed. Check account number and balance.",
           "Transfer Error", JOptionPane.ERROR_MESSAGE)
```

### Style
- `sendButton` background: `new Color(0, 100, 0)`, foreground: `Color.WHITE`
- `statusLabel` starts empty

---

## 6. `HistoryPanel.java`

### Layout
```
BorderLayout:
  NORTH:  JPanel with [Button "Refresh"] right-aligned
  CENTER: JScrollPane wrapping JTable
```

### Table
```java
// Old-style: raw types (pre-generics style preserved)
Vector columnNames = new Vector();
columnNames.add("ID");
columnNames.add("From");
columnNames.add("To");
columnNames.add("Amount (EUR)");
columnNames.add("Description");
columnNames.add("Date");

Vector data = new Vector();  // Vector of Vectors

DefaultTableModel model = new DefaultTableModel(data, columnNames) {
    public boolean isCellEditable(int row, int col) {
        return false;  // read-only table
    }
};
JTable table = new JTable(model);
```

### Column widths (old hardcoded approach)
```java
table.getColumnModel().getColumn(0).setPreferredWidth(40);   // ID
table.getColumnModel().getColumn(1).setPreferredWidth(180);  // From
table.getColumnModel().getColumn(2).setPreferredWidth(180);  // To
table.getColumnModel().getColumn(3).setPreferredWidth(90);   // Amount
table.getColumnModel().getColumn(4).setPreferredWidth(170);  // Description
table.getColumnModel().getColumn(5).setPreferredWidth(130);  // Date
```

### Logic
```
On construction: call loadHistory()

loadHistory():
  List<TransactionRecord> records = soapPort.getTransactionHistory(accountNumber)
  model.setRowCount(0)  // clear existing rows
  for each record in records:
    Vector row = new Vector()
    row.add(record.getId())
    row.add(record.getFromAccount() != null ? record.getFromAccount() : "(external)")
    row.add(record.getToAccount()   != null ? record.getToAccount()   : "(external)")
    row.add(String.format("%.2f", record.getAmount()))
    row.add(record.getDescription())
    row.add(record.getTransactionDate())
    model.addRow(row)

ActionListener on refreshButton:
  loadHistory()
```

### Table header style (old style)
```java
table.getTableHeader().setBackground(new Color(0, 70, 127));
table.getTableHeader().setForeground(Color.WHITE);
table.setRowHeight(20);
table.setGridColor(Color.LIGHT_GRAY);
table.setSelectionBackground(new Color(184, 207, 229));
```

---

## Error handling style

Every SOAP call in the Swing code is wrapped:
```java
try {
    // ... SOAP call ...
} catch (Exception e) {
    JOptionPane.showMessageDialog(this,
        "Service error: " + e.getMessage(),
        "Error", JOptionPane.ERROR_MESSAGE);
    System.err.println("SOAP error: " + e.getMessage());
}
```

No `WebServiceException` specific handling — catch all `Exception` (old style).

---

## Import organisation

All Swing classes import:
```java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import com.legacybank.client.stub.*;
import javax.xml.ws.BindingProvider;
```

---

## Window sizing

| Frame/Panel | Size |
|---|---|
| Login frame | `pack()` — natural size |
| Dashboard frame | `setSize(700, 500)` |
| BalancePanel | fills tab |
| TransferPanel | fills tab |
| HistoryPanel | fills tab (table scrolls) |
