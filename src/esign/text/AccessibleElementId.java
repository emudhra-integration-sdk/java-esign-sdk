package esign.text;

import java.io.Serializable;

public class AccessibleElementId
        implements Comparable<AccessibleElementId>, Serializable {

    private static int id_counter = 0;
    private int id = 0;

    public AccessibleElementId() {
        this.id = ++id_counter;
    }

    public String toString() {
        return Integer.toString(this.id);
    }

    public int hashCode() {
        return this.id;
    }

    public boolean equals(Object o) {
        return (o instanceof AccessibleElementId && this.id == ((AccessibleElementId) o).id);
    }

    public int compareTo(AccessibleElementId elementId) {
        if (this.id < elementId.id) {
            return -1;
        }
        if (this.id > elementId.id) {
            return 1;
        }
        return 0;
    }
}

