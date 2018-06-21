package hu.erdeiattila.poll.util;

import hu.erdeiattila.poll.model.Activity;
import hu.erdeiattila.poll.model.Poll;
import hu.erdeiattila.poll.model.Timer;
import hu.erdeiattila.poll.model.User;
import hu.erdeiattila.poll.payload.ActivityResponse;
import hu.erdeiattila.poll.payload.ChoiceResponse;
import hu.erdeiattila.poll.payload.PollResponse;
import hu.erdeiattila.poll.payload.TagResponse;
import hu.erdeiattila.poll.payload.TimerResponse;
import hu.erdeiattila.poll.payload.UserSummary;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelMapper {
	
	public static PollResponse mapPollToPollResponse(Poll poll, Map<Long, Long> choiceVotesMap, User creator, Long userVote) {
        PollResponse pollResponse = new PollResponse();
        pollResponse.setId(poll.getId());
        pollResponse.setQuestion(poll.getQuestion());
        pollResponse.setCreationDateTime(poll.getCreatedAt());
        pollResponse.setExpirationDateTime(poll.getExpirationDateTime());
        Instant now = Instant.now();
        pollResponse.setExpired(poll.getExpirationDateTime().isBefore(now));

        List<ChoiceResponse> choiceResponses = poll.getChoices().stream().map(choice -> {
            ChoiceResponse choiceResponse = new ChoiceResponse();
            choiceResponse.setId(choice.getId());
            choiceResponse.setText(choice.getText());

            if(choiceVotesMap.containsKey(choice.getId())) {
                choiceResponse.setVoteCount(choiceVotesMap.get(choice.getId()));
            } else {
                choiceResponse.setVoteCount(0);
            }
            return choiceResponse;
        }).collect(Collectors.toList());

        pollResponse.setChoices(choiceResponses);
        UserSummary creatorSummary = new UserSummary(creator.getId(), creator.getUsername(), creator.getName());
        pollResponse.setCreatedBy(creatorSummary);

        if(userVote != null) {
            pollResponse.setSelectedChoice(userVote);
        }

        long totalVotes = pollResponse.getChoices().stream().mapToLong(ChoiceResponse::getVoteCount).sum();
        pollResponse.setTotalVotes(totalVotes);

        return pollResponse;
    }

    public static TimerResponse mapTimerToTimerResponse(Timer timer, User creator) {
    	TimerResponse timerResponse = new TimerResponse();
    	timerResponse.setId(timer.getId());
    	timerResponse.setTitle(timer.getTitle());
    	timerResponse.setCreationDateTime(timer.getCreatedAt());
    	timerResponse.setStartDateTime(timer.getStartDateTime());
    	timerResponse.setEndDateTime(timer.getEndDateTime());

        List<TagResponse> tagResponses = timer.getTags().stream().map(tag -> {
            TagResponse tagResponse = new TagResponse();
            tagResponse.setId(tag.getId());
            tagResponse.setText(tag.getText());
            return tagResponse;
        }).collect(Collectors.toList());

        timerResponse.setTags(tagResponses);
        UserSummary creatorSummary = new UserSummary(creator.getId(), creator.getUsername(), creator.getName());
        timerResponse.setCreatedBy(creatorSummary);

        return timerResponse;
    }
    
    public static ActivityResponse mapActivityToActivityResponse(Activity activity, User creator) {
    	ActivityResponse activityResponse = new ActivityResponse();
    	activityResponse.setId(activity.getId());
    	activityResponse.setTitle(activity.getTitle());
    	activityResponse.setCreationDateTime(activity.getCreatedAt());

        List<TimerResponse> timerResponses = activity.getTimers().stream().map(timer -> {
            TimerResponse timerResponse = new TimerResponse();
            timerResponse.setId(timer.getId());
            timerResponse.setTitle(timer.getTitle());
            return timerResponse;
        }).collect(Collectors.toList());

        activityResponse.setTimers(timerResponses);
        UserSummary creatorSummary = new UserSummary(creator.getId(), creator.getUsername(), creator.getName());
        activityResponse.setCreatedBy(creatorSummary);

        return activityResponse;
    }
}