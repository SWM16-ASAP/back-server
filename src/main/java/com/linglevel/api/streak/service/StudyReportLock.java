package com.linglevel.api.streak.service;

import com.linglevel.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** Serializes user learning mutations, including creation of the first report. */
@Component
@RequiredArgsConstructor
public class StudyReportLock {

	private final UserRepository users;

	@Transactional(propagation = Propagation.MANDATORY)
	public void lock(String userId) {
		users.findForUpdateById(Long.valueOf(userId)).orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

}
