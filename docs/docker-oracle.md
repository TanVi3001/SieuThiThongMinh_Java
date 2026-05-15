# Oracle Database local development with Docker

This project is a Java Swing desktop application, so only the Oracle database is containerized for local development.

## Start Oracle

```bash
docker compose up -d
docker logs -f supermarket-oracle
```

Wait until the logs show that the database is ready before launching the Java application.

## Stop Oracle

```bash
docker compose down
```

## Reset the local database

Initialization scripts run only on the first startup of a fresh database volume. To rebuild the database from the SQL files under `database/`, remove the volume and start again:

```bash
docker compose down -v
docker compose up -d
```

## Connection details

Use these values in SQL Developer or another IDE:

| Field | Value |
| --- | --- |
| Host | `localhost` |
| Port | `1521` |
| Service name | `FREEPDB1` |
| Username | `appuser` |
| Password | `apppass` |
| JDBC URL | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |

The Java application reads the same defaults from `src/main/resources/database.properties`. You can override them without editing source files by setting environment variables before launching the app:

```bash
DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
DB_USERNAME=appuser
DB_PASSWORD=apppass
```
