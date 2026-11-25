package com.splitmate.dao;

import com.splitmate.db.DBConnection;
import com.splitmate.exception.SplitMateException;
import com.splitmate.model.Settlement;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class SettlementDAO {

    public int add(Settlement s) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO settlements (group_id, from_user, to_user, amount, status) VALUES (?,?,?,?,?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getGroupId());
            ps.setInt(2, s.getFromUser());
            ps.setInt(3, s.getToUser());
            ps.setBigDecimal(4, s.getAmount());
            ps.setString(5, s.getStatus().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                s.setId(rs.getInt(1));
                return s.getId();
            }
        } catch (SQLException e) {
            throw new SplitMateException("Could not record settlement: " + e.getMessage(), e);
        }
    }

    public List<Settlement> listByGroup(int groupId) {
        List<Settlement> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM settlements WHERE group_id = ? ORDER BY settlement_id DESC")) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new SplitMateException("Database error: " + e.getMessage(), e);
        }
        return list;
    }

    public Settlement findById(int id) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM settlements WHERE settlement_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new SplitMateException("Database error: " + e.getMessage(), e);
        }
    }

    public void updateStatus(int id, Settlement.Status status) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE settlements SET status = ? WHERE settlement_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SplitMateException("Could not update settlement: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM settlements WHERE settlement_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SplitMateException("Could not delete settlement: " + e.getMessage(), e);
        }
    }

    /** userId -> total of PAID settlements this user has sent out. */
    public Map<Integer, BigDecimal> paidOut(int groupId) {
        return sumQuery("SELECT from_user, SUM(amount) FROM settlements WHERE group_id = ? AND status = 'PAID' GROUP BY from_user", groupId);
    }

    /** userId -> total of PAID settlements this user has received. */
    public Map<Integer, BigDecimal> paidIn(int groupId) {
        return sumQuery("SELECT to_user, SUM(amount) FROM settlements WHERE group_id = ? AND status = 'PAID' GROUP BY to_user", groupId);
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

    private Settlement map(ResultSet rs) throws SQLException {
        return new Settlement(rs.getInt("settlement_id"), rs.getInt("group_id"), rs.getInt("from_user"),
            rs.getInt("to_user"), rs.getBigDecimal("amount"), Settlement.Status.valueOf(rs.getString("status")));
    }
}
