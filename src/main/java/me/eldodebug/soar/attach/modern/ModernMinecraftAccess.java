package me.eldodebug.soar.attach.modern;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModernMinecraftAccess {

    private static final ConcurrentMap<String, Field> FIELDS =
            new ConcurrentHashMap<String, Field>();
    private static final ConcurrentMap<String, Method> METHODS =
            new ConcurrentHashMap<String, Method>();

    private ModernMinecraftAccess() {
    }

    public static Object field(Object owner, String name) throws ReflectiveOperationException {
        return findField(owner.getClass(), name).get(owner);
    }

    public static void setInt(Object owner, String name, int value)
            throws ReflectiveOperationException {
        findField(owner.getClass(), name).setInt(owner, value);
    }

    public static Object invoke(Object owner, String name, Object... arguments)
            throws ReflectiveOperationException {
        Method method = compatibleMethod(owner.getClass(), name, arguments);
        return method.invoke(owner, arguments);
    }

    public static double number(Object owner, String name) throws ReflectiveOperationException {
        return ((Number) invoke(owner, name)).doubleValue();
    }

    public static int integer(Object owner, String name) throws ReflectiveOperationException {
        return ((Number) invoke(owner, name)).intValue();
    }

    public static long windowHandle(Object window) throws ReflectiveOperationException {
        for (String methodName : new String[] {"handle", "getWindow", "getHandle"}) {
            try {
                return ((Number) invoke(window, methodName)).longValue();
            } catch (NoSuchMethodException ignored) {
            }
        }

        for (String fieldName : new String[] {"handle", "window", "windowHandle"}) {
            try {
                return ((Number) findField(window.getClass(), fieldName).get(window)).longValue();
            } catch (NoSuchFieldException ignored) {
            }
        }

        throw new NoSuchMethodException(window.getClass().getName() + ".handle");
    }

    public static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        String key = type.getName() + '#' + name;
        Field cached = FIELDS.get(key);
        if (cached != null) return cached;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                FIELDS.putIfAbsent(key, field);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method compatibleMethod(Class<?> type, String name, Object[] arguments)
            throws NoSuchMethodException {
        StringBuilder keyBuilder = new StringBuilder(type.getName()).append('#').append(name);
        for (Object argument : arguments) {
            keyBuilder.append(':').append(argument == null ? "null" : argument.getClass().getName());
        }
        String key = keyBuilder.toString();
        Method cached = METHODS.get(key);
        if (cached != null) return cached;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (!method.getName().equals(name) || parameters.length != arguments.length) {
                    continue;
                }
                boolean compatible = true;
                for (int index = 0; index < parameters.length; index++) {
                    if (arguments[index] != null
                            && !boxed(parameters[index]).isInstance(arguments[index])) {
                        compatible = false;
                        break;
                    }
                }
                if (compatible) {
                    method.setAccessible(true);
                    METHODS.putIfAbsent(key, method);
                    return method;
                }
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
