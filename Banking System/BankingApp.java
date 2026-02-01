import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

// Serializable Account class
class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accountNumber;
    private String accountHolder;
    private String password;
    private double balance;
    private final java.util.List<String> transactionHistory;

    public Account(String accountNumber, String accountHolder, String password, double initialBalance) {
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.password = password;
        this.balance = initialBalance;
        transactionHistory = new ArrayList<>();
        transactionHistory.add("Account created with balance: " + initialBalance);
    }

    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolder() { return accountHolder; }
    public double getBalance() { return balance; }
    public boolean checkPassword(String pass) { return password.equals(pass); }

    public void deposit(double amount) {
        balance += amount;
        transactionHistory.add("Deposited: " + amount + " | New Balance: " + balance);
    }

    public boolean withdraw(double amount) {
        if (amount > balance) return false;
        balance -= amount;
        transactionHistory.add("Withdrawn: " + amount + " | New Balance: " + balance);
        return true;
    }

    public boolean transfer(Account toAccount, double amount) {
        if (amount > balance) return false;
        balance -= amount;
        toAccount.deposit(amount);
        transactionHistory.add("Transferred: " + amount + " to " + toAccount.getAccountNumber());
        return true;
    }

    public String getTransactionHistory() {
        StringBuilder sb = new StringBuilder();
        for (String t : transactionHistory) sb.append(t).append("\n");
        return sb.toString();
    }
}

// Main Banking App GUI
public class BankingApp extends JFrame {
    private Map<String, Account> accounts;
    private final String DATA_FILE = "accounts.dat";

    public BankingApp() {
        loadAccounts();

        setTitle("Secure Online Banking");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Arial", Font.BOLD, 14));
        tabs.setBackground(new Color(230, 230, 250));

        // Adding all tabs
        tabs.add("Create Account", createAccountPanel());
        tabs.add("Login & Transactions", loginTransactionPanel());
        tabs.add("Transfer Funds", transferPanel());
        tabs.add("Delete Account", deleteAccountPanel());

        add(tabs);
        setVisible(true);
    }

