package com.splitmate.service;

import com.splitmate.dao.ExpenseDAO;
import com.splitmate.dao.SettlementDAO;
import java.math.BigDecimal;
import java.util.*;

/**
 * Net balance = paid - owed + settlements sent (PAID) - settlements received (PAID).
 * Positive: the group owes this person. Negative: this person owes the group.
 */
public class BalanceService {
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final SettlementDAO settlementDAO = new SettlementDAO();

    public static class Transfer {
        public final int from, to;
        public final BigDecimal amount;
        public Transfer(int from, int to, BigDecimal amount) { this.from = from; this.to = to; this.amount = amount; }
    }

    public Map<Integer, BigDecimal> netBalances(int groupId, List<Integer> memberIds) {
        Map<Integer, BigDecimal> paid = expenseDAO.paidTotals(groupId);
        Map<Integer, BigDecimal> owed = expenseDAO.owedTotals(groupId);
        Map<Integer, BigDecimal> out = settlementDAO.paidOut(groupId);
        Map<Integer, BigDecimal> in = settlementDAO.paidIn(groupId);

        Map<Integer, BigDecimal> net = new LinkedHashMap<>();
        for (int id : memberIds) {
            BigDecimal b = get(paid, id).subtract(get(owed, id)).add(get(out, id)).subtract(get(in, id));
            net.put(id, b);
        }
        return net;
    }

    /** Greedy minimal-transfer suggestion: biggest debtor pays biggest creditor. */
    public List<Transfer> suggestTransfers(Map<Integer, BigDecimal> net) {
        List<Map.Entry<Integer, BigDecimal>> debtors = new ArrayList<>();
        List<Map.Entry<Integer, BigDecimal>> creditors = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> e : net.entrySet()) {
            if (e.getValue().signum() < 0) debtors.add(new AbstractMap.SimpleEntry<>(e));
            else if (e.getValue().signum() > 0) creditors.add(new AbstractMap.SimpleEntry<>(e));
        }
        debtors.sort(Comparator.comparing(Map.Entry<Integer, BigDecimal>::getValue));          // most negative first
        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));                         // most positive first

        List<Transfer> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<Integer, BigDecimal> d = debtors.get(i);
            Map.Entry<Integer, BigDecimal> c = creditors.get(j);
            BigDecimal pay = d.getValue().negate().min(c.getValue());
            result.add(new Transfer(d.getKey(), c.getKey(), pay));
            d.setValue(d.getValue().add(pay));
            c.setValue(c.getValue().subtract(pay));
            if (d.getValue().signum() == 0) i++;
            if (c.getValue().signum() == 0) j++;
        }
        return result;
    }

    private BigDecimal get(Map<Integer, BigDecimal> m, int id) {
        return m.getOrDefault(id, BigDecimal.ZERO);
    }
}
