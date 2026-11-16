package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.et4430.entity.CourseTermRule;
import java.util.List;

public interface CourseTermRuleRepository extends JpaRepository<CourseTermRule, Long> {
    List<CourseTermRule> findByTermCodeAndActiveTrue(String termCode);
}

