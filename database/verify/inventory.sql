SET PAGESIZE 200
SET LINESIZE 180
SET FEEDBACK ON

SELECT object_type, status, COUNT(*) AS total
  FROM user_objects
 WHERE object_type IN ('TABLE', 'SEQUENCE', 'INDEX', 'TRIGGER', 'PACKAGE', 'PACKAGE BODY')
 GROUP BY object_type, status
 ORDER BY object_type, status;

SELECT constraint_type, status, COUNT(*) AS total
  FROM user_constraints
 GROUP BY constraint_type, status
 ORDER BY constraint_type;

SELECT trigger_name, status
  FROM user_triggers
 ORDER BY trigger_name;

SELECT COUNT(*) AS invalid_objects
  FROM user_objects
 WHERE status <> 'VALID';

SELECT COUNT(*) AS compilation_errors
  FROM user_errors;

EXIT SUCCESS
