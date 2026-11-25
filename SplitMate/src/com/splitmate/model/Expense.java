package com.splitmate.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Expense extends BaseEntity {
    private int groupId;
    private int paidBy;
    private String description;
    private BigDecimal amount;
    private LocalDate date;
    private String splitType;

    public Expense(int groupId, int paidBy, String description, BigDecimal amount, LocalDate date, String splitType) {
        this.groupId = groupId; this.paidBy = paidBy; this.description = description;
        this.amount = amount; this.date = date; this.splitType = splitType;
    }
    public Expense(int id, int groupId, int paidBy, String description, BigDecimal amount, LocalDate date, String splitType) {
        this(groupId, paidBy, description, amount, date, splitType);
        setId(id);
    }

    public int getGroupId() { return groupId; }
    public int getPaidBy() { return paidBy; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getSplitType() { return splitType; }
    public void setDescription(String description) { this.description = description; }
    public void setDate(LocalDate date) { this.date = date; }

    @Override
    public String describe() {
        return String.format("[%d] %s | Rs.%s | %s | %s", getId(), description, amount, date, splitType);
    }
}
