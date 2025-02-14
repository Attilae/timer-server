package hu.erdeiattila.poll.payload;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class TimerRequest {

	@NotNull
	@Valid
	private List<TagRequest> tags;
	
	@NotNull
	private Instant startDateTime;	

	@NotNull
	private Instant endDateTime;
	
	@NotNull
	private Long activityId;

	public List<TagRequest> getTags() {
		return tags;
	}

	public void setTags(List<TagRequest> tags) {
		this.tags = tags;
	}
	
	public Instant getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(Instant startDateTime) {
		this.startDateTime = startDateTime;
	}
	
	public Instant getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(Instant endDateTime) {
		this.endDateTime = endDateTime;
	}
	
	public Long getActivityId() {
		return activityId;
	}

	public void setActivityId(Long activityId) {
		this.activityId = activityId;
	}

	/*public PollLength getPollLength() {
		return pollLength;
	}

	public void setPollLength(PollLength pollLength) {
		this.pollLength = pollLength;
	}*/
}
