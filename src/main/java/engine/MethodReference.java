package engine;

import java.util.Objects;

public class MethodReference {

    private final String className;   // FQCN
    private final String methodName;

    public MethodReference(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }

    @Override
    public String toString() {
        return className + "." + methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MethodReference)) return false;
        MethodReference that = (MethodReference) o;
        return className.equals(that.className)
                && methodName.equals(that.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName);
    }
}