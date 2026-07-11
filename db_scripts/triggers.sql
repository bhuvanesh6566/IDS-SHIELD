USE ids_system;

DELIMITER //

-- Trigger for UPDATE on users table
CREATE TRIGGER after_user_update
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
    -- Detect if balance or name changed
    IF OLD.balance <> NEW.balance THEN
        INSERT INTO audit_log (user_id, action_type, old_value, new_value, field_affected, status)
        VALUES (OLD.id, 'UPDATE', OLD.balance, NEW.balance, 'balance', 'PENDING');
    END IF;

    IF OLD.name <> NEW.name THEN
        INSERT INTO audit_log (user_id, action_type, old_value, new_value, field_affected, status)
        VALUES (OLD.id, 'UPDATE', OLD.name, NEW.name, 'name', 'PENDING');
    END IF;
END //

-- Trigger for INSERT on users table
CREATE TRIGGER after_user_insert
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (user_id, action_type, old_value, new_value, field_affected, status)
    VALUES (NEW.id, 'INSERT', NULL, NEW.name, 'all', 'APPROVED');
END //

DELIMITER ;
