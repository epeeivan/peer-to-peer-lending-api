package com.taf.p2plending.loan;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);

    @Query("select l.id from Loan l where l.status = com.taf.p2plending.loan.LoanStatus.PENDING and l.fundingDeadline < :now")
    List<Long> findExpiredPendingIds(@Param("now") OffsetDateTime now);
}
