package vn.edu.et4430.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Column(name = "course_code", nullable = false, columnDefinition = "text", unique = true)
    private String courseCode;

    @Column(name = "course_name", nullable = false, columnDefinition = "text")
    private String courseName;

    @Column(name = "credits", nullable = false, precision = 6, scale = 2)
    private BigDecimal credits;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Course() {
    }

    public Long getId() {
        return id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public BigDecimal getCredits() {
        return credits;
    }

    public void setCredits(BigDecimal credits) {
        this.credits = credits;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

