package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.et4430.entity.RegistrationCampaign;
import java.util.List;

public interface RegistrationCampaignRepository extends JpaRepository<RegistrationCampaign, Long> {
    boolean existsByTerm_Id(Long termId);

    List<RegistrationCampaign> findByTerm_Id(Long termId);
}

