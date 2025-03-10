package esign.text.awt.geom.misc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class RenderingHints
        implements Map<Object, Object>, Cloneable {

    public static final Key KEY_ALPHA_INTERPOLATION = new KeyImpl(1);
    public static final Object VALUE_ALPHA_INTERPOLATION_DEFAULT = new KeyValue(KEY_ALPHA_INTERPOLATION);
    public static final Object VALUE_ALPHA_INTERPOLATION_SPEED = new KeyValue(KEY_ALPHA_INTERPOLATION);
    public static final Object VALUE_ALPHA_INTERPOLATION_QUALITY = new KeyValue(KEY_ALPHA_INTERPOLATION);

    public static final Key KEY_ANTIALIASING = new KeyImpl(2);
    public static final Object VALUE_ANTIALIAS_DEFAULT = new KeyValue(KEY_ANTIALIASING);
    public static final Object VALUE_ANTIALIAS_ON = new KeyValue(KEY_ANTIALIASING);
    public static final Object VALUE_ANTIALIAS_OFF = new KeyValue(KEY_ANTIALIASING);

    public static final Key KEY_COLOR_RENDERING = new KeyImpl(3);
    public static final Object VALUE_COLOR_RENDER_DEFAULT = new KeyValue(KEY_COLOR_RENDERING);
    public static final Object VALUE_COLOR_RENDER_SPEED = new KeyValue(KEY_COLOR_RENDERING);
    public static final Object VALUE_COLOR_RENDER_QUALITY = new KeyValue(KEY_COLOR_RENDERING);

    public static final Key KEY_DITHERING = new KeyImpl(4);
    public static final Object VALUE_DITHER_DEFAULT = new KeyValue(KEY_DITHERING);
    public static final Object VALUE_DITHER_DISABLE = new KeyValue(KEY_DITHERING);
    public static final Object VALUE_DITHER_ENABLE = new KeyValue(KEY_DITHERING);

    public static final Key KEY_FRACTIONALMETRICS = new KeyImpl(5);
    public static final Object VALUE_FRACTIONALMETRICS_DEFAULT = new KeyValue(KEY_FRACTIONALMETRICS);
    public static final Object VALUE_FRACTIONALMETRICS_ON = new KeyValue(KEY_FRACTIONALMETRICS);
    public static final Object VALUE_FRACTIONALMETRICS_OFF = new KeyValue(KEY_FRACTIONALMETRICS);

    public static final Key KEY_INTERPOLATION = new KeyImpl(6);
    public static final Object VALUE_INTERPOLATION_BICUBIC = new KeyValue(KEY_INTERPOLATION);
    public static final Object VALUE_INTERPOLATION_BILINEAR = new KeyValue(KEY_INTERPOLATION);
    public static final Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR = new KeyValue(KEY_INTERPOLATION);

    public static final Key KEY_RENDERING = new KeyImpl(7);
    public static final Object VALUE_RENDER_DEFAULT = new KeyValue(KEY_RENDERING);
    public static final Object VALUE_RENDER_SPEED = new KeyValue(KEY_RENDERING);
    public static final Object VALUE_RENDER_QUALITY = new KeyValue(KEY_RENDERING);

    public static final Key KEY_STROKE_CONTROL = new KeyImpl(8);
    public static final Object VALUE_STROKE_DEFAULT = new KeyValue(KEY_STROKE_CONTROL);
    public static final Object VALUE_STROKE_NORMALIZE = new KeyValue(KEY_STROKE_CONTROL);
    public static final Object VALUE_STROKE_PURE = new KeyValue(KEY_STROKE_CONTROL);

    public static final Key KEY_TEXT_ANTIALIASING = new KeyImpl(9);
    public static final Object VALUE_TEXT_ANTIALIAS_DEFAULT = new KeyValue(KEY_TEXT_ANTIALIASING);
    public static final Object VALUE_TEXT_ANTIALIAS_ON = new KeyValue(KEY_TEXT_ANTIALIASING);
    public static final Object VALUE_TEXT_ANTIALIAS_OFF = new KeyValue(KEY_TEXT_ANTIALIASING);

    private HashMap<Object, Object> map = new HashMap<Object, Object>();

    public RenderingHints(Map<Key, ?> map) {
        if (map != null) {
            putAll(map);
        }
    }

    public RenderingHints(Key key, Object value) {
        put(key, value);
    }

    public void add(RenderingHints hints) {
        this.map.putAll(hints.map);
    }

    public Object put(Object key, Object value) {
        if (!((Key) key).isCompatibleValue(value)) {
            throw new IllegalArgumentException();
        }

        return this.map.put(key, value);
    }

    public Object remove(Object key) {
        return this.map.remove(key);
    }

    public Object get(Object key) {
        return this.map.get(key);
    }

    public Set<Object> keySet() {
        return this.map.keySet();
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return this.map.entrySet();
    }

    public void putAll(Map<?, ?> m) {
        if (m instanceof RenderingHints) {
            this.map.putAll(((RenderingHints) m).map);
        } else {
            Set<?> entries = m.entrySet();

            if (entries != null) {
                Iterator<?> it = entries.iterator();
                while (it.hasNext()) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
                    Key key = (Key) entry.getKey();
                    Object val = entry.getValue();
                    put(key, val);
                }
            }
        }
    }

    public Collection<Object> values() {
        return this.map.values();
    }

    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    public boolean containsKey(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }

        return this.map.containsKey(key);
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public void clear() {
        this.map.clear();
    }

    public int size() {
        return this.map.size();
    }

    public boolean equals(Object o) {
        if (!(o instanceof Map)) {
            return false;
        }

        Map<?, ?> m = (Map<?, ?>) o;
        Set<?> keys = keySet();
        if (!keys.equals(m.keySet())) {
            return false;
        }

        Iterator<?> it = keys.iterator();
        while (it.hasNext()) {
            Key key = (Key) it.next();
            Object v1 = get(key);
            Object v2 = m.get(key);
            if ((v1 == null) ? (v2 == null) : v1.equals(v2)) {
                continue;
            }
            return false;
        }

        return true;
    }

    public int hashCode() {
        return this.map.hashCode();
    }

    public Object clone() {
        RenderingHints clone = new RenderingHints(null);
        clone.map = (HashMap<Object, Object>) this.map.clone();
        return clone;
    }

    public String toString() {
        return "RenderingHints[" + this.map.toString() + "]";
    }

    public static abstract class Key {

        private final int key;

        protected Key(int key) {
            this.key = key;
        }

        public final boolean equals(Object o) {
            return (this == o);
        }

        public final int hashCode() {
            return System.identityHashCode(this);
        }

        protected final int intKey() {
            return this.key;
        }

        public abstract boolean isCompatibleValue(Object param1Object);
    }

    private static class KeyImpl
            extends Key {

        protected KeyImpl(int key) {
            super(key);
        }

        public boolean isCompatibleValue(Object val) {
            if (!(val instanceof RenderingHints.KeyValue)) {
                return false;
            }

            return (((RenderingHints.KeyValue) val).key == this);
        }
    }

    private static class KeyValue {

        private final RenderingHints.Key key;

        protected KeyValue(RenderingHints.Key key) {
            this.key = key;
        }
    }
}
