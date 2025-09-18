package com.linglevel.api.user.ticket.repository;

import com.linglevel.api.user.ticket.entity.UserTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataMongoTest
class UserTicketRepositoryTest {

    @Autowired
    private UserTicketRepository userTicketRepository;

    private final String testUserId = "test-user-id";
    private UserTicket userTicket;

    @BeforeEach
    void setUp() {
        userTicketRepository.deleteAll();

        userTicket = UserTicket.builder()
                .userId(testUserId)
                .balance(10)
                .build();
    }

    @Test
    void 사용자ID로_티켓조회_성공() {
        // given
        UserTicket savedUserTicket = userTicketRepository.save(userTicket);

        // when
        Optional<UserTicket> found = userTicketRepository.findByUserId(testUserId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(testUserId);
        assertThat(found.get().getBalance()).isEqualTo(10);
        assertThat(found.get().getId()).isEqualTo(savedUserTicket.getId());
    }

    @Test
    void 존재하지않는사용자ID로_조회시_빈Optional반환() {
        // when
        Optional<UserTicket> found = userTicketRepository.findByUserId("non-existent-user");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void 중복된사용자ID로_티켓생성시_예외발생() {
        // given
        userTicketRepository.save(userTicket);

        UserTicket duplicateUserTicket = UserTicket.builder()
                .userId(testUserId)
                .balance(20)
                .build();

        // when & then
        assertThatThrownBy(() -> userTicketRepository.save(duplicateUserTicket))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void 버전충돌시_OptimisticLockingFailureException발생() {
        // given
        UserTicket savedUserTicket = userTicketRepository.save(userTicket);

        UserTicket userTicket1 = userTicketRepository.findById(savedUserTicket.getId()).orElseThrow();
        UserTicket userTicket2 = userTicketRepository.findById(savedUserTicket.getId()).orElseThrow();

        // when
        userTicket1.setBalance(15);
        userTicketRepository.save(userTicket1);

        userTicket2.setBalance(25);

        // then
        assertThatThrownBy(() -> userTicketRepository.save(userTicket2))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void 티켓잔고_업데이트_성공() {
        // given
        UserTicket savedUserTicket = userTicketRepository.save(userTicket);
        Long originalVersion = savedUserTicket.getVersion();

        // when
        savedUserTicket.setBalance(20);
        UserTicket updatedUserTicket = userTicketRepository.save(savedUserTicket);

        // then
        assertThat(updatedUserTicket.getBalance()).isEqualTo(20);
        assertThat(updatedUserTicket.getVersion()).isGreaterThan(originalVersion);
    }

    @Test
    void 기본필드_자동설정_확인() {
        // when
        UserTicket savedUserTicket = userTicketRepository.save(userTicket);

        // then
        assertThat(savedUserTicket.getId()).isNotNull();
        assertThat(savedUserTicket.getUserId()).isEqualTo(testUserId);
        assertThat(savedUserTicket.getBalance()).isEqualTo(10);
        assertThat(savedUserTicket.getVersion()).isNotNull();
    }
}