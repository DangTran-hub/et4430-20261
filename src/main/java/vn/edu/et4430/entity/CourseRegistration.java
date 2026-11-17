package vn.edu.et4430.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "course_registration", uniqueConstraints = @UniqueConstraint(name = "uq_course_registration", columnNames = {"student_id", "campaign_course_id"}))
public class CourseRegistration {

    public static final String REGISTERED = "REGISTERED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_course_id", nullable = false)
    private CampaignCourse campaignCourse;

    @Column(name = "status", nullable = false, columnDefinition = "text")
    private String status;

    @Column(name = "registered_at", nullable = false, columnDefinition = "timestamp with time zone", insertable = false, updatable = false)
    @Generated(event = EventType.INSERT)
    private OffsetDateTime registeredAt;

    public CourseRegistration() {
    }

    public Long getId() {
        return id;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public void setStudent(StudentProfile student) {
        this.student = student;
    }

    public CampaignCourse getCampaignCourse() {
        return campaignCourse;
    }

    public void setCampaignCourse(CampaignCourse campaignCourse) {
        this.campaignCourse = campaignCourse;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getRegisteredAt() {
        return registeredAt;
    }
}
