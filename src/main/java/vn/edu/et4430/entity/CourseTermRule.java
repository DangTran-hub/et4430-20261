package vn.edu.et4430.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "course_term_rule")
public class CourseTermRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "term_code", nullable = false, columnDefinition = "text")
    private String termCode;

    @Column(name = "program_code", nullable = true, columnDefinition = "text")
    private String programCode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public CourseTermRule() {
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getTermCode() {
        return termCode;
    }

    public void setTermCode(String termCode) {
        this.termCode = termCode;
    }

    public String getProgramCode() {
        return programCode;
    }

    public void setProgramCode(String programCode) {
        this.programCode = programCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

