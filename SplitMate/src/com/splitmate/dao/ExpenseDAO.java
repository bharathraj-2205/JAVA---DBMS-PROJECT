package com.splitmate.dao;

import com.splitmate.db.DBConnection;
import com.splitmate.exception.SplitMateException;
import com.splitmate.model.Expense;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class ExpenseDAO {

    /** Inserts the expense and all its splits atomically. */
    public int add(Expense e, Map<Integer, BigDecimal> shares) {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO expenses (group_id, paid_by, description, amount, expense_date, split_type) "
                      + "VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, e.getGroupId());
                    ps.setInt(2, e.getPaidBy());
                    ps.setString(3, e.getDescription());
                    ps.setBigDecimal(4, e.getAmount());
                    ps.setDate(5, java.sql.Date.valueOf(e.getDate()));
                    ps.setString(6, e.getSplitType());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        e.setId(rs.getInt(1));
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO expense_splits (expense_id, user_id, share_amount) VALUES (?,?,?)")) {
                    for (Map.Entry<Integer, BigDecimal> s : shares.entrySet()) {
                        ps.setInt(1, e.getId());
                        ps.setInt(2, s.getKey());
                        ps.setBigDecimal(3, s.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return e.getId();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new SplitMateException("Could not save expense: " + ex.getMessage(), ex);
        }
    }

    public List<Expense> listByGroup(int groupId) {
        List<Expense> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM expenses WHERE group_id = ? ORDER BY expense_date DESC, expense_id DESC")) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new SplitMateException("Database error: " + e.getMessage(), e);
        }
        return list;
    }

    public Expense findById(int expenseId) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM expenses WHERE expense_id = ?")) {
            ps.setInt(1, expenseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new SplitMateException("Database error: " + e.getMessage(), e);
        }
    }

    public Map<Integer, BigDecimal> getSplits(int expenseId) {
        Map<Integer, BigDecimal> map = new LinkedHashMap<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT user_id, share_amount FROM expense_splits WHERE expense_id = ? ORDER BY user_id")) {
            ps.setInt(1, expenseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getInt(1), rs.getBigDecimal(2));
            }
        } catch (SQLException e) {
            throw new SplitMateException("Database error: " + e.getMessage(), e);
        }
        return map;
    }

    /** Only description and date can be edited; amount/split changes need delete + re-add. */
    public void update(Expense e) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE expenses SET description = ?, expense_date = ? WHERE expense_id = ?")) {
            ps.setString(1, e.getDescription());
            ps.setDate(2, java.sql.Date.valueOf(e.getDate()));
            ps.setInt(3, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new SplitMateException("Could not update expense: " + ex.getMessage(), ex);
        }
    }

    public void delete(int expenseId) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM expenses WHERE expense_id = ?")) {
            ps.setInt(1, expenseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SplitMateException("Could not delete expense: " + e.getMessage(), e);
        }
    }

    /** userId -> total paid in this group. */
    public Map<Integer, BigDecimal> paidTotals(int groupId) {
        return sumQuery("SELECT paid_by, SUM(amount) FROM expenses WHERE group_id = ? GROUP BY paid_by", groupId);
    }

    /** userId -> total share owed in this group. */
    public Map<Integer, BigDecimal> owedTotals(int groupId) {
        return sumQuery("SELECT s.user_id, SUM(s.share_amount) FROM expense_splits s "
                      + "JOIN expenses e ON s.expense_id = e.expense_id WHERE e.group_id = ? GROUP BY s.user_id", groupId);
    }

    private Map<Integer, BigDecimal> sumQuery(String sql, int groupId) {
        Map<Integer, BigDecimal> map = new HashMap<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getInt(1), rs.getBigDecimal(2));
            }
        } catch (SQLException e) {
            throw new SplitMateException("Database error: " + e.getMessage(), e);
        }
        return map;
    }

    private Expense map(ResultSet rs) throws SQLException {
        return new Expense(rs.getInt("expense_id"), rs.getInt("group_id"), rs.getInt("paid_by"),
            rs.getString("description"), rs.getBigDecimal("amount"),
            rs.getDate("expense_date").toLocalDate(), rs.getString("split_type"));
    }
}
