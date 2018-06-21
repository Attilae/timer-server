package hu.erdeiattila.poll.payload;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class ActivityRequest {
	@NotBlank
	@Size(max = 140)
	private String title;

	@Valid
	private List<TimerRequest> timers;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<TimerRequest> getTimers() {
		return timers;
	}

	public void setTimers(List<TimerRequest> timers) {
		this.timers = timers;
	}	
}
