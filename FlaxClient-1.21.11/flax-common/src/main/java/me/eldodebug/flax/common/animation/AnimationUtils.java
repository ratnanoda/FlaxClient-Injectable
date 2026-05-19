package me.eldodebug.flax.common.animation;

public final class AnimationUtils {

	private AnimationUtils() {
	}

	public static float calculateCompensation(float target, float current, double delta, long ms) {
		if (ms == 0) {
			return current;
		}

		double diff = target - current;
		if (Math.abs(diff) <= delta) {
			return target;
		}

		return (float) (current + Math.copySign(delta, diff));
	}
}
