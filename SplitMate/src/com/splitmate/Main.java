package com.splitmate;

import com.splitmate.dao.*;
import com.splitmate.exception.SplitMateException;
import com.splitmate.model.*;
import com.splitmate.service.BalanceService;
import com.splitmate.split.SplitFactory;
import com.splitmate.split.SplitStrategy;
import com.splitmate.util.PasswordUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/** SplitMate - Shared Expense Management System (CLI). */
public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static final UserDAO userDAO = new UserDAO();
    private static final GroupDAO groupDAO = new GroupDAO();
    private static final ExpenseDAO expenseDAO = new ExpenseDAO();
    private static final SettlementDAO settlementDAO = new SettlementDAO();
    private static final BalanceService balanceService = new BalanceService();
    private static User currentUser;

    public static void main(String[] args) {
        System.out.println("==============================");
        System.out.println("   SplitMate - Expense Sharing");
        System.out.println("==============================");
        boolean running = true;
        while (running) {
            try {
                System.out.println("\n1. Register\n2. Login\n0. Exit");
                switch (readInt("Choose: ")) {
                    case 1: register(); break;
                    case 2: if (login()) userMenu(); break;
                    case 0: running = false; break;
                    default: System.out.println("Invalid choice.");
                }
            } catch (SplitMateException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    // ---------------- Authentication ----------------
    private static void register() {
        String name = readLine("Name: ");
        String email = readLine("Email: ").toLowerCase();
        String pw = readLine("Password (min 6 chars): ");
        if (name.isEmpty() || !email.contains("@")) throw new SplitMateException("Enter a valid name and email.");
        if (pw.length() < 6) throw new SplitMateException("Password too short.");
        int id = userDAO.register(new User(name, email, PasswordUtil.hash(pw)));
        System.out.println("Registered! Your user id is " + id + ".");
    }

    private static boolean login() {
        String email = readLine("Email: ").toLowerCase();
        String pw = readLine("Password: ");
        User u = userDAO.findByEmail(email);
        if (u == null || !PasswordUtil.verify(pw, u.getPasswordHash())) {
            System.out.println("Invalid email or password.");
            return false;
        }
        currentUser = u;
        System.out.println("Welcome, " + u.getName() + "!");
        return true;
    }

    // ---------------- User menu ----------------
    private static void userMenu() {
        boolean back = false;
        while (!back) {
            try {
                System.out.println("\n--- Main Menu (" + currentUser.getName() + ") ---");
                System.out.println("1. Create group\n2. My groups\n3. Open a group\n4. Update my name\n0. Logout");
                switch (readInt("Choose: ")) {
                    case 1: createGroup(); break;
                    case 2: listGroups(); break;
                    case 3: openGroup(); break;
                    case 4: updateProfile(); break;
                    case 0: currentUser = null; back = true; break;
                    default: System.out.println("Invalid choice.");
                }
            } catch (SplitMateException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void createGroup() {
        String name = readLine("Group name: ");
        if (name.isEmpty()) throw new SplitMateException("Group name cannot be empty.");
        int id = groupDAO.create(new ExpenseGroup(name, currentUser.getId()));
        System.out.println("Group created with id " + id + ".");
    }

    private static void listGroups() {
        List<ExpenseGroup> groups = groupDAO.listForUser(currentUser.getId());
        if (groups.isEmpty()) { System.out.println("You are not in any group yet."); return; }
        groups.forEach(g -> System.out.println(g.describe() + (g.getCreatedBy() == currentUser.getId() ? "  (owner)" : "")));
    }

    private static void updateProfile() {
        String name = readLine("New name: ");
        if (name.isEmpty()) throw new SplitMateException("Name cannot be empty.");
        userDAO.updateName(currentUser.getId(), name);
        currentUser.setName(name);
        System.out.println("Name updated.");
    }

    private static void openGroup() {
        listGroups();
        int gid = readInt("Group id: ");
        ExpenseGroup g = groupDAO.findById(gid);
        if (g == null || !groupDAO.isMember(gid, currentUser.getId()))
            throw new SplitMateException("Group not found or you are not a member.");
        groupMenu(g);
    }

    // ---------------- Group menu ----------------
    private static void groupMenu(ExpenseGroup group) {
        boolean back = false;
        while (!back) {
            try {
                boolean owner = group.getCreatedBy() == currentUser.getId();
                System.out.println("\n--- Group: " + group.getName() + " ---");
                System.out.println("1. Dashboard (members, balances)\n2. Add member\n3. Add expense\n4. View expenses"
                    + "\n5. Expense details\n6. Update expense\n7. Delete expense\n8. Record settlement"
                    + "\n9. View settlements\n10. Update settlement status\n11. Delete settlement"
                    + (owner ? "\n12. Rename group\n13. Delete group" : "") + "\n0. Back");
                switch (readInt("Choose: ")) {
                    case 1: dashboard(group); break;
                    case 2: addMember(group); break;
                    case 3: addExpense(group); break;
                    case 4: viewExpenses(group); break;
                    case 5: expenseDetails(group); break;
                    case 6: updateExpense(group); break;
                    case 7: deleteExpense(group); break;
                    case 8: recordSettlement(group); break;
                    case 9: viewSettlements(group); break;
                    case 10: updateSettlement(group); break;
                    case 11: deleteSettlement(group); break;
                    case 12:
                        if (!owner) { System.out.println("Only the group owner can rename the group."); break; }
                        renameGroup(group);
                        break;
                    case 13:
                        if (!owner) { System.out.println("Only the group owner can delete the group."); break; }
                        if (deleteGroup(group)) back = true;
                        break;
                    case 0: back = true; break;
                    default: System.out.println("Invalid choice.");
                }
            } catch (SplitMateException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static Map<Integer, String> nameMap(int groupId) {
        Map<Integer, String> names = new LinkedHashMap<>();
        for (User u : groupDAO.getMembers(groupId)) names.put(u.getId(), u.getName());
        return names;
    }

    private static void dashboard(ExpenseGroup group) {
        Map<Integer, String> names = nameMap(group.getId());
        Map<Integer, BigDecimal> net = balanceService.netBalances(group.getId(), new ArrayList<>(names.keySet()));
        System.out.println("\nMembers & balances:");
        for (Map.Entry<Integer, String> e : names.entrySet()) {
            BigDecimal b = net.get(e.getKey());
            String state = b.signum() > 0 ? "gets back Rs." + b : b.signum() < 0 ? "owes Rs." + b.negate() : "settled up";
            System.out.printf("  [%d] %-15s %s%n", e.getKey(), e.getValue(), state);
        }
        List<BalanceService.Transfer> plan = balanceService.suggestTransfers(net);
        System.out.println("\nSuggested settlements:");
        if (plan.isEmpty()) System.out.println("  Everyone is settled up.");
        for (BalanceService.Transfer t : plan)
            System.out.printf("  %s pays %s Rs.%s%n", names.get(t.from), names.get(t.to), t.amount);

        System.out.println("\nTotal expenses: " + expenseDAO.listByGroup(group.getId()).size());
    }

    private static void addMember(ExpenseGroup group) {
        String email = readLine("Email of user to add: ").toLowerCase();
        User u = userDAO.findByEmail(email);
        if (u == null) throw new SplitMateException("No user registered with that email.");
        groupDAO.addMember(group.getId(), u.getId());
        System.out.println(u.getName() + " added to the group.");
    }

    // ---------------- Expenses ----------------
    private static void addExpense(ExpenseGroup group) {
        Map<Integer, String> names = nameMap(group.getId());
        String desc = readLine("Description: ");
        if (desc.isEmpty()) throw new SplitMateException("Description cannot be empty.");
        BigDecimal amount = readDecimal("Amount: ");
        if (amount.signum() <= 0) throw new SplitMateException("Amount must be positive.");
        amount = amount.setScale(2, java.math.RoundingMode.HALF_UP);
        LocalDate date = readDate("Date (yyyy-MM-dd, Enter = today): ");

        names.forEach((id, n) -> System.out.println("  [" + id + "] " + n));
        int paidBy = readIntDefault("Paid by (user id, Enter = you): ", currentUser.getId());
        if (!names.containsKey(paidBy)) throw new SplitMateException("Payer must be a group member.");

        System.out.println("Split type: 1. Equal  2. Exact amounts  3. Percentages");
        String type;
        switch (readInt("Choose: ")) {
            case 1: type = "EQUAL"; break;
            case 2: type = "EXACT"; break;
            case 3: type = "PERCENTAGE"; break;
            default: throw new SplitMateException("Invalid split type.");
        }

        Map<Integer, BigDecimal> inputs = new LinkedHashMap<>();
        if (type.equals("EQUAL")) {
            String who = readLine("Split between (comma-separated ids, Enter = everyone): ");
            if (who.isEmpty()) {
                for (int id : names.keySet()) inputs.put(id, BigDecimal.ZERO);
            } else {
                for (String part : who.split(",")) {
                    int id = parseInt(part.trim());
                    if (!names.containsKey(id)) throw new SplitMateException("User " + id + " is not in this group.");
                    inputs.put(id, BigDecimal.ZERO);
                }
            }
        } else {
            String unit = type.equals("EXACT") ? "amount" : "percent";
            for (Map.Entry<Integer, String> e : names.entrySet()) {
                BigDecimal v = readDecimal(e.getValue() + "'s " + unit + " (0 to skip): ");
                if (v.signum() < 0) throw new SplitMateException("Values cannot be negative.");
                if (v.signum() > 0) inputs.put(e.getKey(), v);
            }
        }

        SplitStrategy strategy = SplitFactory.forType(type);          // polymorphism in action
        Map<Integer, BigDecimal> shares = strategy.calculate(amount, inputs);

        Expense exp = new Expense(group.getId(), paidBy, desc, amount, date, type);
        int id = expenseDAO.add(exp, shares);
        System.out.println("Expense saved (id " + id + "). Shares:");
        shares.forEach((uid, s) -> System.out.printf("  %-15s Rs.%s%n", names.get(uid), s));
    }

    private static void viewExpenses(ExpenseGroup group) {
        Map<Integer, String> names = nameMap(group.getId());
        List<Expense> list = expenseDAO.listByGroup(group.getId());
        if (list.isEmpty()) { System.out.println("No expenses yet."); return; }
        for (Expense e : list)
            System.out.println(e.describe() + " | paid by " + names.getOrDefault(e.getPaidBy(), "?"));
    }

    private static Expense pickExpense(ExpenseGroup group) {
        viewExpenses(group);
        Expense e = expenseDAO.findById(readInt("Expense id: "));
        if (e == null || e.getGroupId() != group.getId()) throw new SplitMateException("Expense not found in this group.");
        return e;
    }

    private static void expenseDetails(ExpenseGroup group) {
        Map<Integer, String> names = nameMap(group.getId());
        Expense e = pickExpense(group);
        System.out.println(e.describe() + " | paid by " + names.get(e.getPaidBy()));
        expenseDAO.getSplits(e.getId()).forEach((uid, s) ->
            System.out.printf("  %-15s owes Rs.%s%n", names.getOrDefault(uid, "?"), s));
    }

    private static void updateExpense(ExpenseGroup group) {
        Expense e = pickExpense(group);
        requirePayerOrOwner(e, group);
        String desc = readLine("New description (Enter = keep): ");
        if (!desc.isEmpty()) e.setDescription(desc);
        String d = readLine("New date yyyy-MM-dd (Enter = keep): ");
        if (!d.isEmpty()) e.setDate(parseDate(d));
        expenseDAO.update(e);
        System.out.println("Expense updated. (To change amount or split, delete and re-add it.)");
    }

    private static void deleteExpense(ExpenseGroup group) {
        Expense e = pickExpense(group);
        requirePayerOrOwner(e, group);
        if (confirm("Delete this expense?")) { expenseDAO.delete(e.getId()); System.out.println("Deleted."); }
    }

    private static void requirePayerOrOwner(Expense e, ExpenseGroup g) {
        if (e.getPaidBy() != currentUser.getId() && g.getCreatedBy() != currentUser.getId())
            throw new SplitMateException("Only the payer or the group owner can change this expense.");
    }

    // ---------------- Settlements ----------------
    private static void recordSettlement(ExpenseGroup group) {
        Map<Integer, String> names = nameMap(group.getId());
        names.forEach((id, n) -> System.out.println("  [" + id + "] " + n));
        int from = readIntDefault("Who pays (user id, Enter = you): ", currentUser.getId());
        int to = readInt("Who receives (user id): ");
        if (!names.containsKey(from) || !names.containsKey(to)) throw new SplitMateException("Both must be group members.");
        if (from == to) throw new SplitMateException("Payer and receiver must be different.");
        BigDecimal amt = readDecimal("Amount: ").setScale(2, java.math.RoundingMode.HALF_UP);
        if (amt.signum() <= 0) throw new SplitMateException("Amount must be positive.");
        int id = settlementDAO.add(new Settlement(group.getId(), from, to, amt));
        System.out.println("Settlement recorded as PENDING (id " + id + ").");
    }

    private static void viewSettlements(ExpenseGroup group) {
        Map<Integer, String> names = nameMap(group.getId());
        List<Settlement> list = settlementDAO.listByGroup(group.getId());
        if (list.isEmpty()) { System.out.println("No settlements yet."); return; }
        for (Settlement s : list)
            System.out.printf("[%d] %s -> %s : Rs.%s (%s)%n", s.getId(), names.getOrDefault(s.getFromUser(), "?"),
                names.getOrDefault(s.getToUser(), "?"), s.getAmount(), s.getStatus());
    }

    private static Settlement pickSettlement(ExpenseGroup group) {
        viewSettlements(group);
        Settlement s = settlementDAO.findById(readInt("Settlement id: "));
        if (s == null || s.getGroupId() != group.getId()) throw new SplitMateException("Settlement not found in this group.");
        return s;
    }

    private static void updateSettlement(ExpenseGroup group) {
        Settlement s = pickSettlement(group);
        if (s.getStatus() != Settlement.Status.PENDING)
            throw new SplitMateException("Only PENDING settlements can be changed.");
        if (s.getToUser() != currentUser.getId() && s.getFromUser() != currentUser.getId())
            throw new SplitMateException("Only the payer or receiver can update this settlement.");
        System.out.println("1. Mark PAID  2. Mark CANCELLED");
        int c = readInt("Choose: ");
        if (c == 1) settlementDAO.updateStatus(s.getId(), Settlement.Status.PAID);
        else if (c == 2) settlementDAO.updateStatus(s.getId(), Settlement.Status.CANCELLED);
        else throw new SplitMateException("Invalid choice.");
        System.out.println("Settlement updated.");
    }

    private static void deleteSettlement(ExpenseGroup group) {
        Settlement s = pickSettlement(group);
        if (s.getFromUser() != currentUser.getId() && group.getCreatedBy() != currentUser.getId())
            throw new SplitMateException("Only the payer or the group owner can delete this settlement.");
        if (confirm("Delete this settlement?")) { settlementDAO.delete(s.getId()); System.out.println("Deleted."); }
    }

    // ---------------- Group owner actions ----------------
    private static void renameGroup(ExpenseGroup group) {
        String name = readLine("New group name: ");
        if (name.isEmpty()) throw new SplitMateException("Name cannot be empty.");
        groupDAO.rename(group.getId(), name);
        group.setName(name);
        System.out.println("Group renamed.");
    }

    private static boolean deleteGroup(ExpenseGroup group) {
        if (!confirm("This deletes ALL expenses and settlements in the group. Continue?")) return false;
        groupDAO.delete(group.getId());
        System.out.println("Group deleted.");
        return true;
    }

    // ---------------- Input helpers ----------------
    private static String readLine(String prompt) {
        System.out.print(prompt);
        return in.nextLine().trim();
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { throw new SplitMateException("'" + s + "' is not a valid number."); }
    }

    private static int readInt(String prompt) { return parseInt(readLine(prompt)); }

    private static int readIntDefault(String prompt, int def) {
        String s = readLine(prompt);
        return s.isEmpty() ? def : parseInt(s);
    }

    private static BigDecimal readDecimal(String prompt) {
        String s = readLine(prompt);
        try { return new BigDecimal(s); }
        catch (NumberFormatException e) { throw new SplitMateException("'" + s + "' is not a valid amount."); }
    }

    private static LocalDate readDate(String prompt) {
        String s = readLine(prompt);
        return s.isEmpty() ? LocalDate.now() : parseDate(s);
    }

    private static LocalDate parseDate(String s) {
        try { return LocalDate.parse(s); }
        catch (DateTimeParseException e) { throw new SplitMateException("Use the date format yyyy-MM-dd."); }
    }

    private static boolean confirm(String question) {
        return readLine(question + " (y/n): ").equalsIgnoreCase("y");
    }
}
