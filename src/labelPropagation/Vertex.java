package labelPropagation;

import java.io.Serializable;

public class Vertex<T> implements Serializable {
	/**
     * 
     */
	private static final long serialVersionUID = 1L;
	private T value;
	private int threadNumber;

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

	}

	@Override
	public boolean equals(Object obj) {
		return this.value.equals(((Vertex<?>) obj).value);
	}

	public T getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	public void setValue(T value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	public int getThreadNumber() {
		return threadNumber;
	}

	public void setThreadNumber(int threadNumber) {
		this.threadNumber = threadNumber;
	}



}
