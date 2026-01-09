# CRUUD

## Panoramica
CRUUD (Cruise-UD Transformer) converte file CSV/JSON non strutturati in file Urban Dataset (UD) secondo mapping
e configurazioni salvate a database. Gestisce conversioni manuali e automatiche tramite RabbitMQ.

## Funzionalita principali
- Conversione CSV/JSON in UrbanDataset.
- Conversione automatica e cleaning schedulato (RabbitMQ).
- Normalizzazione temporale con gestione DST e offset UD.
- Chunking dei file UD (max righe configurabile).

## Architettura
- Spring Boot 3.x
- MongoDB (configurazioni di property ed extraction)
- RabbitMQ (auto-convert / auto-clean)
- Docker Compose per ambiente locale

## Requisiti
- Docker Desktop (Windows) o Docker Engine (Linux/macOS)
- Java 21 (solo per build locale senza Docker)
- Maven (solo per build locale senza Docker)

## Configurazione Docker Compose
File `.env` in `docker/compose`:
- `DB_*` / `RABBITMQ_*`: credenziali e host interni ai container.
- `HOST_DOCUMENTS_DIR`: directory base sul host (impostata dagli script).
- `MONGO_DATA_SUBDIR`, `APP_DATA_SUBDIR`: sottocartelle su host.
- `MONGO_DATA_PATH`, `APP_DATA_PATH` (opzionali): override path completi.

I volumi nel compose montano:
- MongoDB -> `/data/db`
- App -> `/data/app`

## Avvio con Docker (consigliato)

### 1) Build immagine
Windows:
```powershell
cd docker
.\build-image.bat
```

Linux/macOS:
```bash
cd docker
./build-image.sh
```

### 2) Avvio stack
Windows PowerShell:
```powershell
cd docker\compose
.\compose-up.ps1
```

Linux/macOS:
```bash
cd docker/compose
bash ./compose-up.sh
```

Gli script calcolano automaticamente la cartella Documenti e impostano `HOST_DOCUMENTS_DIR`.

### Stop e shutdown
Windows PowerShell:
```powershell
.\compose-down.ps1
```

Linux/macOS:
```bash
bash ./compose-down.sh
```

## Endpoint principali
Base URL: `http://localhost:8090`

- `POST /transformer/csv` -> conversione da cartella (payload ExtractionDto)
- `GET /transformer/csv/{extractionName}` -> conversione da extractionName
- `POST /transformer/upload/{property}` -> upload CSV
- `POST /transformer/external/open-cruise/{extractionName}` -> JSON OpenCruise
- `POST /property` / `GET /property/all` / `GET /property/{id}` / `POST /property/filter`
- `POST /extraction` / `GET /extraction/all` / `GET /extraction/{id}` / `GET /extraction/name/{extractionName}`

## Note
- La build Docker usa `mvn -Pprod`, quindi il profilo attivo e `prod`.
- Se vuoi usare un path diverso sul host, imposta `HOST_DOCUMENTS_DIR` o `APP_DATA_PATH` nel `.env`.
