CREATE DATABASE IF NOT EXISTS splitmate;
USE splitmate;

CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS expense_groups (
    group_id    INT AUTO_INCREMENT PRIMARY KEY,
    group_name  VARCHAR(100) NOT NULL,
    created_by  INT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS group_members (
    group_id  INT NOT NULL,
    user_id   INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES expense_groups(group_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)  REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS expenses (
    expense_id   INT AUTO_INCREMENT PRIMARY KEY,
    group_id     INT NOT NULL,
    paid_by      INT NOT NULL,
    description  VARCHAR(200) NOT NULL,
    amount       DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    expense_date DATE NOT NULL,
    split_type   ENUM('EQUAL','EXACT','PERCENTAGE') NOT NULL,
    FOREIGN KEY (group_id) REFERENCES expense_groups(group_id) ON DELETE CASCADE,
    FOREIGN KEY (paid_by)  REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS expense_splits (
    split_id     INT AUTO_INCREMENT PRIMARY KEY,
    expense_id   INT NOT NULL,
    user_id      INT NOT NULL,
    share_amount DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (expense_id) REFERENCES expenses(expense_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS settlements (
    settlement_id INT AUTO_INCREMENT PRIMARY KEY,
    group_id      INT NOT NULL,
    from_user     INT NOT NULL,
    to_user       INT NOT NULL,
    amount        DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    status        ENUM('PENDING','PAID','CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id)  REFERENCES expense_groups(group_id) ON DELETE CASCADE,
    FOREIGN KEY (from_user) REFERENCES users(user_id),
    FOREIGN KEY (to_user)   REFERENCES users(user_id)
);
