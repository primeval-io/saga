package io.primeval.saga.templating;

public final class Template {

    public final String name;

    public final String engine;

    public final ClassLoader owner;

    public Template(String name, String engine, ClassLoader owner) {
        this.name = name;
        this.engine = engine;
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "Template [name=" + name + ", engine=" + engine + ", owner=" + owner + "]";
    }

}
