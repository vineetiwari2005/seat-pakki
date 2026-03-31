package com.driver.bookMyShow.modules.receipt.repository;

import com.driver.bookMyShow.modules.receipt.entity.Receipt;
import com.driver.bookMyShow.modules.receipt.enums.ReceiptStatus;
import com.driver.bookMyShow.modules.receipt.enums.ReceiptType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Receipt Repository
 */
@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Integer> {

    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    List<Receipt> findByTicketId(Integer ticketId);

    Optional<Receipt> findByTicketIdAndReceiptType(Integer ticketId, ReceiptType receiptType);

    List<Receipt> findByStatus(ReceiptStatus status);

    List<Receipt> findByStatusAndRetryCountLessThan(ReceiptStatus status, Integer maxRetries);

    Optional<Receipt> findByReferenceIdAndReceiptType(Integer referenceId, ReceiptType receiptType);
}
