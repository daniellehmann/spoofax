package org.metaborg.spoofax.core.stratego;

import java.io.File;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.InterpreterErrorExit;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.InterpreterExit;
import org.spoofax.interpreter.core.UndefinedStrategyException;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.lang.Context;
import org.strategoxt.stratego_aterm.aterm_escape_strings_0_0;
import org.strategoxt.stratego_aterm.pp_aterm_box_0_0;
import org.strategoxt.stratego_gpp.box2text_string_0_1;

import com.google.inject.Inject;

/**
 * Common code for using Stratego transformations in Spoofax.
 */
public class StrategoCommon implements IStrategoCommon {
    private static final ILogger logger = LoggerUtils.logger(StrategoCommon.class);

    private final IStrategoRuntimeService strategoRuntimeService;
    private final ITermFactoryService termFactoryService;


    @Inject public StrategoCommon(IStrategoRuntimeService strategoRuntimeService,
        ITermFactoryService termFactoryService) {
        this.strategoRuntimeService = strategoRuntimeService;
        this.termFactoryService = termFactoryService;
    }


    @Override public @Nullable IStrategoTerm invoke(ILanguageComponent component, IContext context, IStrategoTerm input,
        String strategy) throws MetaborgException {
        final HybridInterpreter runtime = strategoRuntimeService.runtime(component, context, true);
        return invoke(runtime, input, strategy);
    }

    @Override public @Nullable IStrategoTerm invoke(ILanguageImpl impl, IContext context, IStrategoTerm input,
        String strategy) throws MetaborgException {
        for(ILanguageComponent component : impl.components()) {
            if(component.facet(StrategoRuntimeFacet.class) == null) {
                continue;
            }

            final HybridInterpreter runtime = strategoRuntimeService.runtime(component, context, true);
            final IStrategoTerm result = invoke(runtime, input, strategy);
            if(result != null) {
                return result;
            }

        }
        return null;
    }

    @Override public @Nullable IStrategoTerm invoke(HybridInterpreter runtime, IStrategoTerm input, String strategy)
        throws MetaborgException {
        runtime.setCurrent(input);
        try {
            boolean success = runtime.invoke(strategy);
            if(!success) {
                return null;
            }
            return runtime.current();
        } catch(InterpreterException e) {
            handleException(e, runtime, strategy);
            throw new MetaborgException("Invoking Stratego strategy failed unexpectedly", e);
        }
    }

    // @Override public IStrategoTerm invoke(HybridInterpreter runtime, IStrategoTerm input, String strategy,
    // Strategy[] sp, IStrategoTerm... tp) {
    // final SDefT def = runtime.lookupUncifiedSVar(strategy);
    // final Strategy strat = def.getBody();
    // final CallT callT = (CallT) strat;
    //
    // final org.spoofax.interpreter.core.IContext context = runtime.getContext();
    // context.setCurrent(input);
    // boolean success = false;
    // try {
    // success = callT.evaluateWithArgs(context, sp, tp);
    // } catch(InterpreterException e) {
    // throw new RuntimeException("Failed to evaluate strategy " + strategy, e);
    // }
    // return success ? context.current() : null;
    // }

    private void handleException(InterpreterException ex, HybridInterpreter runtime, String strategy) throws MetaborgException {
        String trace = "Stratego trace:\n" + runtime.getCompiledContext().getTraceString();
        try {
            throw ex;
        } catch(InterpreterErrorExit e) {
            final String message;
            final IStrategoTerm term = e.getTerm();
            if(term != null) {
                final String termString;
                final IStrategoString ppTerm = prettyPrint(term);
                if(ppTerm != null) {
                    termString = ppTerm.stringValue();
                } else {
                    termString = term.toString();
                }
                message = logger.format("Invoking Stratego strategy {} failed at term\n\t{}", strategy, termString);
            } else {
                message = logger.format("Invoking Stratego strategy {} failed", strategy);
            }
            throw new MetaborgException(message + "\n" + trace, e);
        } catch(InterpreterExit e) {
            final String message =
                logger.format("Invoking Stratego strategy {} failed with exit code {}", strategy, e.getValue());
            throw new MetaborgException(message + "\n" + trace, e);
        } catch(UndefinedStrategyException e) {
            final String message =
                logger.format("Invoking Stratego strategy {} failed, strategy is undefined", strategy);
            throw new MetaborgException(message + "\n" + trace, e);
        } catch(InterpreterException e) {
            final Throwable cause = e.getCause();
            if(cause != null && cause instanceof InterpreterException) {
                handleException((InterpreterException) cause, runtime, strategy);
            } else {
                throw new MetaborgException("Invoking Stratego strategy failed unexpectedly: " + cause.getMessage() + "\n" + trace, cause);
            }
        }
    }

    @Override public IStrategoString localLocationTerm(File localLocation) {
        final ITermFactory termFactory = termFactoryService.getGeneric();
        final String locationPath = localLocation.getAbsolutePath();
        final IStrategoString locationPathTerm = termFactory.makeString(locationPath);
        return locationPathTerm;
    }

    @Override public IStrategoString localResourceTerm(File localResource, File localLocation) {
        final ITermFactory termFactory = termFactoryService.getGeneric();
        final String resourcePath = localLocation.toURI().relativize(localResource.toURI()).getPath();
        final IStrategoString resourcePathTerm = termFactory.makeString(resourcePath);
        return resourcePathTerm;
    }

    @Override public IStrategoTerm builderInputTerm(IStrategoTerm ast, FileObject resource, FileObject location)
        throws MetaborgException {
        final ITermFactory termFactory = termFactoryService.getGeneric();

        // TODO: support selected node
        final IStrategoTerm node = ast;
        // TODO: support position
        final IStrategoTerm position = termFactory.makeList();

        final String locationURI = location.getName().getURI();
        final IStrategoString locationTerm = termFactory.makeString(locationURI);

        String resourceURI;
        try {
            resourceURI = location.getName().getRelativeName(resource.getName());
        } catch(FileSystemException e) {
            resourceURI = resource.getName().getURI();
        }
        final IStrategoString resourceTerm = termFactory.makeString(resourceURI);

        return termFactory.makeTuple(node, position, ast, resourceTerm, locationTerm);
    }

    @Override public String toString(IStrategoTerm term) {
        if(term instanceof IStrategoString) {
            return ((IStrategoString) term).stringValue();
        } else {
            final IStrategoString pp = prettyPrint(term);
            if(pp != null) {
                return pp.stringValue();
            } else {
                logger.error("Could not pretty print ATerm, falling back to non-pretty printed ATerm");
                return term.toString();
            }
        }
    }

    @Override public IStrategoString prettyPrint(IStrategoTerm term) {
        final Context context = strategoRuntimeService.genericRuntime().getCompiledContext();
        final ITermFactory termFactory = termFactoryService.getGeneric();
        org.strategoxt.stratego_aterm.Main.init(context);
        term = aterm_escape_strings_0_0.instance.invoke(context, term);
        term = pp_aterm_box_0_0.instance.invoke(context, term);
        term = box2text_string_0_1.instance.invoke(context, term, termFactory.makeInt(120));
        return (IStrategoString) term;
    }
}
