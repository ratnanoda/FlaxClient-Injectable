package me.eldodebug.soar.attach.modern;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class ModernSetting<T> {

    private final String key;
    private final String name;
    private T value;

    protected ModernSetting(String key, String name, T value) {
        this.key = key;
        this.name = name;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public abstract String serialize();

    public abstract void deserialize(String value);

    public static final class BooleanSetting extends ModernSetting<Boolean> {
        public BooleanSetting(String key, String name, boolean value) {
            super(key, name, value);
        }

        @Override
        public String serialize() {
            return Boolean.toString(getValue());
        }

        @Override
        public void deserialize(String value) {
            setValue(Boolean.parseBoolean(value));
        }
    }

    public static final class NumberSetting extends ModernSetting<Double> {
        private final double minimum;
        private final double maximum;

        public NumberSetting(
                String key,
                String name,
                double value,
                double minimum,
                double maximum) {
            super(key, name, value);
            this.minimum = minimum;
            this.maximum = maximum;
        }

        public double getMinimum() {
            return minimum;
        }

        public double getMaximum() {
            return maximum;
        }

        @Override
        public String serialize() {
            return Double.toString(getValue());
        }

        @Override
        public void deserialize(String value) {
            try {
                double parsed = Double.parseDouble(value);
                setValue(Math.max(minimum, Math.min(maximum, parsed)));
            } catch (NumberFormatException ignored) {
                // Keep the default value when a config entry is malformed.
            }
        }
    }

    public static final class ComboSetting extends ModernSetting<String> {
        private final List<String> options;

        public ComboSetting(String key, String name, String value, String... options) {
            super(key, name, value);
            this.options = Collections.unmodifiableList(Arrays.asList(options));
        }

        public List<String> getOptions() {
            return options;
        }

        public void next() {
            int index = options.indexOf(getValue());
            setValue(options.get((index + 1 + options.size()) % options.size()));
        }

        @Override
        public String serialize() {
            return getValue();
        }

        @Override
        public void deserialize(String value) {
            if (options.contains(value)) setValue(value);
        }
    }

    public static final class ColorSetting extends ModernSetting<Integer> {
        public ColorSetting(String key, String name, int red, int green, int blue) {
            super(key, name, (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF));
        }

        @Override
        public String serialize() {
            return Integer.toHexString(getValue());
        }

        @Override
        public void deserialize(String value) {
            try {
                setValue(Integer.parseInt(value, 16) & 0xFFFFFF);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static final class KeybindSetting extends ModernSetting<Integer> {
        public KeybindSetting(String key, String name, int value) {
            super(key, name, value);
        }

        @Override
        public String serialize() {
            return Integer.toString(getValue());
        }

        @Override
        public void deserialize(String value) {
            try {
                setValue(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
