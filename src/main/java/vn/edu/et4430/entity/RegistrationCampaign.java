package vn.edu.et4430.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "registration_campaign")
public class RegistrationCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private AcademicTerm term;

    @Column(name = "registration_open_at", nullable = true, columnDefinition = "timestamp with time zone")
    private OffsetDateTime registrationOpenAt;

    @Column(name = "registration_close_at", nullable = true, columnDefinition = "timestamp with time zone")
    private OffsetDateTime registrationCloseAt;

    @Column(name = "status", nullable = false, columnDefinition = "text")
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "copied_from_campaign_id", nullable = true)
    private RegistrationCampaign copiedFromCampaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "approved_by", nullable = true)
    private UserAccount approvedBy;

    @Column(name = "approved_at", nullable = true, columnDefinition = "timestamp with time zone")
    private OffsetDateTime approvedAt;

    @Column(name = "published_at", nullable = true, columnDefinition = "timestamp with time zone")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp with time zone", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    private OffsetDateTime updatedAt;

    public RegistrationCampaign() {
    }

    public Long getId() {
        return id;
    }

    public AcademicTerm getTerm() {
        return term;
    }

    public void setTerm(AcademicTerm term) {
        this.term = term;
    }

    public OffsetDateTime getRegistrationOpenAt() {
        return registrationOpenAt;
    }

    public void setRegistrationOpenAt(OffsetDateTime registrationOpenAt) {
        this.registrationOpenAt = registrationOpenAt;
    }

    public OffsetDateTime getRegistrationCloseAt() {
        return registrationCloseAt;
    }

    public void setRegistrationCloseAt(OffsetDateTime registrationCloseAt) {
        this.registrationCloseAt = registrationCloseAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RegistrationCampaign getCopiedFromCampaign() {
        return copiedFromCampaign;
    }

    public void setCopiedFromCampaign(RegistrationCampaign copiedFromCampaign) {
        this.copiedFromCampaign = copiedFromCampaign;
    }

    public UserAccount getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserAccount createdBy) {
        this.createdBy = createdBy;
    }

    public UserAccount getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UserAccount approvedBy) {
        this.approvedBy = approvedBy;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void initializeAudit() {
        OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateAudit() {
        updatedAt = OffsetDateTime.now(java.time.ZoneOffset.UTC);
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

