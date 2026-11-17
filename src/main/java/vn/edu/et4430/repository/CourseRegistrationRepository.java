package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.et4430.entity.CourseRegistration;
import java.util.List;
import java.util.Optional;

public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {
    List<CourseRegistration> findByStudent_IdAndCampaignCourse_Campaign_Id(Long studentId, Long campaignId);

    Optional<CourseRegistration> findByStudent_IdAndCampaignCourse_Id(Long studentId, Long campaignCourseId);

    boolean existsByStudent_IdAndCampaignCourse_Id(Long studentId, Long campaignCourseId);

    long countByCampaignCourse_Id(Long campaignCourseId);
}
