package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.et4430.entity.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByOrderByPublishedAtDesc();

    boolean existsByCampaign_Id(Long campaignId);

    Optional<Notification> findByCampaign_Id(Long campaignId);
}
