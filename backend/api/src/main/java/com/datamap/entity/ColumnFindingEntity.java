package com.datamap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Metadata record for a single detected column. Stores indexes and confidence only; never stores raw cell values.
 */
@Entity
@Table(name = "column_findings")
public class ColumnFindingEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", nullable = false)
    private ScanEntity scan;

    @Column(name = "column_index", nullable = false)
    private int columnIndex;

    @Column(name = "identifier_type", nullable = false, length = 20)
    private String identifierType;

    @Column(name = "confidence", nullable = false, length = 10)
    private String confidence;

    @Column(name = "match_rate", precision = 5, scale = 2)
    private BigDecimal matchRate;

    public ColumnFindingEntity() {
    }

    public ColumnFindingEntity(
        UUID id,
        ScanEntity scan,
        int columnIndex,
        String identifierType,
        String confidence,
        BigDecimal matchRate
    ) {
        this.id = id;
        this.scan = scan;
        this.columnIndex = columnIndex;
        this.identifierType = identifierType;
        this.confidence = confidence;
        this.matchRate = matchRate;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ScanEntity getScan() {
        return scan;
    }

    public void setScan(ScanEntity scan) {
        this.scan = scan;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public BigDecimal getMatchRate() {
        return matchRate;
    }

    public void setMatchRate(BigDecimal matchRate) {
        this.matchRate = matchRate;
    }
}
