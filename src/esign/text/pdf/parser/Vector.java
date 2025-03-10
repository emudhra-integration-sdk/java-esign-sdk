package esign.text.pdf.parser;

import java.util.Arrays;

public class Vector {

    public static final int I1 = 0;
    public static final int I2 = 1;
    public static final int I3 = 2;
    private final float[] vals = new float[]{0.0F, 0.0F, 0.0F};

    public Vector(float x, float y, float z) {
        this.vals[0] = x;
        this.vals[1] = y;
        this.vals[2] = z;
    }

    public float get(int index) {
        return this.vals[index];
    }

    public Vector cross(Matrix by) {
        float x = this.vals[0] * by.get(0) + this.vals[1] * by.get(3) + this.vals[2] * by.get(6);
        float y = this.vals[0] * by.get(1) + this.vals[1] * by.get(4) + this.vals[2] * by.get(7);
        float z = this.vals[0] * by.get(2) + this.vals[1] * by.get(5) + this.vals[2] * by.get(8);

        return new Vector(x, y, z);
    }

    public Vector subtract(Vector v) {
        float x = this.vals[0] - v.vals[0];
        float y = this.vals[1] - v.vals[1];
        float z = this.vals[2] - v.vals[2];

        return new Vector(x, y, z);
    }

    public Vector cross(Vector with) {
        float x = this.vals[1] * with.vals[2] - this.vals[2] * with.vals[1];
        float y = this.vals[2] * with.vals[0] - this.vals[0] * with.vals[2];
        float z = this.vals[0] * with.vals[1] - this.vals[1] * with.vals[0];

        return new Vector(x, y, z);
    }

    public Vector normalize() {
        float l = length();
        float x = this.vals[0] / l;
        float y = this.vals[1] / l;
        float z = this.vals[2] / l;
        return new Vector(x, y, z);
    }

    public Vector multiply(float by) {
        float x = this.vals[0] * by;
        float y = this.vals[1] * by;
        float z = this.vals[2] * by;
        return new Vector(x, y, z);
    }

    public float dot(Vector with) {
        return this.vals[0] * with.vals[0] + this.vals[1] * with.vals[1] + this.vals[2] * with.vals[2];
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public float lengthSquared() {
        return this.vals[0] * this.vals[0] + this.vals[1] * this.vals[1] + this.vals[2] * this.vals[2];
    }

    public String toString() {
        return this.vals[0] + "," + this.vals[1] + "," + this.vals[2];
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + Arrays.hashCode(this.vals);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Vector other = (Vector) obj;
        if (!Arrays.equals(this.vals, other.vals)) {
            return false;
        }
        return true;
    }
}
