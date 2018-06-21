package hu.erdeiattila.poll.repository;

import hu.erdeiattila.poll.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Optional<Activity> findById(Long activityId);

    Page<Activity> findByCreatedBy(Long userId, Pageable pageable);

    long countByCreatedBy(Long userId);

    List<Activity> findByIdIn(List<Long> activityIds);

    List<Activity> findByIdIn(List<Long> activityIds, Sort sort);
}
