package com.linglevel.api.user.ticket.service;

import com.linglevel.api.user.ticket.dto.TicketBalanceResponse;
import com.linglevel.api.user.ticket.dto.TicketTransactionResponse;
import com.linglevel.api.user.ticket.entity.TicketTransaction;
import com.linglevel.api.user.ticket.entity.TransactionStatus;
import com.linglevel.api.user.ticket.entity.UserTicket;
import com.linglevel.api.user.ticket.exception.TicketErrorCode;
import com.linglevel.api.user.ticket.exception.TicketException;
import com.linglevel.api.user.ticket.repository.TicketTransactionRepository;
import com.linglevel.api.user.ticket.repository.UserTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

	private final UserTicketRepository userTicketRepository;

	private final TicketTransactionRepository ticketTransactionRepository;

	public TicketBalanceResponse getTicketBalance(String userId) {
		UserTicket userTicket = getOrCreateUserTicket(userId);
		return TicketBalanceResponse.builder()
			.balance(userTicket.getBalance())
			.updatedAt(userTicket.getUpdatedAt())
			.build();
	}

	public Page<TicketTransactionResponse> getTicketTransactions(String userId, int page, int limit) {
		// 지갑이 없으면 생성 (잔고 조회와 동일한 동작)
		getOrCreateUserTicket(userId);

		PageRequest pageRequest = PageRequest.of(page - 1, limit);
		Page<TicketTransaction> transactions = ticketTransactionRepository
			.findByUserIdAndStatusOrderByCreatedAtDesc(userId, TransactionStatus.CONFIRMED, pageRequest);

		return transactions.map(this::toTicketTransactionResponse);
	}

	@Transactional
	public String reserveTicket(String userId, int amount, String description) {
		UserTicket userTicket = getOrCreateUserTicket(userId);

		// 잔고 확인
		if (userTicket.getBalance() < amount) {
			throw new TicketException(TicketErrorCode.INSUFFICIENT_BALANCE);
		}

		String reservationId = UUID.randomUUID().toString();

		// 티켓 차감 (예약 상태)
		userTicket.setBalance(userTicket.getBalance() - amount);
		userTicketRepository.save(userTicket);

		// 예약 거래 내역 기록
		TicketTransaction transaction = TicketTransaction.builder()
			.userId(userId)
			.amount(-amount)
			.description(description)
			.status(TransactionStatus.RESERVED)
			.reservationId(reservationId)
			.build();
		ticketTransactionRepository.save(transaction);

		return reservationId;
	}

	@Transactional
	public void confirmReservation(String reservationId) {
		TicketTransaction transaction = ticketTransactionRepository
			.findByReservationIdAndStatus(reservationId, TransactionStatus.RESERVED)
			.orElseThrow(() -> new TicketException(TicketErrorCode.RESERVATION_NOT_FOUND));

		// 예약 상태를 확정으로 변경
		transaction.setStatus(TransactionStatus.CONFIRMED);
		ticketTransactionRepository.save(transaction);
	}

	@Transactional
	public void cancelReservation(String reservationId) {
		TicketTransaction transaction = ticketTransactionRepository
			.findByReservationIdAndStatus(reservationId, TransactionStatus.RESERVED)
			.orElseThrow(() -> new TicketException(TicketErrorCode.RESERVATION_NOT_FOUND));

		// 티켓 복구
		UserTicket userTicket = getOrCreateUserTicket(transaction.getUserId());
		userTicket.setBalance(userTicket.getBalance() + Math.abs(transaction.getAmount()));
		userTicketRepository.save(userTicket);

		// 예약 상태를 취소로 변경
		transaction.setStatus(TransactionStatus.CANCELLED);
		ticketTransactionRepository.save(transaction);
	}

	/**
	 * 티켓을 사용합니다. (내부 로직에서만 사용 - 즉시 확정)
	 * @param userId 사용자 ID
	 * @param amount 사용할 티켓 수
	 * @param description 사용 내역 설명
	 * @return 남은 티켓 잔고
	 */
	@Transactional
	public int spendTicket(String userId, int amount, String description) {
		UserTicket userTicket = getOrCreateUserTicket(userId);

		// 잔고 확인
		if (userTicket.getBalance() < amount) {
			throw new TicketException(TicketErrorCode.INSUFFICIENT_BALANCE);
		}

		// 티켓 차감
		userTicket.setBalance(userTicket.getBalance() - amount);
		userTicketRepository.save(userTicket);

		// 거래 내역 기록
		TicketTransaction transaction = TicketTransaction.builder()
			.userId(userId)
			.amount(-amount) // 음수로 저장
			.description(description)
			.status(TransactionStatus.CONFIRMED)
			.build();
		ticketTransactionRepository.save(transaction);

		return userTicket.getBalance();
	}

	/**
	 * 티켓을 지급합니다. (관리자 또는 시스템에서 사용)
	 * @param userId 사용자 ID
	 * @param amount 지급할 티켓 수
	 * @param description 지급 사유
	 * @return 지급 후 티켓 잔고
	 */
	@Transactional
	public int grantTicket(String userId, int amount, String description) {
		UserTicket userTicket = getOrCreateUserTicket(userId);

		// 티켓 지급
		userTicket.setBalance(userTicket.getBalance() + amount);
		userTicketRepository.save(userTicket);

		// 거래 내역 기록
		TicketTransaction transaction = TicketTransaction.builder()
			.userId(userId)
			.amount(amount) // 양수로 저장
			.description(description)
			.status(TransactionStatus.CONFIRMED)
			.build();
		ticketTransactionRepository.save(transaction);

		return userTicket.getBalance();
	}

	private UserTicket getOrCreateUserTicket(String userId) {
		return userTicketRepository.findByUserId(userId).orElseGet(() -> createDefaultUserTicket(userId));
	}

	/**
	 * 기본 사용자 티켓을 생성합니다 🎁 이벤트: 최초 지갑 생성 시 10개 티켓 지급
	 */
	private UserTicket createDefaultUserTicket(String userId) {
		UserTicket userTicket = UserTicket.builder()
			.userId(userId)
			.balance(10) // 🎁 이벤트: 최초 10개 티켓 지급
			.build();
		UserTicket savedUserTicket = userTicketRepository.save(userTicket);

		TicketTransaction welcomeTransaction = TicketTransaction.builder()
			.userId(userId)
			.amount(10)
			.description("Welcome bonus for new user")
			.status(TransactionStatus.CONFIRMED)
			.build();
		ticketTransactionRepository.save(welcomeTransaction);

		return savedUserTicket;
	}

	private TicketTransactionResponse toTicketTransactionResponse(TicketTransaction transaction) {
		return TicketTransactionResponse.builder()
			.id(transaction.getId())
			.amount(transaction.getAmount())
			.description(transaction.getDescription())
			.createdAt(transaction.getCreatedAt())
			.build();
	}

}