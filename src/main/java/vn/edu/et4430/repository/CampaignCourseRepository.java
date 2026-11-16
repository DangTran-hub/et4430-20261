package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.et4430.entity.CampaignCourse;
import java.util.List;

public interface CampaignCourseRepository extends JpaRepository<CampaignCourse, Long> {
    List<CampaignCourse> findByCampaign_Id(Long campaignId);

    List<CampaignCourse> findByCampaign_IdOrderByCourse_CourseCodeAsc(Long campaignId);

    List<CampaignCourse> findByCampaign_IdAndOfferingStatusOrderByCourse_CourseCodeAsc(Long campaignId, String offeringStatus);

    boolean existsByCampaign_IdAndCourse_Id(Long campaignId, Long courseId);

    long countByCampaign_IdAndOfferingStatus(Long campaignId, String offeringStatus);
}
