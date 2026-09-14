package com.linglevel.api.user.ticket.service;

import com.linglevel.api.user.ticket.dto.TicketBalanceResponse;
import com.linglevel.api.user.ticket.dto.TicketTransactionResponse;
import com.linglevel.api.user.ticket.entity.TicketReservation;
import com.linglevel.api.user.ticket.entity.TicketReservationStatus;
import com.linglevel.api.user.ticket.entity.TicketTransaction;
import com.linglevel.api.user.ticket.entity.UserTicket;
import com.linglevel.api.user.ticket.exception.TicketErrorCode;
import com.linglevel.api.user.ticket.exception.TicketException;
import com.linglevel.api.user.ticket.repository.TicketTransactionRepository;
import com.linglevel.api.user.ticket.repository.TicketReservationRepository;
import com.linglevel.api.user.ticket.repository.UserTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

	private final UserTicketRepository userTicketRepository;

	private final TicketTransactionRepository ticketTransactionRepository;

	private final TicketReservationRepository ticketReservationRepository;

	@Transactional
	public TicketBalanceResponse getTicketBalance(String userId) {
		UserTicket userTicket = getOrCreateUserTicket(userId);
		return TicketBalanceResponse.builder()
			.balance(userTicket.getBalance())
			.updatedAt(userTicket.getUpdatedAt())
			.build();
	}

	@Transactional
	public Page<TicketTransactionResponse> getTicketTransactions(String userId, int page, int limit) {
		// 지갑이 없으면 생성 (잔고 조회와 동일한 동작)
		getOrCreateUserTicket(userId);

		PageRequest pageRequest = PageRequest.of(page - 1, limit);
		Page<TicketTransaction> transactions = ticketTransactionRepository
			.findByUserIdOrderByCreatedAtDesc(toUserId(userId), pageRequest);

		return transactions.map(this::toTicketTransactionResponse);
	}

	@Transactional
	public String reserveTicket(String userId, int amount, String description) {
		UserTicket userTicket = getOrCreateUserTicket(userId);

		// 잔고 확인
		if (userTicket.getBalance() < amount) {
			throw new TicketException(TicketErrorCode.INSUFFICIENT_BALANCE);
		}

		// 예약 금액은 사용 가능 잔액에서 제외한다.
		userTicket.setBalance(userTicket.getBalance() - amount);
		userTicketRepository.save(userTicket);

		TicketReservation reservation = TicketReservation.builder()
			.userId(toUserId(userId))
			.amount(amount)
			.description(description)
			.status(TicketReservationStatus.RESERVED)
			.build();
		TicketReservation savedReservation = ticketReservationRepository.save(reservation);

		return savedReservation.getId().toString();
	}

	@Transactional
	public void confirmReservation(String reservationId) {
		TicketReservation reservation = ticketReservationRepository.findById(toReservationId(reservationId))
			.filter(candidate -> candidate.getStatus() == TicketReservationStatus.RESERVED)
			.orElseThrow(() -> new TicketException(TicketErrorCode.RESERVATION_NOT_FOUND));

		reservation.setStatus(TicketReservationStatus.CONFIRMED);
		ticketReservationRepository.save(reservation);
		ticketTransactionRepository.save(TicketTransaction.builder()
			.userId(reservation.getUserId())
			.amount(-reservation.getAmount())
			.description(reservation.getDescription())
			.reservationId(reservation.getId())
			.build());
	}

	@Transactional
	public void cancelReservation(String reservationId) {
		TicketReservation reservation = ticketReservationRepository.findById(toReservationId(reservationId))
			.filter(candidate -> candidate.getStatus() == TicketReservationStatus.RESERVED)
			.orElseThrow(() -> new TicketException(TicketErrorCode.RESERVATION_NOT_FOUND));

		UserTicket userTicket = getOrCreateUserTicket(reservation.getUserId());
		userTicket.setBalance(userTicket.getBalance() + reservation.getAmount());
		userTicketRepository.save(userTicket);

		reservation.setStatus(TicketReservationStatus.CANCELLED);
		ticketReservationRepository.save(reservation);
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
			.userId(toUserId(userId))
			.amount(-amount) // 음수로 저장
			.description(description)
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
			.userId(toUserId(userId))
			.amount(amount) // 양수로 저장
			.description(description)
			.build();
		ticketTransactionRepository.save(transaction);

		return userTicket.getBalance();
	}

	private UserTicket getOrCreateUserTicket(String userId) {
		return getOrCreateUserTicket(toUserId(userId));
	}

	private UserTicket getOrCreateUserTicket(Long userId) {
		return userTicketRepository.findById(userId).orElseGet(() -> createDefaultUserTicket(userId));
	}

	/**
	 * 기본 사용자 티켓을 생성합니다 🎁 이벤트: 최초 지갑 생성 시 10개 티켓 지급
	 */
	private UserTicket createDefaultUserTicket(Long userId) {
		UserTicket userTicket = UserTicket.builder()
			.userId(userId)
			.balance(10) // 🎁 이벤트: 최초 10개 티켓 지급
			.build();
		UserTicket savedUserTicket = userTicketRepository.save(userTicket);

		TicketTransaction welcomeTransaction = TicketTransaction.builder()
			.userId(userId)
			.amount(10)
			.description("Welcome bonus for new user")
			.build();
		ticketTransactionRepository.save(welcomeTransaction);

		return savedUserTicket;
	}

	private TicketTransactionResponse toTicketTransactionResponse(TicketTransaction transaction) {
		return TicketTransactionResponse.builder()
			.id(transaction.getId().toString())
			.amount(transaction.getAmount())
			.description(transaction.getDescription())
			.createdAt(transaction.getCreatedAt())
			.build();
	}

	private Long toUserId(String userId) {
		try {
			return Long.parseLong(userId);
		}
		catch (NumberFormatException exception) {
			throw new TicketException(TicketErrorCode.TICKET_NOT_FOUND);
		}
	}

	private Long toReservationId(String reservationId) {
		try {
			return Long.parseLong(reservationId);
		}
		catch (NumberFormatException exception) {
			throw new TicketException(TicketErrorCode.RESERVATION_NOT_FOUND);
		}
	}

}
