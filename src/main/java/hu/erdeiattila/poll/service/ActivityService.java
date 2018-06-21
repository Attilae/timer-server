package hu.erdeiattila.poll.service;

import hu.erdeiattila.poll.exception.BadRequestException;
import hu.erdeiattila.poll.exception.ResourceNotFoundException;
import hu.erdeiattila.poll.model.*;
import hu.erdeiattila.poll.payload.ActivityRequest;
import hu.erdeiattila.poll.payload.ActivityResponse;
import hu.erdeiattila.poll.payload.PagedResponse;
import hu.erdeiattila.poll.payload.PollRequest;
import hu.erdeiattila.poll.payload.PollResponse;
import hu.erdeiattila.poll.payload.TimerRequest;
import hu.erdeiattila.poll.payload.TimerResponse;
import hu.erdeiattila.poll.payload.VoteRequest;
import hu.erdeiattila.poll.repository.ActivityRepository;
import hu.erdeiattila.poll.repository.TimerRepository;
import hu.erdeiattila.poll.repository.UserRepository;
import hu.erdeiattila.poll.repository.VoteRepository;
import hu.erdeiattila.poll.security.UserPrincipal;
import hu.erdeiattila.poll.util.AppConstants;
import hu.erdeiattila.poll.util.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(ActivityService.class);

    public PagedResponse<ActivityResponse> getAllActivities(UserPrincipal currentUser, int page, int size) {
        validatePageNumberAndSize(page, size);

        // Retrieve Polls
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Activity> activities = activityRepository.findAll(pageable);

        if(activities.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), activities.getNumber(),
            		activities.getSize(), activities.getTotalElements(), activities.getTotalPages(), activities.isLast());
        }

        // Map Polls to PollResponses containing vote counts and poll creator details
        List<Long> activityIds = activities.map(Activity::getId).getContent();
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(activityIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, activityIds);
        Map<Long, User> creatorMap = getTimerCreatorMap(activities.getContent());

        List<ActivityResponse> activityResponses = activities.map(activity -> {
            return ModelMapper.mapActivityToActivityResponse(activity, creatorMap.get(activity.getCreatedBy()));
        }).getContent();

        return new PagedResponse<>(activityResponses, activities.getNumber(),
        		activities.getSize(), activities.getTotalElements(), activities.getTotalPages(), activities.isLast());
    }

    public PagedResponse<ActivityResponse> getActivitiesCreatedBy(String username, UserPrincipal currentUser, int page, int size) {
        validatePageNumberAndSize(page, size);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Retrieve all polls created by the given username
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Activity> activities = activityRepository.findByCreatedBy(user.getId(), pageable);

        if (activities.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), activities.getNumber(),
            		activities.getSize(), activities.getTotalElements(), activities.getTotalPages(), activities.isLast());
        }

        // Map Polls to PollResponses containing vote counts and poll creator details
        List<Long> activityIds = activities.map(Activity::getId).getContent();
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(activityIds);
        Map<Long, Long> timerUserVoteMap = getPollUserVoteMap(currentUser, activityIds);

        List<ActivityResponse> activityResponses = activities.map(activity -> {
            return ModelMapper.mapActivityToActivityResponse(activity, user);
        }).getContent();

        return new PagedResponse<>(activityResponses, activities.getNumber(),
        		activities.getSize(), activities.getTotalElements(), activities.getTotalPages(), activities.isLast());
    }

    public Activity createActivity(ActivityRequest activityRequest) {
    	Activity activity = new Activity();
    	activity.setTitle(activityRequest.getTitle());
        return activityRepository.save(activity);
    }

    public ActivityResponse getActivityById(Long activityId, UserPrincipal currentUser) {
    	Activity activity = activityRepository.findById(activityId).orElseThrow(
                () -> new ResourceNotFoundException("Timer", "id", activityId));

        // Retrieve poll creator details
        User creator = userRepository.findById(activity.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", activity.getCreatedBy()));

        return ModelMapper.mapActivityToActivityResponse(activity, creator);
    }

    private void validatePageNumberAndSize(int page, int size) {
        if(page < 0) {
            throw new BadRequestException("Page number cannot be less than zero.");
        }

        if(size > AppConstants.MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must not be greater than " + AppConstants.MAX_PAGE_SIZE);
        }
    }

    private Map<Long, Long> getChoiceVoteCountMap(List<Long> pollIds) {
        // Retrieve Vote Counts of every Choice belonging to the given pollIds
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdInGroupByChoiceId(pollIds);

        Map<Long, Long> choiceVotesMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        return choiceVotesMap;
    }

    private Map<Long, Long> getPollUserVoteMap(UserPrincipal currentUser, List<Long> pollIds) {
        // Retrieve Votes done by the logged in user to the given pollIds
        Map<Long, Long> pollUserVoteMap = null;
        if(currentUser != null) {
            List<Vote> userVotes = voteRepository.findByUserIdAndPollIdIn(currentUser.getId(), pollIds);

            pollUserVoteMap = userVotes.stream()
                    .collect(Collectors.toMap(vote -> vote.getPoll().getId(), vote -> vote.getChoice().getId()));
        }
        return pollUserVoteMap;
    }

    Map<Long, User> getTimerCreatorMap(List<Activity> list) {
        // Get Poll Creator details of the given list of polls
        List<Long> creatorIds = list.stream()
                .map(Activity::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        List<User> creators = userRepository.findByIdIn(creatorIds);
        Map<Long, User> creatorMap = creators.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return creatorMap;
    }
}
