package org.strategoxt.imp.runtime.services.views.outline;

import org.eclipse.imp.language.ILanguageService;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.imp.runtime.EditorState;

/**
 * @author Oskar van Rest
 */
public interface IOutlineService extends ILanguageService {

	void setOutlineRule(String rule);
	
	void setExpandToLevel(int level);
	
	IStrategoTerm getOutline(EditorState editorState);
	
	int getExpandToLevel();
	
}
