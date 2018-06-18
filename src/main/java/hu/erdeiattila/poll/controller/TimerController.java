package hu.erdeiattila.poll.controller;

import hu.erdeiattila.poll.model.*;
import hu.erdeiattila.poll.payload.*;
import hu.erdeiattila.poll.repository.TimerRepository;
import hu.erdeiattila.poll.repository.UserRepository;
import hu.erdeiattila.poll.repository.VoteRepository;
import hu.erdeiattila.poll.security.CurrentUser;
import hu.erdeiattila.poll.security.UserPrincipal;
import hu.erdeiattila.poll.service.PollService;
import hu.erdeiattila.poll.service.TimerService;
import hu.erdeiattila.poll.util.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import javax.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/api/timer")
public class TimerController {

	@Autowired
	private TimerRepository timerRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TimerService timerService;

	private static final Logger logger = LoggerFactory.getLogger(TimerController.class);

	@GetMapping
	public PagedResponse<TimerResponse> getTimers(@CurrentUser UserPrincipal currentUser,
			@RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
			@RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
		return timerService.getAllTimers(currentUser, page, size);
	}

	@PostMapping
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> createTimer(@Valid @RequestBody TimerRequest timerRequest) {
		Timer timer = timerService.createTimer(timerRequest);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{timerId}").buildAndExpand(timer.getId())
				.toUri();

		return ResponseEntity.created(location).body(new ApiResponse(true, "Timer Created Successfully"));
	}

	@GetMapping("/{timerId}")
	public TimerResponse getTimerById(@CurrentUser UserPrincipal currentUser, @PathVariable Long timerId) {
		return timerService.getTimerById(timerId, currentUser);
	}

	/*@PostMapping("/{pollId}/votes")
	@PreAuthorize("hasRole('USER')")
	public PollResponse castVote(@CurrentUser UserPrincipal currentUser, @PathVariable Long pollId,
			@Valid @RequestBody VoteRequest voteRequest) {
		return pollService.castVoteAndGetUpdatedPoll(pollId, voteRequest, currentUser);
	}*/
}
