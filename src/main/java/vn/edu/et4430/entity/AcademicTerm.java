package vn.edu.et4430.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "academic_term", uniqueConstraints = @UniqueConstraint(name = "uq_academic_term", columnNames = {"academic_year", "term_code"}))
public class AcademicTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long id;

    @Column(name = "academic_year", nullable = false, columnDefinition = "text")
    private String academicYear;

    @Column(name = "term_code", nullable = false, columnDefinition = "text")
    private String termCode;

    @Column(name = "term_start_date", nullable = false)
    private LocalDate termStartDate;

    @Column(name = "term_end_date", nullable = false)
    private LocalDate termEndDate;

    @Column(name = "status", nullable = false, columnDefinition = "text")
    private String status;

    public AcademicTerm() {
    }

    public Long getId() {
        return id;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getTermCode() {
        return termCode;
    }

    public void setTermCode(String termCode) {
        this.termCode = termCode;
    }

    public LocalDate getTermStartDate() {
        return termStartDate;
    }

    public void setTermStartDate(LocalDate termStartDate) {
        this.termStartDate = termStartDate;
    }

    public LocalDate getTermEndDate() {
        return termEndDate;
    }

    public void setTermEndDate(LocalDate termEndDate) {
        this.termEndDate = termEndDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

