package com.splitmate.model;

public class User extends BaseEntity {
    private String name;
    private String email;
    private String passwordHash;

    public User(String name, String email, String passwordHash) {
        this.name = name; this.email = email; this.passwordHash = passwordHash;
    }
    public User(int id, String name, String email, String passwordHash) {
        super(id);
        this.name = name; this.email = email; this.passwordHash = passwordHash;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setName(String name) { this.name = name; }

    @Override
    public String describe() { return String.format("[%d] %s <%s>", getId(), name, email); }
}
