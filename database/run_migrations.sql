SET ECHO ON
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

@/opt/project/database/migrations/V1__create_relational_schema.sql
@/opt/project/database/migrations/V2__business_rules_indexes_and_api.sql
@/opt/project/database/migrations/V3__reference_data.sql
@/opt/project/database/migrations/V4__front_office_actual_times.sql
@/opt/project/database/migrations/V5__room_housekeeping_and_operations.sql
@/opt/project/database/migrations/V6__staff_authentication_and_roles.sql
@/opt/project/database/demo/demo_data.sql

COMMIT;
EXIT SUCCESS
