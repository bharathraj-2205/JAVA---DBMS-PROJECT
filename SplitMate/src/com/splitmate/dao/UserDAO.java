package com.splitmate.dao;

import com.splitmate.db.DBConnection;
import com.splitmate.exception.SplitMateException;
import com.splitmate.model.User;
import java.sql.*;

public class UserDAO {

    public int register(User u) {
        String sql = "INSERT INTO users (name, email, password_hash) VALUES (?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                u.setId(rs.getInt(1));
                return u.getId();
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new SplitMateException("That email is already registered.");
        } catch (SQLException e) {
            throw new SplitMateException("Could not register user: " + e.getMessage(), e);
        }
    }

    public User findByEmail(String email) {
        return findOne("SELECT * FROM users WHERE email = ?", email);
    }

    public User findById(int id) {
        return findOne("SELECT * FROM users WHERE user_id = ?", id);
    }

    private User findOne(String sql, Object param) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new User(rs.getInt("user_id"), rs.getString("name"),
                                rs.getString("email"), rs.getString("password_hash"));
            }
        } catch (SQLException e) {
            throw new SplitMateException("Database error: " + e.getMessage(), e);
        }
    }

    public void updateName(int userId, String newName) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET name = ? WHERE user_id = ?")) {
            ps.setString(1, newName);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SplitMateException("Could not update profile: " + e.getMessage(), e);
        }
    }
}
