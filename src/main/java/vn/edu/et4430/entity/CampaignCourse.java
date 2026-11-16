package vn.edu.et4430.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "campaign_course", uniqueConstraints = @UniqueConstraint(name = "uq_campaign_course", columnNames = {"campaign_id", "course_id"}))
public class CampaignCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_course_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private RegistrationCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "copied_from_campaign_course_id", nullable = true)
    private CampaignCourse copiedFromCampaignCourse;

    @Column(name = "offering_status", nullable = false, columnDefinition = "text")
    private String offeringStatus;

    public CampaignCourse() {
    }

    public Long getId() {
        return id;
    }

    public RegistrationCampaign getCampaign() {
        return campaign;
    }

    public void setCampaign(RegistrationCampaign campaign) {
        this.campaign = campaign;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public CampaignCourse getCopiedFromCampaignCourse() {
        return copiedFromCampaignCourse;
    }

    public void setCopiedFromCampaignCourse(CampaignCourse copiedFromCampaignCourse) {
        this.copiedFromCampaignCourse = copiedFromCampaignCourse;
    }

    public String getOfferingStatus() {
        return offeringStatus;
    }

    public void setOfferingStatus(String offeringStatus) {
        this.offeringStatus = offeringStatus;
    }
}

