package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.et4430.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
}

