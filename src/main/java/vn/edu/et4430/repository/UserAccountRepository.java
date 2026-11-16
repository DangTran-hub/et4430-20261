package vn.edu.et4430.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.et4430.entity.UserAccount;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
}

