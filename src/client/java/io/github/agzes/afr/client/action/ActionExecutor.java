// SPDX-License-Identifier: GPL-3.0-only
package io.github.agzes.afr.client.action;

import io.github.agzes.afr.AFR;
import io.github.agzes.afr.client.model.RpAction;
import io.github.agzes.afr.client.model.RpProfile;
import io.github.agzes.afr.client.model.UserSettings;
import io.github.agzes.afr.client.util.ActionLogger;
import java.util.ArrayList;
import java.util.List;

public class ActionExecutor {

	private record Task(int delay, Runnable action, int rawIndex) {}

	private final List<Task> tasks = new ArrayList<>();

	private String actionName = "";
	private String status = "idle";
	private int step;
	private int total;
	private int index;
	private int wait;
	private int resumeIndex;
	private boolean running;
	private long lastStepAt;

	public boolean isRunning() {
		return running;
	}

	public String actionName() {
		return actionName;
	}

	public String status() {
		return status;
	}

	public int step() {
		return step;
	}

	public int total() {
		return total;
	}

	public int resumeIndex() {
		return resumeIndex;
	}

	public float progress() {
		return total == 0 ? 0f : Math.min(1f, (float) step / total);
	}

	public boolean run(
		RpAction action,
		UserSettings settings,
		RpProfile profile
	) {
		return run(action, settings, profile, 0);
	}

	public boolean run(
		RpAction action,
		UserSettings settings,
		RpProfile profile,
		int fromStep
	) {
		if (
			running ||
			action == null ||
			action.steps == null ||
			action.steps.isEmpty()
		) {
			return false;
		}

		tasks.clear();
		int start = Math.max(0, Math.min(fromStep, action.steps.size()));
		int parsed = 0;
		for (int i = start; i < action.steps.size(); i++) {
			List<String> raw = action.steps.get(i);
			StepSpec spec = StepSpec.parse(raw);
			if (spec == null) {
				continue;
			}
			parsed++;
			boolean female =
				spec.gender() && "female".equalsIgnoreCase(settings.gender);
			String rawText = female ? spec.textFemale() : spec.text();
			String text = PlaceholderResolver.resolve(
				rawText,
				settings,
				profile
			);
			int before = ticks(
				(int) Math.round(
					profile.profile_settings.delay(spec.before()) *
						settings.timerMultiplier
				)
			);
			int after = ticks(
				(int) Math.round(
					profile.profile_settings.delay(spec.after()) *
						settings.timerMultiplier
				)
			);
			if (before > 0) {
				tasks.add(new Task(before, null, -1));
			}
			tasks.add(
				new Task(
					0,
					() -> {
						ChatSender.send(text, spec.send());
						step++;
						AFR.LOGGER.debug(
							"AFR: action '{}' step {}/{}",
							actionName,
							step,
							total
						);
					},
					i
				)
			);
			List<Integer> keys = PlaceholderResolver.specialKeys(rawText);
			if (!keys.isEmpty()) {
				tasks.add(
					new Task(
						0,
						() -> {
							for (int key : keys) {
								KeyPresser.press(key);
							}
						},
						i
					)
				);
			}
			if (after > 0) {
				tasks.add(new Task(after, null, -1));
			}
		}

		if (tasks.isEmpty()) {
			return false;
		}

		actionName = action.displayName();
		status = "running";
		step = 0;
		total = parsed;
		index = 0;
		resumeIndex = start;
		wait = tasks.get(0).delay;
		running = true;
		lastStepAt = System.currentTimeMillis();
		return true;
	}

	public void tick() {
		if (!running) {
			return;
		}
		if (wait > 0) {
			wait--;
			return;
		}
		if (index >= tasks.size()) {
			finish("finished");
			return;
		}
		Task task = tasks.get(index++);
		lastStepAt = System.currentTimeMillis();
		if (task.action != null) {
			try {
				task.action.run();
			} catch (Exception error) {
				AFR.LOGGER.error(
					"AFR: step execution failed for '{}'",
					actionName,
					error
				);
				finish("error");
				return;
			}
		}
		if (task.rawIndex >= 0) {
			resumeIndex = task.rawIndex + 1;
		}
		if (index >= tasks.size()) {
			finish("finished");
			return;
		}
		wait = tasks.get(index).delay;
	}

	public void reset() {
		running = false;
		status = "idle";
		actionName = "";
		tasks.clear();
		index = 0;
		wait = 0;
		step = 0;
		total = 0;
		resumeIndex = 0;
	}

	public void watchdog() {
		if (
			running &&
			wait == 0 &&
			System.currentTimeMillis() - lastStepAt > 120_000L
		) {
			AFR.LOGGER.warn(
				"AFR: execution of '{}' hung, forcing reset",
				actionName
			);
			reset();
		}
	}

	public void forceStop() {
		if (running) {
			finish("stopped");
		}
	}

	private void finish(String newStatus) {
		running = false;
		status = newStatus;
		tasks.clear();
		index = 0;
		wait = 0;
		ActionLogger.log(
			"ACTION",
			"finish: " + actionName + " (" + newStatus + ")"
		);
		AFR.LOGGER.debug("AFR: action '{}' {}", actionName, newStatus);
	}

	private static int ticks(int millis) {
		if (millis <= 0) {
			return 0;
		}
		return Math.max(1, millis / 50);
	}
}
