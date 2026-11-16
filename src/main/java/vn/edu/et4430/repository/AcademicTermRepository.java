package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.et4430.entity.AcademicTerm;
import java.util.List;
import java.util.Optional;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {
    Optional<AcademicTerm> findByAcademicYearAndTermCode(String academicYear, String termCode);

    List<AcademicTerm> findAllByOrderByAcademicYearDescTermCodeAsc();
}
