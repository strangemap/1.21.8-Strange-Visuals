package ru.strange.client.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    private static final Map<Class<? extends Event>, List<MethodData>> REGISTRY_MAP = new  HashMap<>();


    public static void register(Object object) {
        for (final Method method : object.getClass().getDeclaredMethods()) {
            if (isMethodBad(method)) continue;
            register(method, object);
        }
    }

    public static void unregister(Object object) {
        for (final List<MethodData> dataList : REGISTRY_MAP.values()) {
            dataList.removeIf(data -> data.getSource().equals(object));
        }
        cleanMap(true);
    }

    private static void register(Method method, Object object) {
        try {
            Class<? extends Event> indexClass = (Class<? extends Event>) method.getParameterTypes()[0];
            MethodData data = new MethodData(object, method, method.getAnnotation(EventInit.class).value());

            if (!data.getTarget().isAccessible()) data.getTarget().setAccessible(true);
            if (REGISTRY_MAP.containsKey(indexClass)) {
                if (!REGISTRY_MAP.get(indexClass).contains(data)) {
                    REGISTRY_MAP.get(indexClass).add(data);
                    sortListValue(indexClass);
                }
            } else {
                REGISTRY_MAP.put(indexClass, new CopyOnWriteArrayList<MethodData>() {
                    {
                        add(data);
                    }
                });
            }
            System.out.println("[EventManager] Registered: " + object.getClass().getSimpleName() 
                    + "." + method.getName() + "(" + indexClass.getSimpleName() + ")");
        } catch (Exception e) {
            System.err.println("Failed to register event handler: " + method.getName() + " in " + object.getClass().getName());
            e.printStackTrace();
        }
    }

    public static void cleanMap(boolean onlyEmptyEntries) {
        Iterator<Map.Entry<Class<? extends Event>, List<MethodData>>> mapIterator = REGISTRY_MAP.entrySet().iterator();

        while (mapIterator.hasNext()) {
            if (!onlyEmptyEntries || mapIterator.next().getValue().isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    private static void sortListValue(Class<? extends Event> indexClass) {
        List<MethodData> sortedList = new CopyOnWriteArrayList<>();

        for (final byte priority : Priority.VALUE_ARRAY) {
            for (final MethodData data : REGISTRY_MAP.get(indexClass)) {
                if (data.getPriority() == priority) sortedList.add(data);
            }
        }

        REGISTRY_MAP.put(indexClass, sortedList);
    }

    private static boolean isMethodBad(Method method) {
        return method.getParameterTypes().length != 1 || !method.isAnnotationPresent(EventInit.class);
    }

    private static boolean isMethodBad(Method method, Class<? extends Event> eventClass) {
        return isMethodBad(method) || !method.getParameterTypes()[0].equals(eventClass);
    }

    private static boolean debugLogged = false;
    private static int renderEventCalls = 0;
    
    public static void printRegistryStatus() {
//        System.out.println("[EventManager] === REGISTRY STATUS ===");
//        System.out.println("[EventManager] Total event types: " + REGISTRY_MAP.size());
//        for (var entry : REGISTRY_MAP.entrySet()) {
//            System.out.println("[EventManager]   " + entry.getKey().getSimpleName() + ": " + entry.getValue().size() + " handlers");
//        }
//        System.out.println("[EventManager] ======================");
    }
    
    public static Event call(Event event) {
        List<MethodData> dataList = REGISTRY_MAP.get(event.getClass());


        if (dataList != null) {
            if (event instanceof EventStoppable) {
                EventStoppable stoppable = (EventStoppable) event;
                
                for (final MethodData data : dataList) {
                    invoke(data, event);
                    if (stoppable.isStopped()) break;
                }
            } else for (final MethodData data : dataList) invoke(data, event);
        }

        return event;
    }

    private static void invoke(MethodData data, Event argument) {
        try {
            data.getTarget().invoke(data.getSource(), argument);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            System.err.println("[EventManager] Failed to invoke " + data.getTarget().getName() 
                    + " on " + data.getSource().getClass().getSimpleName() + ": " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            System.err.println("[EventManager] Exception in handler " + data.getTarget().getName() 
                    + " on " + data.getSource().getClass().getSimpleName() + ": " 
                    + (cause != null ? cause.getMessage() : e.getMessage()));
            if (cause != null) {
                cause.printStackTrace();
            }
        }
    }



    private static final class MethodData {
        private final Object source;
        private final Method target;
        private final byte priority;

        public MethodData(Object source, Method target, byte priority){
            this.source = source;
            this.target = target;
            this.priority = priority;
        }

        public Object getSource() {
            return source;
        }

        public Method getTarget() {
            return target;
        }

        public byte getPriority() {
            return priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodData that = (MethodData) o;
            return priority == that.priority && source.equals(that.source) && target.equals(that.target);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(source, target, priority);
        }

    }
}

