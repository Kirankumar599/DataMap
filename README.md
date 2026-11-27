# DataMap — Sensitive Data Detection & Masking Backend

DataMap is a privacy-focused sensitive-data detection and masking platform designed to identify and mask Indian sensitive identifiers in uploaded CSV and Excel (XLSX) files:
- **Aadhaar numbers** (12-digit with Verhoeff dihedral group D5 checksum validation)
- **PAN numbers** (10-character with 4th character PAN holder type verification)
- **Indian Mobile numbers** (10-digit starting with 6-9, supporting `+91`, `91`, and `0` prefixes)
- **Email addresses** (practical RFC-compliant email detection)

---

## 1. Architecture Overview

The backend is built as a strict two-module Maven architecture:

```
backend/
├── pom.xml                   # Root Parent POM (Java 21, Spring Boot 4.1.3 BOM, JUnit 5)
├── mvnw.cmd                  # Maven wrapper executable
│
├── engine/                   # Module 1: PURE Java Detection & Masking Engine
│   ├── pom.xml               # Pure Java, JUnit 5 only (Zero Spring / Web / DB dependencies)
│   └── src/
│       ├── main/java/com/datamap/engine/
│       │   ├── ColumnScanner.java          # Public scanner contract
│       │   ├── DefaultColumnScanner.java   # Core scanner orchestrator
│       │   ├── checksum/VerhoeffChecksum.java # Verhoeff D5 checksum algorithm
│       │   ├── detector/                   # Aadhaar, PAN, Phone, Email detectors
│       │   ├── enums/                      # IdentifierType, Confidence
│       │   ├── header/HeaderMatcher.java   # Token & Levenshtein fuzzy matcher
│       │   ├── masker/Masker.java          # Pure Java masking functions
│       │   ├── model/                      # ColumnFinding, DetectionResult, HeaderMatchResult
│       │   └── scoring/                    # Sampler (systematic spread), ConfidenceScorer
│       └── test/java/com/datamap/engine/   # 35 Unit tests
│
└── api/                      # Module 2: Spring Boot REST API & Metadata Persistence
    ├── pom.xml               # Depends on datamap-engine, Spring Boot, Commons CSV, POI, JPA
    └── src/
        ├── main/java/com/datamap/
        │   ├── DatamapApplication.java     # Spring Boot application entry point
        │   ├── config/WebConfig.java       # CORS configuration & Engine Bean injection
        │   ├── controller/                 # ScanController, HealthController
        │   ├── dto/                        # ScanResponseDto, ScanSummaryDto, ColumnFindingDto, etc.
        │   ├── entity/                     # ScanEntity, ColumnFindingEntity (metadata only)
        │   ├── enums/ErrorCode.java        # Error codes
        │   ├── exception/                  # DataMapException, GlobalExceptionHandler
        │   ├── parser/                     # CsvParser, XlsxParser, ParserFactory
        │   ├── repository/                 # ScanRepository, ColumnFindingRepository
        │   └── service/ScanService.java    # Workflow orchestration & privacy boundary
        └── test/java/com/datamap/          # 17 Integration and controller tests
```

### Dependency Rule
```
API ---> ENGINE
```
The Engine is completely decoupled from Spring Boot, HTTP, Repositories, and Databases.

---

## 2. Privacy & Security Safeguards

- **Metadata Only Persistence**: The database strictly records file metrics (`id`, `file_name`, `file_type`, `row_count`, `column_count`, `flagged_columns`, `duration_ms`, `match_rate`).
- **Never Store Personal Data**: Raw cell values, uploaded files, complete parsed rows, and unmasked identifiers are **NEVER** stored in the database or written to temporary server files.
- **Masked Previews Only**: Scan results return masked samples (e.g. `XXXX-XXXX-0124`, `XXXXXX234F`, `XXXXXX3210`, `r***@gmail.com`).
- **No Request-Body Logging**: SQL query parameter logging and HTTP body logging are disabled.
- **Upload Limit**: Restricted to a maximum of 10 MB per upload.

---

## 3. Technology Stack

- **Java**: 21
- **Spring Boot**: 4.1.3
- **File Parsers**: Apache Commons CSV 1.12.0, Apache POI 5.3.0 (OOXML)
- **Database**: PostgreSQL (Production), H2 in-memory (Testing)
- **Testing**: JUnit 5, AssertJ, Mockito, Spring MockMvc

---

## 4. API Endpoints

### 4.1. Scan Upload: `POST /api/scans`
- **Request**: `multipart/form-data`, field: `file` (.csv, .xlsx, max 10MB)
- **Response**:
```json
{
  "scanId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "fileName": "students.xlsx",
  "rowCount": 1200,
  "columnCount": 8,
  "summary": {
    "flaggedColumns": 3,
    "high": 2,
    "medium": 1,
    "low": 0
  },
  "columns": [
    {
      "index": 2,
      "name": "Aadhaar No",
      "type": "AADHAAR",
      "confidence": "HIGH",
      "reasons": [
        "Header matches 'aadhaar'",
        "96% of sampled values match the 12-digit format",
        "94% of Aadhaar-like values pass the Verhoeff checksum"
      ],
      "maskedSamples": [
        "XXXX-XXXX-1234",
        "XXXX-XXXX-5678"
      ]
    }
  ],
  "disclaimer": "Results are candidate matches based on format rules, not proof of identity."
}
```

### 4.2. Mask & Download: `POST /api/mask`
- **Request**: `multipart/form-data`
  - `file`: original file
  - `columns`: JSON array or comma-separated column indexes (e.g. `[1, 2]` or `1,2`)
- **Response**: Binary file download with `Content-Disposition: attachment; filename="masked_<originalName>"`

### 4.3. Scan History: `GET /api/scans`
- **Response**: Array of metadata-only scan records (ID, filename, counts, timestamp, duration).

### 4.4. Health Check: `GET /api/health`
- **Response**:
```json
{
  "status": "ok"
}
```

---

## 5. Environment Variables & Database Setup

The application uses PostgreSQL with sensible defaults, configurable via environment variables:

| Variable | Default Value | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/datamap` | PostgreSQL JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Database password |
| `SERVER_PORT` | `8080` | Application HTTP port |

---

## 6. How to Build & Run

### Run Tests
```powershell
cd c:\Data-Map\backend
.\mvnw.cmd clean test
```
*(All 52 tests across both modules run and validate the complete pipeline).*

### Run the Application
```powershell
cd c:\Data-Map\backend
.\mvnw.cmd spring-boot:run -pl api
```

The server starts on `http://localhost:8080` with CORS enabled for `http://localhost:5173`.
