package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import vn.edu.et4430.entity.AcademicTerm;
import java.util.List;
import java.util.Optional;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AcademicTerm t where t.id = :id")
    Optional<AcademicTerm> findLockedById(@Param("id") Long id);

    Optional<AcademicTerm> findByAcademicYearAndTermCode(String academicYear, String termCode);

    List<AcademicTerm> findAllByOrderByAcademicYearDescTermCodeAsc();
}
