package vn.edu.et4430.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_profile")
public class StudentProfile {

    @Id
    @Column(name = "student_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private UserAccount user;

    @Column(name = "student_code", nullable = false, columnDefinition = "text", unique = true)
    private String studentCode;

    @Column(name = "program_code", nullable = true, columnDefinition = "text")
    private String programCode;

    @Column(name = "cohort_year", nullable = true)
    private Integer cohortYear;

    @Column(name = "class_code", nullable = true, columnDefinition = "text")
    private String classCode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "academic_warning_level", nullable = false)
    private int academicWarningLevel = 0;

    public StudentProfile() {
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getProgramCode() {
        return programCode;
    }

    public void setProgramCode(String programCode) {
        this.programCode = programCode;
    }

    public Integer getCohortYear() {
        return cohortYear;
    }

    public void setCohortYear(Integer cohortYear) {
        this.cohortYear = cohortYear;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getAcademicWarningLevel() {
        return academicWarningLevel;
    }

    public void setAcademicWarningLevel(int academicWarningLevel) {
        this.academicWarningLevel = academicWarningLevel;
    }
}
