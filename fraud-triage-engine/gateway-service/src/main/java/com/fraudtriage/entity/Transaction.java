package com.fraudtriage.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions", uniqueConstraints = @UniqueConstraint(name = "uk_external_ref", columnNames = "external_ref"))
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="external_ref", nullable=false, length=64)
    private String externalRef;
    @Column(name="account_id", nullable=false, length=64)
    private String accountId;
    @Column(nullable=false, precision=15, scale=2)
    private BigDecimal amount;
    @Column(nullable=false, length=8)
    private String currency;
    @Column(length=128) private String merchant;
    @Column(length=64) private String country;
    @Column(name="ip_address", length=64) private String ipAddress;
    @Column(name="recent_transaction_count_10m", nullable=false)
    private Integer recentTransactionCount10m = 0;
    @Column(name="home_country", length=64)
    private String homeCountry;
    @Column(name="submitted_at", nullable=false)
    private Instant submittedAt = Instant.now();
    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private TransactionStatus status = TransactionStatus.PENDING;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getExternalRef(){return externalRef;} public void setExternalRef(String v){externalRef=v;}
    public String getAccountId(){return accountId;} public void setAccountId(String v){accountId=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
    public String getMerchant(){return merchant;} public void setMerchant(String v){merchant=v;}
    public String getCountry(){return country;} public void setCountry(String v){country=v;}
    public String getIpAddress(){return ipAddress;} public void setIpAddress(String v){ipAddress=v;}
    public Integer getRecentTransactionCount10m(){return recentTransactionCount10m;} public void setRecentTransactionCount10m(Integer v){recentTransactionCount10m=v;}
    public String getHomeCountry(){return homeCountry;} public void setHomeCountry(String v){homeCountry=v;}
    public Instant getSubmittedAt(){return submittedAt;} public void setSubmittedAt(Instant v){submittedAt=v;}
    public TransactionStatus getStatus(){return status;} public void setStatus(TransactionStatus v){status=v;}
}
