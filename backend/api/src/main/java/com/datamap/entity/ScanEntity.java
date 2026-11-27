package com.datamap.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Scan audit metadata entity. Stores file metrics only; never stores raw cell values.
 */
@Entity
@Table(name = "scans")
public class ScanEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 10)
    private String fileType;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "column_count", nullable = false)
    private int columnCount;

    @Column(name = "flagged_columns", nullable = false)
    private int flaggedColumns;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ColumnFindingEntity> findings = new ArrayList<>();

    public ScanEntity() {
    }

    public ScanEntity(
        UUID id,
        String fileName,
        String fileType,
        int rowCount,
        int columnCount,
        int flaggedColumns,
        LocalDateTime createdAt,
        int durationMs
    ) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.flaggedColumns = flaggedColumns;
        this.createdAt = createdAt;
        this.durationMs = durationMs;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public int getFlaggedColumns() {
        return flaggedColumns;
    }

    public void setFlaggedColumns(int flaggedColumns) {
        this.flaggedColumns = flaggedColumns;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    public List<ColumnFindingEntity> getFindings() {
        return findings;
    }

    public void setFindings(List<ColumnFindingEntity> findings) {
        this.findings = findings;
    }

    public void addFinding(ColumnFindingEntity finding) {
        findings.add(finding);
        finding.setScan(this);
    }
}
