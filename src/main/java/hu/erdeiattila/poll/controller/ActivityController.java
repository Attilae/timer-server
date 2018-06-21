package hu.erdeiattila.poll.controller;

import hu.erdeiattila.poll.model.*;
import hu.erdeiattila.poll.payload.*;
import hu.erdeiattila.poll.repository.ActivityRepository;
import hu.erdeiattila.poll.repository.TimerRepository;
import hu.erdeiattila.poll.repository.UserRepository;
import hu.erdeiattila.poll.repository.VoteRepository;
import hu.erdeiattila.poll.security.CurrentUser;
import hu.erdeiattila.poll.security.UserPrincipal;
import hu.erdeiattila.poll.service.ActivityService;
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
@RequestMapping("/api/activity")
public class ActivityController {

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ActivityService activityService;

	private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);

	@GetMapping
	public PagedResponse<ActivityResponse> getActivities(@CurrentUser UserPrincipal currentUser,
			@RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
			@RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
		return activityService.getAllActivities(currentUser, page, size);
	}

	@PostMapping
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> createActivity(@Valid @RequestBody ActivityRequest activityRequest) {
		Activity activity = activityService.createActivity(activityRequest);

		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{activityId}").buildAndExpand(activity.getId())
				.toUri();

		return ResponseEntity.created(location).body(new ApiResponse(true, "Timer Created Successfully"));
	}

	@GetMapping("/{activityIdId}")
	public ActivityResponse getActivityById(@CurrentUser UserPrincipal currentUser, @PathVariable Long activityId) {
		return activityService.getActivityById(activityId, currentUser);
	}

	/*@PostMapping("/{pollId}/votes")
	@PreAuthorize("hasRole('USER')")
	public PollResponse castVote(@CurrentUser UserPrincipal currentUser, @PathVariable Long pollId,
			@Valid @RequestBody VoteRequest voteRequest) {
		return pollService.castVoteAndGetUpdatedPoll(pollId, voteRequest, currentUser);
	}*/
}
