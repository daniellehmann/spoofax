package org.metaborg.spoofax.build.cleardep.builders;

import java.io.IOException;

import org.metaborg.spoofax.build.cleardep.SpoofaxBuilder;
import org.metaborg.spoofax.build.cleardep.SpoofaxBuilder.SpoofaxInput;
import org.strategoxt.imp.metatooling.building.AntForceRefreshScheduler;
import org.strategoxt.imp.metatooling.loading.AntDescriptorLoader;
import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.build.BuildManager;
import org.sugarj.cleardep.build.BuildRequirement;
import org.sugarj.cleardep.buildjava.JavaJar;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

public class SpoofaxDefaultCtree extends SpoofaxBuilder<SpoofaxInput> {

	public static SpoofaxBuilderFactory<SpoofaxInput, SpoofaxDefaultCtree> factory = new SpoofaxBuilderFactory<SpoofaxInput, SpoofaxDefaultCtree>() {
		private static final long serialVersionUID = -6945708860855449389L;

		@Override
		public SpoofaxDefaultCtree makeBuilder(SpoofaxInput input, BuildManager manager) { return new SpoofaxDefaultCtree(input, manager); }
	};
	

	public SpoofaxDefaultCtree(SpoofaxInput input, BuildManager manager) {
		super(input, factory, manager);
	}
	
	@Override
	protected String taskDescription() {
		return null;
	}

	@Override
	protected Path persistentPath() {
		return context.depPath("spoofaxDefault.dep");
	}
	
	@Override
	public void build(CompilationUnit result) throws IOException {
		String sdfmodule = context.props.getOrFail("sdfmodule");
		String strmodule = context.props.getOrFail("strmodule");
		String esvmodule = context.props.getOrFail("esvmodule");
		String metasdfmodule = context.props.getOrFail("metasdfmodule");
		String buildSdfImports = context.props.getOrElse("build.sdf.imports", "");
		Path externaldef = context.props.isDefined("externaldef") ? new AbsolutePath(context.props.get("externaldef")) : null;
		Path externaljar = context.props.isDefined("externaljar") ? new AbsolutePath(context.props.get("externaljar")) : null;
		String externaljarflags = context.props.getOrElse("externaljarflags", "");

		try {
			checkClassPath();
			
			require(Sdf2Table.factory, new Sdf2Table.Input(context, sdfmodule, buildSdfImports, externaldef));
			require(MetaSdf2Table.factory, new MetaSdf2Table.Input(context, metasdfmodule, buildSdfImports, externaldef));
			require(PPGen.factory, input);
			
			RelativePath ppPackInputPath = context.basePath("${syntax}/${sdfmodule}.pp");
			RelativePath ppPackOutputPath = context.basePath("${include}/${sdfmodule}.pp.af");
			require(PPPack.factory, new PPPack.Input(context, ppPackInputPath, ppPackOutputPath, true));
			
			require(StrategoAster.factory, new StrategoAster.Input(context, strmodule));
	
			// This dependency was discovered by cleardep, due to an implicit dependency on 'org.strategoxt.imp.editors.template/lib/editor-common.generated.str'.

			BuildRequirement<?,?,?,?> sdf2Imp = new BuildRequirement<>(Sdf2ImpEclipse.factory, new Sdf2ImpEclipse.Input(context, esvmodule, sdfmodule, buildSdfImports));
			// This dependency was discovered by cleardep, due to an implicit dependency on 'org.strategoxt.imp.editors.template/include/TemplateLang-parenthesize.str'.
			BuildRequirement<?,?,?,?> sdf2Parenthesize = new BuildRequirement<>(Sdf2Parenthesize.factory, new Sdf2Parenthesize.Input(context, sdfmodule, buildSdfImports, externaldef));
	
			require(StrategoCtree.factory,
					new StrategoCtree.Input(
							context,
							sdfmodule, 
							buildSdfImports, 
							strmodule, 
							externaljar, 
							externaljarflags, 
							externaldef,
							new BuildRequirement<?,?,?,?>[] {sdf2Imp, sdf2Parenthesize}));
			
			// This dependency was discovered by cleardep, due to an implicit dependency on 'org.strategoxt.imp.editors.template/editor/java/org/strategoxt/imp/editors/template/strategies/InteropRegisterer.class'.
			BuildRequirement<?,?,?,?> compileJavaCode = new BuildRequirement<>(CompileJavaCode.factory, input);
			require(compileJavaCode);
			
			javaJar(result, strmodule, compileJavaCode);
			
			sdf2impEclipseReload(result);
			
		} finally {
			forceWorkspaceRefresh();
		}
	}

	
	private void checkClassPath() {
		@SuppressWarnings("unused")
		org.strategoxt.imp.generator.sdf2imp c;
	}

	private void javaJar(CompilationUnit result, String strmodule, BuildRequirement<?,?,?,?> compileJavaCode) throws IOException {
		if (!context.isJavaJarEnabled(result))
			return;
		
		Path baseDir = context.basePath("${build}");
		String[] sfiles = context.props.getOrElse("javajar-includes", "org/strategoxt/imp/editors/template/strategies/").split("[\\s]+");
		Path[] files = new Path[sfiles.length];
		for (int i = 0; i < sfiles.length; i++)
			if (AbsolutePath.acceptable(sfiles[i]))
				files[i] = new AbsolutePath(sfiles[i]);
			else
				files[i] = new RelativePath(baseDir, sfiles[i]);
		
		Path jarPath = context.basePath("${include}/" + strmodule + "-java.jar");
		JavaJar.Mode jarMode = FileCommands.exists(jarPath) ? JavaJar.Mode.Update : JavaJar.Mode.Create;
		require(JavaJar.factory, 
				new JavaJar.Input(
						jarMode,
						jarPath, 
						null,
						files, 
						new BuildRequirement<?,?,?,?>[] {compileJavaCode}));
	}

	private void sdf2impEclipseReload(CompilationUnit result) {
		RelativePath packedEsv = context.basePath("${include}/${esvmodule}.packed.esv");
		result.addSourceArtifact(packedEsv);
		AntDescriptorLoader.main(new String[]{packedEsv.getAbsolutePath()});
	}

	protected void forceWorkspaceRefresh() {
		try {
			AntForceRefreshScheduler.main(new String[] {context.baseDir.getAbsolutePath()});
		} catch (Exception e) {
			Log.log.logErr(e.getMessage(), Log.CORE);
		}
	}
}
