package labelPropagation;

public class Vertex<T> {
    private T value;
    private T label;

    public Vertex() {
        super();
    }

    public Vertex(T value) {
        super();
        this.value = value;
    }


    public Vertex(T value, T label) {
        super();
        this.value = value;
        this.label = label;
    }

    @Override
    public boolean equals(Object obj) {
        return this.value.equals(((Vertex<?>) obj).value);
    }

    public T getLabel() {
        return label;
    }

    public T getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public void setLabel(T label) {
        this.label = label;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Vertex [value=" + value + ", label=" + label + "]";
    }

}
