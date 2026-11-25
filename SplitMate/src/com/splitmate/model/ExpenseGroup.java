package com.splitmate.model;

public class ExpenseGroup extends BaseEntity {
    private String name;
    private int createdBy;

    public ExpenseGroup(String name, int createdBy) { this.name = name; this.createdBy = createdBy; }
    public ExpenseGroup(int id, String name, int createdBy) {
        super(id); this.name = name; this.createdBy = createdBy;
    }

    public String getName() { return name; }
    public int getCreatedBy() { return createdBy; }
    public void setName(String name) { this.name = name; }

    @Override
    public String describe() { return String.format("[%d] %s", getId(), name); }
}
