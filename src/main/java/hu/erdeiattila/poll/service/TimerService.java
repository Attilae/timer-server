package hu.erdeiattila.poll.service;

import hu.erdeiattila.poll.exception.BadRequestException;
import hu.erdeiattila.poll.exception.ResourceNotFoundException;
import hu.erdeiattila.poll.model.*;
import hu.erdeiattila.poll.payload.PagedResponse;
import hu.erdeiattila.poll.payload.PollRequest;
import hu.erdeiattila.poll.payload.PollResponse;
import hu.erdeiattila.poll.payload.TimerRequest;
import hu.erdeiattila.poll.payload.TimerResponse;
import hu.erdeiattila.poll.payload.VoteRequest;
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
public class TimerService {

    @Autowired
    private TimerRepository timerRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(TimerService.class);

    public PagedResponse<TimerResponse> getAllTimers(UserPrincipal currentUser, int page, int size) {
        validatePageNumberAndSize(page, size);

        // Retrieve Polls
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Timer> timers = timerRepository.findAll(pageable);

        if(timers.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), timers.getNumber(),
            		timers.getSize(), timers.getTotalElements(), timers.getTotalPages(), timers.isLast());
        }

        // Map Polls to PollResponses containing vote counts and poll creator details
        List<Long> pollIds = timers.map(Timer::getId).getContent();
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);
        Map<Long, User> creatorMap = getTimerCreatorMap(timers.getContent());

        List<TimerResponse> timerResponses = timers.map(timer -> {
            return ModelMapper.mapTimerToTimerResponse(timer, creatorMap.get(timer.getCreatedBy()));
        }).getContent();

        return new PagedResponse<>(timerResponses, timers.getNumber(),
        		timers.getSize(), timers.getTotalElements(), timers.getTotalPages(), timers.isLast());
    }

    public PagedResponse<TimerResponse> getTimersCreatedBy(String username, UserPrincipal currentUser, int page, int size) {
        validatePageNumberAndSize(page, size);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // Retrieve all polls created by the given username
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Timer> timers = timerRepository.findByCreatedBy(user.getId(), pageable);

        if (timers.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), timers.getNumber(),
            		timers.getSize(), timers.getTotalElements(), timers.getTotalPages(), timers.isLast());
        }

        // Map Polls to PollResponses containing vote counts and poll creator details
        List<Long> timerIds = timers.map(Timer::getId).getContent();
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(timerIds);
        Map<Long, Long> timerUserVoteMap = getPollUserVoteMap(currentUser, timerIds);

        List<TimerResponse> timerResponses = timers.map(timer -> {
            return ModelMapper.mapTimerToTimerResponse(timer, user);
        }).getContent();

        return new PagedResponse<>(timerResponses, timers.getNumber(),
        		timers.getSize(), timers.getTotalElements(), timers.getTotalPages(), timers.isLast());
    }

    public Timer createTimer(TimerRequest timerRequest) {
        Timer timer = new Timer();
        timer.setTitle(timerRequest.getTitle());
        timer.setStartDateTime(timerRequest.getStartDateTime());
        timer.setEndDateTime(timerRequest.getEndDateTime());

        return timerRepository.save(timer);
    }

    public TimerResponse getTimerById(Long timerId, UserPrincipal currentUser) {
        Timer timer = timerRepository.findById(timerId).orElseThrow(
                () -> new ResourceNotFoundException("Timer", "id", timerId));

        // Retrieve poll creator details
        User creator = userRepository.findById(timer.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", timer.getCreatedBy()));

        return ModelMapper.mapTimerToTimerResponse(timer, creator);
    }

    /*public PollResponse castVoteAndGetUpdatedPoll(Long pollId, VoteRequest voteRequest, UserPrincipal currentUser) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));

        if(poll.getExpirationDateTime().isBefore(Instant.now())) {
            throw new BadRequestException("Sorry! This Poll has already expired");
        }

        User user = userRepository.getOne(currentUser.getId());

        Choice selectedChoice = poll.getChoices().stream()
                .filter(choice -> choice.getId().equals(voteRequest.getChoiceId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Choice", "id", voteRequest.getChoiceId()));

        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setUser(user);
        vote.setChoice(selectedChoice);

        try {
            vote = voteRepository.save(vote);
        } catch (DataIntegrityViolationException ex) {
            logger.info("User {} has already voted in Poll {}", currentUser.getId(), pollId);
            throw new BadRequestException("Sorry! You have already cast your vote in this poll");
        }

        //-- Vote Saved, Return the updated Poll Response now --

        // Retrieve Vote Counts of every choice belonging to the current poll
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdGroupByChoiceId(pollId);

        Map<Long, Long> choiceVotesMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        // Retrieve poll creator details
        User creator = userRepository.findById(poll.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", poll.getCreatedBy()));

        return ModelMapper.mapPollToPollResponse(poll, choiceVotesMap, creator, vote.getChoice().getId());
    }*/


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

    Map<Long, User> getTimerCreatorMap(List<Timer> timers) {
        // Get Poll Creator details of the given list of polls
        List<Long> creatorIds = timers.stream()
                .map(Timer::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        List<User> creators = userRepository.findByIdIn(creatorIds);
        Map<Long, User> creatorMap = creators.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return creatorMap;
    }
}
