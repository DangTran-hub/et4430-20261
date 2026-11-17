package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import vn.edu.et4430.entity.RegistrationCampaign;
import java.util.List;

public interface RegistrationCampaignRepository extends JpaRepository<RegistrationCampaign, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from RegistrationCampaign c where c.id = :id")
    Optional<RegistrationCampaign> findLockedById(@Param("id") Long id);

    List<RegistrationCampaign> findByStatusAndTerm_TermCodeAndTerm_TermStartDateBeforeOrderByTerm_TermStartDateDesc(
            String status, String termCode, LocalDate before);

    boolean existsByTerm_Id(Long termId);

    List<RegistrationCampaign> findByTerm_Id(Long termId);
}

