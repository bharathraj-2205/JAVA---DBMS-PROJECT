package com.splitmate.dao;

import com.splitmate.db.DBConnection;
import com.splitmate.exception.SplitMateException;
import com.splitmate.model.ExpenseGroup;
import com.splitmate.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupDAO {

    /** Creates the group and adds the creator as first member in one transaction. */
    public int create(ExpenseGroup g) {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO expense_groups (group_name, created_by) VALUES (?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, g.getName());
                ps.setInt(2, g.getCreatedBy());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    g.setId(rs.getInt(1));
                }
                try (PreparedStatement pm = c.prepareStatement(
                        "INSERT INTO group_members (group_id, user_id) VALUES (?,?)")) {
                    pm.setInt(1, g.getId());
                    pm.setInt(2, g.getCreatedBy());
                    pm.executeUpdate();
                }
                c.commit();
                return g.getId();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new SplitMateException("Could not create group: " + e.getMessage(), e);
        }
    }

    public void addMember(int groupId, int userId) {
        if (isMember(groupId, userId)) throw new SplitMateException("User is already in this group.");
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO group_members (group_id, user_id) VALUES (?,?)")) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SplitMateException("Could not add member: " + e.getMessage(), e);
        }
    }

    public boolean isMember(int groupId, int userId) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT 1 FROM group_members WHERE group_id = ? AND user_id = ?")) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            throw new SplitMateException("Database error: " + e.getMessage(), e);
        }
    }

    public List<ExpenseGroup> listForUser(int userId) {
        String sql = "SELECT g.* FROM expense_groups g JOIN group_members m ON g.group_id = m.group_id "
                   + "WHERE m.user_id = ? ORDER BY g.group_id";
        List<ExpenseGroup> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new ExpenseGroup(rs.getInt("group_id"), rs.getString("group_name"), rs.getInt("created_by")));
            }
        } catch (SQLException e) {
            throw new SplitMateException("Database error: " + e.getMessage(), e);
        }
        return list;
    }

    public ExpenseGroup findById(int groupId) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM expense_groups WHERE group_id = ?")) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new ExpenseGroup(rs.getInt("group_id"), rs.getString("group_name"), rs.getInt("created_by"));
            }
        } catch (SQLException e) {
            throw new SplitMateException("Database error: " + e.getMessage(), e);
        }
    }

    public List<User> getMembers(int groupId) {
        String sql = "SELECT u.* FROM users u JOIN group_members m ON u.user_id = m.user_id "
                   + "WHERE m.group_id = ? ORDER BY u.user_id";
        List<User> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(new User(rs.getInt("user_id"), rs.getString("name"),
                                      rs.getString("email"), rs.getString("password_hash")));
            }
        } catch (SQLException e) {
            throw new SplitMateException("Database error: " + e.getMessage(), e);
        }
        return list;
    }

    public void rename(int groupId, String newName) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE expense_groups SET group_name = ? WHERE group_id = ?")) {
            ps.setString(1, newName);
            ps.setInt(2, groupId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SplitMateException("Could not rename group: " + e.getMessage(), e);
        }
    }

    /** Cascades to members, expenses, splits and settlements. */
    public void delete(int groupId) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM expense_groups WHERE group_id = ?")) {
            ps.setInt(1, groupId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SplitMateException("Could not delete group: " + e.getMessage(), e);
        }
    }
}
