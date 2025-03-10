package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class IntHashtable
        implements Cloneable {

    private transient Entry[] table;
    private transient int count;
    private int threshold;
    private float loadFactor;

    public IntHashtable() {
        this(150, 0.75F);
    }

    public IntHashtable(int initialCapacity) {
        this(initialCapacity, 0.75F);
    }

    public IntHashtable(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("illegal.capacity.1", initialCapacity));
        }
        if (loadFactor <= 0.0F) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("illegal.load.1", new Object[]{String.valueOf(loadFactor)}));
        }
        if (initialCapacity == 0) {
            initialCapacity = 1;
        }
        this.loadFactor = loadFactor;
        this.table = new Entry[initialCapacity];
        this.threshold = (int) (initialCapacity * loadFactor);
    }

    public int size() {
        return this.count;
    }

    public boolean isEmpty() {
        return (this.count == 0);
    }

    public boolean contains(int value) {
        Entry[] tab = this.table;
        for (int i = tab.length; i-- > 0;) {
            for (Entry e = tab[i]; e != null; e = e.next) {
                if (e.value == value) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsValue(int value) {
        return contains(value);
    }

    public boolean containsKey(int key) {
        Entry[] tab = this.table;
        int hash = key;
        int index = (hash & Integer.MAX_VALUE) % tab.length;
        for (Entry e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash && e.key == key) {
                return true;
            }
        }
        return false;
    }

    public int get(int key) {
        Entry[] tab = this.table;
        int hash = key;
        int index = (hash & Integer.MAX_VALUE) % tab.length;
        for (Entry e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash && e.key == key) {
                return e.value;
            }
        }
        return 0;
    }

    protected void rehash() {
        int oldCapacity = this.table.length;
        Entry[] oldMap = this.table;

        int newCapacity = oldCapacity * 2 + 1;
        Entry[] newMap = new Entry[newCapacity];

        this.threshold = (int) (newCapacity * this.loadFactor);
        this.table = newMap;

        for (int i = oldCapacity; i-- > 0;) {
            for (Entry old = oldMap[i]; old != null;) {
                Entry e = old;
                old = old.next;

                int index = (e.hash & Integer.MAX_VALUE) % newCapacity;
                e.next = newMap[index];
                newMap[index] = e;
            }
        }
    }

    public int put(int key, int value) {
        Entry[] tab = this.table;
        int hash = key;
        int index = (hash & Integer.MAX_VALUE) % tab.length;
        Entry e;
        for (e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash && e.key == key) {
                int old = e.value;

                e.value = value;
                return old;
            }
        }

        if (this.count >= this.threshold) {

            rehash();

            tab = this.table;
            index = (hash & Integer.MAX_VALUE) % tab.length;
        }

        e = new Entry(hash, key, value, tab[index]);
        tab[index] = e;
        this.count++;
        return 0;
    }

    public int remove(int key) {
        Entry[] tab = this.table;
        int hash = key;
        int index = (hash & Integer.MAX_VALUE) % tab.length;
        for (Entry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
            if (e.hash == hash && e.key == key) {
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                this.count--;
                int oldValue = e.value;
                e.value = 0;
                return oldValue;
            }
        }
        return 0;
    }

    public void clear() {
        Entry[] tab = this.table;
        for (int index = tab.length; --index >= 0;) {
            tab[index] = null;
        }
        this.count = 0;
    }

    static class Entry {

        int hash;

        int key;

        int value;

        Entry next;

        protected Entry(int hash, int key, int value, Entry next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public int getKey() {
            return this.key;
        }

        public int getValue() {
            return this.value;
        }

        protected Object clone() {
            Entry entry = new Entry(this.hash, this.key, this.value, (this.next != null) ? (Entry) this.next.clone() : null);
            return entry;
        }
    }

    static class IntHashtableIterator
            implements Iterator<Entry> {

        int index;
        IntHashtable.Entry[] table;
        IntHashtable.Entry entry;

        IntHashtableIterator(IntHashtable.Entry[] table) {
            this.table = table;
            this.index = table.length;
        }

        public boolean hasNext() {
            if (this.entry != null) {
                return true;
            }
            while (this.index-- > 0) {
                if ((this.entry = this.table[this.index]) != null) {
                    return true;
                }
            }
            return false;
        }

        public IntHashtable.Entry next() {
            if (this.entry == null) {
                while (this.index-- > 0 && (this.entry = this.table[this.index]) == null);
            }
            if (this.entry != null) {
                IntHashtable.Entry e = this.entry;
                this.entry = e.next;
                return e;
            }
            throw new NoSuchElementException(MessageLocalization.getComposedMessage("inthashtableiterator", new Object[0]));
        }

        public void remove() {
            throw new UnsupportedOperationException(MessageLocalization.getComposedMessage("remove.not.supported", new Object[0]));
        }
    }

    public Iterator<Entry> getEntryIterator() {
        return new IntHashtableIterator(this.table);
    }

    public int[] toOrderedKeys() {
        int[] res = getKeys();
        Arrays.sort(res);
        return res;
    }

    public int[] getKeys() {
        int[] res = new int[this.count];
        int ptr = 0;
        int index = this.table.length;
        Entry entry = null;
        while (true) {
            if (entry == null) {
                while (index-- > 0 && (entry = this.table[index]) == null);
            }
            if (entry == null) {
                break;
            }
            Entry e = entry;
            entry = e.next;
            res[ptr++] = e.key;
        }
        return res;
    }

    public int getOneKey() {
        if (this.count == 0) {
            return 0;
        }
        int index = this.table.length;
        Entry entry = null;
        while (index-- > 0 && (entry = this.table[index]) == null);
        if (entry == null) {
            return 0;
        }
        return entry.key;
    }

    public Object clone() {
        try {
            IntHashtable t = (IntHashtable) super.clone();
            t.table = new Entry[this.table.length];
            for (int i = this.table.length; i-- > 0;) {
                t.table[i] = (this.table[i] != null) ? (Entry) this.table[i]
                        .clone() : null;
            }
            return t;
        } catch (CloneNotSupportedException e) {

            throw new InternalError();
        }
    }
}
