CREATE DATABASE inventory_db;
CREATE USER 'username'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'username'@'localhost';
FLUSH PRIVILEGES;
