package org.metaborg.spoofax.core.build;

import javax.annotation.Nullable;

import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.resource.IResourceChange;

public class IdentifiedResourceChange {
    public final IResourceChange change;
    public final ILanguage language;
    public final @Nullable ILanguage dialect;


    public IdentifiedResourceChange(IResourceChange change, ILanguage language, @Nullable ILanguage dialect) {
        this.change = change;
        this.language = language;
        this.dialect = dialect;
    }


    @Override public String toString() {
        return change.toString() + " in " + language.toString();
    }
}