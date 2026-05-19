package me.eldodebug.flax.common.animation;

public class SimpleAnimation {

	private float value;
	private long lastMs;

	public SimpleAnimation() {
		this(0.0F);
	}

	public SimpleAnimation(float value) {
		this.value = value;
		this.lastMs = System.currentTimeMillis();
	}

	public void setAnimation(float target, double speed) {
		long now = System.currentTimeMillis();
		long delta = now - lastMs;
		lastMs = now;

		double deltaValue = 0.0;
		if (speed > 28) {
			speed = 28;
		}
		if (speed != 0.0) {
			deltaValue = Math.abs(target - value) * 0.35f / (10.0 / speed);
		}

		value = AnimationUtils.calculateCompensation(target, value, deltaValue, delta);
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}
}
