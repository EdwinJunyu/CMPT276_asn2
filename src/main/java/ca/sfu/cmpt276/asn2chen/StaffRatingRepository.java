package ca.sfu.cmpt276.asn2chen;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRatingRepository extends JpaRepository<StaffRating, Long> {

    List<StaffRating> findAllByOrderByIdDesc();
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}
