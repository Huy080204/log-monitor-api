package logs.api.repository;

import logs.api.model.QueryTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface QueryTemplateRepository extends JpaRepository<QueryTemplate, Long>, JpaSpecificationExecutor<QueryTemplate> {
    boolean existsByNameAndApplicationId(String name, Long applicationId);

    boolean existsByNameAndApplicationIdIsNull(String name);

    boolean existsByNameAndApplicationIdAndIdNot(String name, Long applicationId, Long id);

    boolean existsByNameAndApplicationIdIsNullAndIdNot(String name, Long id);

    @Modifying
    @Transactional
    @Query("DELETE FROM QueryTemplate q WHERE q.application.id = :applicationId")
    void deleteAllByApplicationId(@Param("applicationId") Long applicationId);
}
