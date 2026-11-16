package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.et4430.entity.CampaignCourse;
import java.util.List;

public interface CampaignCourseRepository extends JpaRepository<CampaignCourse, Long> {
    List<CampaignCourse> findByCampaign_Id(Long campaignId);
}

