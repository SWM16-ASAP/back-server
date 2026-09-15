package com.linglevel.api.content.custom.service;

import com.linglevel.api.common.AbstractMysqlTest;
import com.linglevel.api.content.custom.dto.*;
import com.linglevel.api.content.custom.entity.*;
import com.linglevel.api.content.custom.repository.*;
import com.linglevel.api.content.common.service.ReadingTimeService;
import com.linglevel.api.user.entity.*;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.user.ticket.entity.*;
import com.linglevel.api.user.ticket.repository.*;
import com.linglevel.api.user.ticket.service.TicketService;
import com.linglevel.api.s3.service.*;
import com.linglevel.api.s3.strategy.CustomContentPathStrategy;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/mysql,classpath:db/testmigration/mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ CustomContentWebhookService.class, CustomContentImportService.class, UserCustomContentService.class,
		CustomContentReadingTimeService.class, TicketService.class, CustomContentRequestService.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CustomContentCompletionIntegrationTest extends AbstractMysqlTest {

	@Autowired
	CustomContentWebhookService webhook;

	@Autowired
	CustomContentRequestService requestService;

	@MockitoBean
	com.linglevel.api.crawling.service.CrawlingService crawling;

	@Autowired
	UserRepository users;

	@Autowired
	ContentRequestRepository requests;

	@Autowired
	CustomContentRepository contents;

	@Autowired
	UserCustomContentRepository ownership;

	@Autowired
	TicketService tickets;

	@Autowired
	TicketReservationRepository reservations;

	@Autowired
	TicketTransactionRepository transactions;

	@Autowired
	UserTicketRepository wallets;

	@MockitoBean
	S3AiService ai;

	@MockitoBean
	S3TransferService transfer;

	@MockitoBean
	S3UrlService urls;

	@MockitoBean
	ImageResizeService resize;

	@MockitoBean
	CustomContentPathStrategy paths;

	@MockitoBean
	CustomContentNotificationService notifications;

	@MockitoBean
	CustomContentChunkRepository chunks;

	@MockitoBean
	ReadingTimeService readingTime;

	ContentRequest request;

	@BeforeEach
	void setup() {
		User user = users
			.saveAndFlush(User.builder().username(UUID.randomUUID().toString()).role(UserRole.USER).build());
		String reservation = tickets.reserveTicket(user.getId().toString(), 1, "content");
		request = requests.saveAndFlush(ContentRequest.builder()
			.requestKey(UUID.randomUUID().toString())
			.userId(user.getId())
			.ticketReservationId(Long.valueOf(reservation))
			.title("source")
			.contentType(ContentType.TEXT)
			.build());
		AiResultDto result = new AiResultDto();
		result.setTitle("Generated");
		result.setOriginalTextLevel("A1");
		AiResultDto.Chunk chunk = new AiResultDto.Chunk();
		chunk.setChunkText("Hello world");
		chunk.setIsImage(false);
		AiResultDto.Chapter chapter = new AiResultDto.Chapter();
		chapter.setChunks(List.of(chunk));
		AiResultDto.LeveledResult level = new AiResultDto.LeveledResult();
		level.setTextLevel("A1");
		level.setChapters(List.of(chapter));
		result.setLeveledResults(List.of(level));
		when(ai.downloadJsonFile(eq(request.getRequestKey()), eq(AiResultDto.class), any())).thenReturn(result);
	}

	@Test
	void completionCommitsMetadataOwnershipAndTicketOnce() {
		webhook.handleContentCompleted(completed());
		webhook.handleContentCompleted(completed());
		ContentRequest done = requests.findById(request.getId()).orElseThrow();
		assertThat(done.getStatus()).isEqualTo(ContentRequestStatus.COMPLETED);
		assertThat(done.getProgress()).isEqualTo(100);
		assertThat(ownership.existsByUserIdAndCustomContentId(done.getUserId(), done.getResultCustomContentId()))
			.isTrue();
		assertThat(contents.findById(done.getResultCustomContentId())).isPresent();
		assertThat(reservations.findById(done.getTicketReservationId()).orElseThrow().getStatus())
			.isEqualTo(TicketReservationStatus.CONFIRMED);
		assertThat(
				transactions.findAll().stream().filter(t -> done.getTicketReservationId().equals(t.getReservationId())))
			.hasSize(1);
		verify(chunks).saveAll(argThat(values -> {
			CustomContentChunk chunk = values.iterator().next();
			return chunk.getCustomContentId().equals(done.getResultCustomContentId())
					&& chunk.getUserId().equals(done.getUserId());
		}));
		verify(notifications, times(1)).sendContentCompletedNotification(anyString(), anyString(), anyString(),
				anyString());
	}

	@Test
	void concurrentDuplicateCompletionCreatesSingleResult() throws Exception {
		ExecutorService workers = Executors.newFixedThreadPool(2);
		try {
			Future<?> first = workers.submit(() -> webhook.handleContentCompleted(completed()));
			Future<?> second = workers.submit(() -> webhook.handleContentCompleted(completed()));
			first.get(20, TimeUnit.SECONDS);
			second.get(20, TimeUnit.SECONDS);
			assertThat(ownership.findByUserId(request.getUserId())).hasSize(1);
			verify(chunks, times(1)).saveAll(any());
		}
		finally {
			workers.shutdownNow();
		}
	}

	@Test
	void mongoFailureRollsBackSqlAndAllowsRetry() {
		doThrow(new IllegalStateException("Mongo unavailable")).when(chunks).saveAll(any());
		assertThatThrownBy(() -> webhook.handleContentCompleted(completed())).isInstanceOf(RuntimeException.class);
		assertThat(requests.findById(request.getId()).orElseThrow().getStatus())
			.isEqualTo(ContentRequestStatus.PENDING);
		assertThat(ownership.findByUserId(request.getUserId())).isEmpty();
		assertThat(reservations.findById(request.getTicketReservationId()).orElseThrow().getStatus())
			.isEqualTo(TicketReservationStatus.RESERVED);
		doReturn(List.of()).when(chunks).saveAll(any());
		webhook.handleContentCompleted(completed());
		assertThat(requests.findById(request.getId()).orElseThrow().getStatus())
			.isEqualTo(ContentRequestStatus.COMPLETED);
	}

	@Test
	void repeatedFailureReleasesReservationOnce() {
		webhook.handleContentFailed(failed());
		webhook.handleContentFailed(failed());
		assertThat(wallets.findById(request.getUserId()).orElseThrow().getBalance()).isEqualTo(10);
		assertThat(reservations.findById(request.getTicketReservationId()).orElseThrow().getStatus())
			.isEqualTo(TicketReservationStatus.CANCELLED);
		assertThatThrownBy(() -> webhook.handleContentCompleted(completed())).isInstanceOf(RuntimeException.class);
		assertThat(ownership.findByUserId(request.getUserId())).isEmpty();
	}

	@Test
	void lateFailureAndProgressCannotOverwriteCompletion() {
		webhook.handleContentCompleted(completed());
		webhook.handleContentFailed(failed());
		CustomContentProgressRequest progress = new CustomContentProgressRequest();
		progress.setRequestId(request.getRequestKey());
		progress.setProgress(5);
		webhook.handleContentProgress(progress);
		assertThat(requests.findById(request.getId()).orElseThrow().getStatus())
			.isEqualTo(ContentRequestStatus.COMPLETED);
		assertThat(wallets.findById(request.getUserId()).orElseThrow().getBalance()).isEqualTo(9);
	}

	@Test
	void duplicateOwnershipIsRejected() {
		webhook.handleContentCompleted(completed());
		UserCustomContent existing = ownership.findByUserId(request.getUserId()).get(0);
		assertThatThrownBy(() -> ownership.saveAndFlush(UserCustomContent.builder()
			.userId(existing.getUserId())
			.customContentId(existing.getCustomContentId())
			.contentRequestId(existing.getContentRequestId())
			.build())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void cachedContentGrantsOwnershipAndConfirmsNewReservation() {
		webhook.handleContentCompleted(completed());
		Long contentId = requests.findById(request.getId()).orElseThrow().getResultCustomContentId();
		CustomContent content = contents.findById(contentId).orElseThrow();
		content.setOriginUrl("https://example.com/article");
		contents.saveAndFlush(content);
		User reader = users
			.saveAndFlush(User.builder().username(UUID.randomUUID().toString()).role(UserRole.USER).build());
		CreateContentRequestRequest input = new CreateContentRequestRequest();
		input.setTitle("cached");
		input.setContentType(ContentType.LINK);
		input.setOriginUrl(content.getOriginUrl());
		CreateContentRequestResponse result = requestService.createContentRequest(reader.getId().toString(), input);
		assertThat(result.isCached()).isTrue();
		ContentRequest cached = requests.findByRequestKey(result.getRequestId()).orElseThrow();
		assertThat(cached.getResultCustomContentId()).isEqualTo(contentId);
		assertThat(ownership.existsByUserIdAndCustomContentId(reader.getId(), contentId)).isTrue();
		assertThat(reservations.findById(cached.getTicketReservationId()).orElseThrow().getStatus())
			.isEqualTo(TicketReservationStatus.CONFIRMED);
		assertThat(wallets.findById(reader.getId()).orElseThrow().getBalance()).isEqualTo(9);
	}

	@Test
	void emptyAiResultDoesNotConsumeReservation() {
		AiResultDto empty = new AiResultDto();
		empty.setLeveledResults(List.of());
		when(ai.downloadJsonFile(anyString(), eq(AiResultDto.class), any())).thenReturn(empty);
		assertThatThrownBy(() -> webhook.handleContentCompleted(completed())).isInstanceOf(RuntimeException.class);
		assertThat(ownership.findByUserId(request.getUserId())).isEmpty();
		assertThat(reservations.findById(request.getTicketReservationId()).orElseThrow().getStatus())
			.isEqualTo(TicketReservationStatus.RESERVED);
		verifyNoInteractions(notifications);
	}

	private CustomContentCompletedRequest completed() {
		CustomContentCompletedRequest value = new CustomContentCompletedRequest();
		value.setRequestId(request.getRequestKey());
		return value;
	}

	private CustomContentFailedRequest failed() {
		CustomContentFailedRequest value = new CustomContentFailedRequest();
		value.setRequestId(request.getRequestKey());
		value.setErrorMessage("AI failed");
		return value;
	}

}
