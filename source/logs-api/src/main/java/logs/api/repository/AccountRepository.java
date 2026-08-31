package logs.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import logs.api.model.Account;

import java.util.Optional;
import java.util.Set;

public interface AccountRepository extends JpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByIdAndStatus(Long id, Integer status);

    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.status = :status WHERE a.id NOT IN :ids")
    void updateStatusNotInId(@Param("status") Integer status, @Param("ids") Set<Long> ids);
}
