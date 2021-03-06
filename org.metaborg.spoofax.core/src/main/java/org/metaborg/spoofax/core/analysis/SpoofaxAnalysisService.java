package org.metaborg.spoofax.core.analysis;

import org.metaborg.core.analysis.AnalysisException;
import org.metaborg.core.analysis.AnalysisService;
import org.metaborg.core.context.IContext;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnitUpdate;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;

/**
 * Typedef class for {@link AnalysisService} with Spoofax interfaces.
 */
public class SpoofaxAnalysisService
    extends AnalysisService<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ISpoofaxAnalyzeUnitUpdate>
    implements ISpoofaxAnalysisService {
    @Override public ISpoofaxAnalyzeResult analyze(ISpoofaxParseUnit input, IContext context) throws AnalysisException {
        return (ISpoofaxAnalyzeResult) super.analyze(input, context);
    }

    @Override public ISpoofaxAnalyzeResults analyzeAll(Iterable<ISpoofaxParseUnit> inputs, IContext context)
        throws AnalysisException {
        return (ISpoofaxAnalyzeResults) super.analyzeAll(inputs, context);
    }
}
