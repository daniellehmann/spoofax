package org.metaborg.spoofax.core.stratego.primitive;

import java.util.Set;

import org.metaborg.spoofax.core.stratego.primitive.generic.GenericPrimitiveLibrary;
import org.spoofax.interpreter.library.AbstractPrimitive;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SpoofaxPrimitiveLibrary extends GenericPrimitiveLibrary {
    public static final String name = "SpoofaxLibrary";


    @Inject public SpoofaxPrimitiveLibrary(@Named(name) Set<AbstractPrimitive> primitives) {
        super(primitives, name);
    }
}