    // ---------------- Styled Helpers ----------------
    private JPanel styledPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(245, 245, 255));
        return panel;
    }

    private JLabel styledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        return label;
    }

    private JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(new Color(65, 105, 225));
        btn.setForeground(Color.WHITE);
        return btn;
    }

    private JTextField styledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        return field;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        return field;
    }

    private JTextArea styledTextArea() {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBackground(new Color(230, 230, 250));
        area.setEditable(false);
        return area;
    }

    // ---------------- Create Account ----------------
    private JPanel createAccountPanel() {
        JPanel panel = styledPanel();
        panel.setLayout(new GridLayout(6, 2, 15, 15));

        JTextField accNumField = styledTextField();
        JTextField nameField = styledTextField();
        JPasswordField passwordField = styledPasswordField();
        JTextField balanceField = styledTextField();
        JButton createBtn = styledButton("Create Account");
        JLabel msgLabel = styledLabel("");

        panel.add(styledLabel("Account Number:")); panel.add(accNumField);
        panel.add(styledLabel("Account Holder Name:")); panel.add(nameField);
        panel.add(styledLabel("Password:")); panel.add(passwordField);
        panel.add(styledLabel("Initial Balance:")); panel.add(balanceField);
        panel.add(createBtn); panel.add(msgLabel);

        createBtn.addActionListener(e -> {
            String accNum = accNumField.getText();
            String name = nameField.getText();
            String password = new String(passwordField.getPassword());
            double balance;
            try { balance = Double.parseDouble(balanceField.getText()); }
            catch(Exception ex) { msgLabel.setText("Invalid balance!"); return; }

            if(accounts.containsKey(accNum)) msgLabel.setText("Account already exists!");
            else {
                accounts.put(accNum, new Account(accNum, name, password, balance));
                saveAccounts();
                msgLabel.setText("Account created successfully!");
            }
        });

        return panel;
    }

    // ---------------- Login & Transactions (with Check Balance) ----------------
    private JPanel loginTransactionPanel() {
        JPanel panel = styledPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField accNumField = styledTextField();
        JPasswordField passwordField = styledPasswordField();
        JTextField amountField = styledTextField();
        JButton loginBtn = styledButton("Login");
        JButton depositBtn = styledButton("Deposit");
        JButton withdrawBtn = styledButton("Withdraw");
        JButton checkBalanceBtn = styledButton("Check Balance");
        JButton historyBtn = styledButton("Transaction History");
        JLabel msgLabel = styledLabel("");
        JTextArea historyArea = styledTextArea();
        JScrollPane scroll = new JScrollPane(historyArea);

        final Account[] loggedIn = {null};

        gbc.gridx=0; gbc.gridy=0; panel.add(styledLabel("Account Number:"), gbc);
        gbc.gridx=1; panel.add(accNumField, gbc);
        gbc.gridx=0; gbc.gridy=1; panel.add(styledLabel("Password:"), gbc);
        gbc.gridx=1; panel.add(passwordField, gbc);
        gbc.gridx=0; gbc.gridy=2; panel.add(styledLabel("Amount:"), gbc);
        gbc.gridx=1; panel.add(amountField, gbc);
        gbc.gridx=0; gbc.gridy=3; panel.add(loginBtn, gbc);
        gbc.gridx=1; panel.add(msgLabel, gbc);
        gbc.gridx=0; gbc.gridy=4; panel.add(depositBtn, gbc);
        gbc.gridx=1; panel.add(withdrawBtn, gbc);
        gbc.gridx=0; gbc.gridy=5; panel.add(checkBalanceBtn, gbc);
        gbc.gridx=1; panel.add(historyBtn, gbc);
        gbc.gridx=0; gbc.gridy=6; gbc.gridwidth=2; gbc.weightx=1; gbc.weighty=1;
        gbc.fill = GridBagConstraints.BOTH; panel.add(scroll, gbc);

        loginBtn.addActionListener(e -> {
            Account acc = accounts.get(accNumField.getText());
            if(acc != null && acc.checkPassword(new String(passwordField.getPassword()))) {
                loggedIn[0] = acc;
                msgLabel.setText("Login successful!");
            } else msgLabel.setText("Invalid credentials!");
        });

        depositBtn.addActionListener(e -> {
            if(loggedIn[0]==null){ msgLabel.setText("Login first!"); return; }
            try { double amt = Double.parseDouble(amountField.getText());
                loggedIn[0].deposit(amt); saveAccounts();
                msgLabel.setText("Deposited! New Balance: " + loggedIn[0].getBalance());
            } catch(Exception ex){ msgLabel.setText("Invalid amount!"); }
        });

        withdrawBtn.addActionListener(e -> {
            if(loggedIn[0]==null){ msgLabel.setText("Login first!"); return; }
            try { double amt = Double.parseDouble(amountField.getText());
                if(loggedIn[0].withdraw(amt)){ saveAccounts();
                    msgLabel.setText("Withdrawn! New Balance: " + loggedIn[0].getBalance());
                } else msgLabel.setText("Insufficient balance!");
            } catch(Exception ex){ msgLabel.setText("Invalid amount!"); }
        });

        checkBalanceBtn.addActionListener(e -> {
            if(loggedIn[0]==null){ msgLabel.setText("Login first!"); return; }
            msgLabel.setText("Current Balance: " + loggedIn[0].getBalance());
        });

        historyBtn.addActionListener(e -> {
            if(loggedIn[0]==null){ msgLabel.setText("Login first!"); return; }
            historyArea.setText(loggedIn[0].getTransactionHistory());
        });

        return panel;
    }

    // ---------------- Transfer Funds ----------------
    private JPanel transferPanel() {
        JPanel panel = styledPanel();
        panel.setLayout(new GridLayout(5,2,15,15));

        JTextField senderField = styledTextField();
        JPasswordField passwordField = styledPasswordField();
        JTextField receiverField = styledTextField();
        JTextField amountField = styledTextField();
        JButton transferBtn = styledButton("Transfer");
        JLabel msgLabel = styledLabel("");

        panel.add(styledLabel("Sender Account Number:")); panel.add(senderField);
        panel.add(styledLabel("Password:")); panel.add(passwordField);
        panel.add(styledLabel("Receiver Account Number:")); panel.add(receiverField);
        panel.add(styledLabel("Amount:")); panel.add(amountField);
        panel.add(transferBtn); panel.add(msgLabel);

        transferBtn.addActionListener(e -> {
            Account sender = accounts.get(senderField.getText());
            Account receiver = accounts.get(receiverField.getText());
            if(sender==null || receiver==null){ msgLabel.setText("Invalid account(s)!"); return; }
            if(!sender.checkPassword(new String(passwordField.getPassword()))){ msgLabel.setText("Wrong password!"); return; }
            try { double amt = Double.parseDouble(amountField.getText());
                if(sender.transfer(receiver, amt)){ saveAccounts(); msgLabel.setText("Transfer successful!"); }
                else msgLabel.setText("Insufficient balance!");
            } catch(Exception ex){ msgLabel.setText("Invalid amount!"); }
        });

        return panel;
    }

    // ---------------- Delete Account ----------------
    private JPanel deleteAccountPanel() {
        JPanel panel = styledPanel();
        panel.setLayout(new GridLayout(3,2,15,15));

        JTextField accNumField = styledTextField();
        JPasswordField passwordField = styledPasswordField();
        JButton deleteBtn = styledButton("Delete Account");
        JLabel msgLabel = styledLabel("");

        panel.add(styledLabel("Account Number:")); panel.add(accNumField);
        panel.add(styledLabel("Password:")); panel.add(passwordField);
        panel.add(deleteBtn); panel.add(msgLabel);

        deleteBtn.addActionListener(e -> {
            String accNum = accNumField.getText();
            String password = new String(passwordField.getPassword());
            Account acc = accounts.get(accNum);
            if(acc==null){ msgLabel.setText("Account not found!"); return; }
            if(!acc.checkPassword(password)){ msgLabel.setText("Wrong password!"); return; }
            accounts.remove(accNum); saveAccounts();
            msgLabel.setText("Account deleted successfully!");
        });

        return panel;
    }

    // ---------------- Persistence ----------------
    @SuppressWarnings("unchecked")
    private void loadAccounts() {
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))){
            accounts = (Map<String, Account>)ois.readObject();
        } catch(Exception e){ accounts = new HashMap<>(); }
    }

    private void saveAccounts() {
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))){
            oos.writeObject(accounts);
        } catch(Exception e){ JOptionPane.showMessageDialog(this,"Error saving accounts!"); }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(BankingApp::new);
    }
}
