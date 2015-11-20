package org.msyu.javautil.exceptions;

@FunctionalInterface
public interface SupplierWithException<O, X extends Exception> {

    O get() throws X;

}
