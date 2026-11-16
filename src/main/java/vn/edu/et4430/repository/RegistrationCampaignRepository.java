package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.et4430.entity.RegistrationCampaign;
import java.util.List;

public interface RegistrationCampaignRepository extends JpaRepository<RegistrationCampaign, Long> {
    boolean existsByTerm_Id(Long termId);

    List<RegistrationCampaign> findByTerm_Id(Long termId);

    /** UC1 4a: đã có đợt chưa bị hủy cho học kỳ này chưa. */
    boolean existsByTerm_IdAndStatusNot(Long termId, String status);

    List<RegistrationCampaign> findByStatusNotOrderByCreatedAtDesc(String status);

    List<RegistrationCampaign> findByStatusOrderByPublishedAtDesc(String status);

    /** UC1 bước 3: các đợt đã công bố của cùng loại học kỳ, năm học trước, mới nhất trước. */
    @Query("select c from RegistrationCampaign c join fetch c.term t "
            + "where t.termCode = :termCode and t.academicYear < :academicYear and c.status = :status "
            + "order by t.academicYear desc")
    List<RegistrationCampaign> findPreviousOfSameTermType(@Param("termCode") String termCode,
                                                          @Param("academicYear") String academicYear,
                                                          @Param("status") String status);
}
