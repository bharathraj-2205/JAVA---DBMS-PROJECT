# SplitMate – Shared Expense Management System (CLI)

Java (OOP) + JDBC + MySQL. Console application.

## 1. Setup the database (MySQL Workbench)
1. Open MySQL Workbench and connect to your local server.
2. File > Open SQL Script > `sql/schema.sql`, then click the lightning bolt to run it.
3. Confirm the `splitmate` schema has 6 tables.

## 2. Get the JDBC driver
Download **MySQL Connector/J** (Platform Independent, ZIP) from https://dev.mysql.com/downloads/connector/j/
and copy `mysql-connector-j-x.x.x.jar` into the `lib/` folder.

## 3. Set DB credentials (optional)
Defaults are user `root`, password `root`, database `splitmate` on localhost:3306.
Override with environment variables:
- `SPLITMATE_DB_USER`, `SPLITMATE_DB_PASSWORD`, `SPLITMATE_DB_URL`

PowerShell: `$env:SPLITMATE_DB_PASSWORD="yourpassword"`

## 4. Compile and run (from the SplitMate folder)
Windows:
    javac -cp "lib/*" -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
    java -cp "out;lib/*" com.splitmate.Main

Linux / macOS:
    javac -cp "lib/*" -d out $(find src -name "*.java")
    java -cp "out:lib/*" com.splitmate.Main

## OOP concepts used
| Concept | Where |
|---|---|
| Encapsulation | private fields + getters/setters in `model/*` |
| Inheritance | `User`, `ExpenseGroup`, `Expense`, `Settlement` extend `BaseEntity`; `EqualSplit`, `ExactSplit`, `PercentageSplit` extend `AbstractSplit` |
| Abstraction | `SplitStrategy` interface, `AbstractSplit` and `BaseEntity` abstract classes |
| Polymorphism | `SplitFactory.forType()` returns a `SplitStrategy`; `calculate()` behaves differently per type; `describe()` overridden per entity |

## DBMS concepts used
Primary/foreign keys, composite key (`group_members`), UNIQUE, CHECK, ENUM, ON DELETE CASCADE,
JOINs, GROUP BY with SUM, transactions (commit/rollback) for group creation and expense + splits,
PreparedStatements (prevents SQL injection).

## Typical demo flow
Register 3 users > login as first > create group > add the others by email > add an expense (equal) >
add one (exact) and one (percentage) > Dashboard to see balances and suggested payments > record a
settlement > mark it PAID > check Dashboard again.
