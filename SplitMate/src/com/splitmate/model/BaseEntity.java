package com.splitmate.model;

/** Abstract parent: every entity has a database id. (Inheritance + Abstraction) */
public abstract class BaseEntity {
    private int id;

    protected BaseEntity() { }
    protected BaseEntity(int id) { this.id = id; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    /** Each entity describes itself differently (Polymorphism). */
    public abstract String describe();

    @Override
    public String toString() { return describe(); }
}
