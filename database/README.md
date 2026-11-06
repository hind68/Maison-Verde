# Installation Oracle du socle hôtelier

Ces scripts sont destinés exclusivement à un schéma Oracle de développement neuf et vide. Ils ne contiennent aucun `DROP`, `DELETE` ou `TRUNCATE`. Les scripts académiques situés dans `SQL_PLSQL`, `SGBRO` et `MULTIMEDIA` restent des références et ne doivent pas être utilisés comme migrations applicatives.

## Ordre d'installation

1. Créer un schéma propriétaire dédié, par exemple `HOTEL_APP_DEV`, avec les droits de création de tables, séquences, triggers et procédures.
2. Exécuter dans l'ordre les migrations `V1` à `V6`. `V4` conserve les horaires réels du séjour, `V5` sépare les états opérationnel et de propreté, et `V6` ajoute les comptes du personnel sans créer d'identifiants.
3. Charger éventuellement `demo/demo_data.sql` dans ce schéma neuf.
4. Exécuter `verify/verify_schema.sql`. Les deux requêtes doivent retourner zéro ligne.

La procédure `PKG_RESERVATION_APP.CREER_RESERVATION_CONFIRMEE` verrouille la chambre, revalide le chevauchement, crée les occupants et confirme la réservation. Elle ne contient ni `COMMIT` ni `ROLLBACK`; le futur service Spring Boot pilotera l'unique transaction avec `@Transactional`.

Pour limiter les écritures directes, utiliser idéalement un schéma propriétaire pour les objets et un utilisateur d'exécution distinct recevant uniquement les droits nécessaires, notamment `EXECUTE` sur le package. Les paramètres Spring Boot proviendront de `ORACLE_JDBC_URL`, `ORACLE_USERNAME`, `ORACLE_PASSWORD` et `ORACLE_SCHEMA`. Seul `.env.example` est versionné; les valeurs réelles restent locales.

Le conteneur local se lance avec `docker compose --env-file .env.oracle.local up -d`. La connexion JDBC locale utilise le service `FREEPDB1` et le port défini par `ORACLE_PORT`. Le fichier `.env.oracle.local` est ignoré par Git et ne doit jamais être ajouté au dépôt.
