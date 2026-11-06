# Backend Gestion Hôtel

## Connexion locale

Le backend utilise Java 17 et Spring Boot 3.5.16. La connexion Oracle est fournie exclusivement par les variables `ORACLE_JDBC_URL`, `ORACLE_USERNAME`, `ORACLE_PASSWORD` et `ORACLE_SCHEMA`. Pour Docker local, l'URL attendue est de la forme `jdbc:oracle:thin:@//127.0.0.1:1521/FREEPDB1`.

Le fichier local racine `.env.oracle.local` contient les valeurs de développement et reste ignoré par Git. Le backend valide le schéma avec Hibernate (`ddl-auto=validate`) mais ne crée aucune table et n'exécute aucune migration au démarrage.

## Premier administrateur local

Sur un schéma sans administrateur actif, définir ensemble `APP_BOOTSTRAP_ADMIN_LOGIN`, `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_PASSWORD`, `APP_BOOTSTRAP_ADMIN_FIRST_NAME` et `APP_BOOTSTRAP_ADMIN_LAST_NAME` avant un démarrage. Le mot de passe est immédiatement haché avec BCrypt et n'est jamais enregistré en clair. Retirer ces variables après le premier démarrage ; si un administrateur actif existe déjà, le bootstrap ne crée rien.

La session utilise un cookie `JSESSIONID` HttpOnly et SameSite=Lax. Conserver `SESSION_COOKIE_SECURE=false` uniquement en développement HTTP local et utiliser `true` derrière HTTPS.

## Commandes

Après avoir chargé les variables d'environnement dans le terminal :

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

L'API écoute localement sur `http://localhost:8080`. L'origine CORS autorisée par défaut est `http://localhost:5173` et peut être remplacée avec `FRONTEND_ORIGIN`.
