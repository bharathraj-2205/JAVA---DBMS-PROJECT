package com.splitmate.model;

import java.math.BigDecimal;

public class Settlement extends BaseEntity {
    public enum Status { PENDING, PAID, CANCELLED }

    private int groupId;
    private int fromUser;
    private int toUser;
    private BigDecimal amount;
    private Status status;

    public Settlement(int groupId, int fromUser, int toUser, BigDecimal amount) {
        this.groupId = groupId; this.fromUser = fromUser; this.toUser = toUser;
        this.amount = amount; this.status = Status.PENDING;
    }
    public Settlement(int id, int groupId, int fromUser, int toUser, BigDecimal amount, Status status) {
        super(id);
        this.groupId = groupId; this.fromUser = fromUser; this.toUser = toUser;
        this.amount = amount; this.status = status;
    }

    public int getGroupId() { return groupId; }
    public int getFromUser() { return fromUser; }
    public int getToUser() { return toUser; }
    public BigDecimal getAmount() { return amount; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    @Override
    public String describe() {
        return String.format("[%d] user %d -> user %d : Rs.%s (%s)", getId(), fromUser, toUser, amount, status);
    }
}
