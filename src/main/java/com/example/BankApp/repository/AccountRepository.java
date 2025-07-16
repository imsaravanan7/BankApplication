package com.example.BankApp.repository;

import com.example.BankApp.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    Optional<Account> findByAccountNo(Long accountNo);
    Boolean existsByAccountNo(Long accountNo);

    @Query(nativeQuery = true,
            value = "select * from where accountNo =:accountNo and username =:username")
    Optional<Account> findByAcNoAndUsername(
            @Param("accountNo") Long accountNo, @Param("username") String username
    );
}
