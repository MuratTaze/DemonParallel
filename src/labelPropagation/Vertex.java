package labelPropagation;

public class Vertex<T> {
    private T value;

    public Vertex() {
	super();
	// TODO Auto-generated constructor stub
    }

    public Vertex(T value) {
	super();
	this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
	this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
	return this.value.equals(((Vertex<?>)obj).value);
    }

    @Override
    public int hashCode() {
	return value.hashCode();
    }

    @Override
    public String toString() {
	return "Vertex [value=" + value + "]";
    }

}
