package hu.erdeiattila.poll.repository;

import hu.erdeiattila.poll.model.Timer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimerRepository extends JpaRepository<Timer, Long> {
    Optional<Timer> findById(Long timerId);

    Page<Timer> findByCreatedBy(Long userId, Pageable pageable);

    long countByCreatedBy(Long userId);

    List<Timer> findByIdIn(List<Long> timerIds);

    List<Timer> findByIdIn(List<Long> timerIds, Sort sort);
}
