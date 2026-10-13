package com.neueda.positionservice.models;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.neueda.positionservice.enums.PortfolioRisk;

import jakarta.persistence.*;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "portfolios")
public class Portfolio {

    public static final String DEFAULT_NAME = "Main";

    @Id
    @Column(name = "portfolio_id")
    private String portfolioId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private String accountId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk", nullable = false)
    private PortfolioRisk risk;

    @Column(name = "benchmark")
    private String benchmark;

    @Column(name = "is_default", nullable = false, updatable = false)
    private boolean defaultPortfolio;

    @Version
    @Column(name = "version")
    private Integer version;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    protected Portfolio() {
    }

    public Portfolio(String portfolioId, String accountId, String name, PortfolioRisk risk, String benchmark) {
        this.portfolioId = portfolioId;
        this.accountId = accountId;
        this.name = name;
        this.risk = risk;
        this.benchmark = benchmark;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastUpdated = now;
    }

    @PreUpdate
    void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PortfolioRisk getRisk() {
        return risk;
    }

    public void setRisk(PortfolioRisk risk) {
        this.risk = risk;
    }

    public String getBenchmark() {
        return benchmark;
    }

    public void setBenchmark(String benchmark) {
        this.benchmark = benchmark;
    }

    public boolean isDefaultPortfolio() {
        return defaultPortfolio;
    }

    public Integer getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Portfolio other)) return false;
        return Objects.equals(portfolioId, other.portfolioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portfolioId);
    }

    @Override
    public String toString() {
        return "Portfolio{portfolioId='" + portfolioId + "', accountId='" + accountId + "', name='" + name
                + "', default=" + defaultPortfolio + '}';
    }
}
