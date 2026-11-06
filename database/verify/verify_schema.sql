-- Read-only verification. A successful installation must return no rows.

SELECT object_name, object_type, status
  FROM user_objects
 WHERE status <> 'VALID'
 ORDER BY object_type, object_name;

SELECT name, type, line, position, text
  FROM user_errors
 ORDER BY name, sequence;

