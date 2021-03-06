package main;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import spoon.examples.tracing.processing.MemoryLeaksProcessor;
import spoon.examples.tracing.processing.ThreadIntroductionProcessor;
import spoon.processing.AbstractProcessor;
import spoon.processing.Environment;
import spoon.processing.ProcessingManager;
import spoon.reflect.Factory;
import spoon.reflect.declaration.CtSimpleType;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.DefaultCoreFactory;
import spoon.support.JavaOutputProcessor;
import spoon.support.QueueProcessingManager;
import spoon.support.StandardEnvironment;
import fr.irisa.diversify.spoon.processing.PerforableProcessor;
import fr.irisa.diversify.spoon.utils.MyBuilder;

public class Test {
	public static final String srcfolderstatic = "src_to_modify";
	public static final String srcgenfolderstatic = "src_modified";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		List<File> files = new ArrayList<File>();
		files.add(new File(srcfolderstatic));
		
		Test t = new Test(files, new File(srcgenfolderstatic));
	}
	private File srcgenfolder; 

	
	public Test(List<File> srcfolders, File srcgenfolder ) {
		this.srcgenfolder =srcgenfolder;
		this.initSpoon(srcfolders);
	}
	
	protected void initSpoon(List<File> folderToParse) {
		Environment env = new StandardEnvironment();

		env.setVerbose(true);
		env.setDebug(true);
		DefaultCoreFactory f = new DefaultCoreFactory();
		

		Factory factory = new Factory(f, env);

		MyBuilder builder = new MyBuilder(factory);
		
		
		
		try {
			for (File file : folderToParse) {
				try {
					builder.addInputSource(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			
			java.net.URI uri = this.getClass().getClassLoader()
					.getResource("fr/irisa/diversify/spoon/template/MemoryLeaksIntroduction.java").toURI();
			//JarUtils.extractNestedJar(new URL("jar:"+uri.toURL().getFile().substring(0,uri.toURL().getFile().indexOf("!"))+"!/"),new File("/tmp")); 
			
			Path p1 = java.nio.file.Files.createTempDirectory("kevMut");
			JarUtils.extractNestedJar(uri.toURL(),p1.toFile());
			
			uri = this.getClass().getClassLoader()
					.getResource("fr/irisa/diversify/spoon/template/MemoryLeaksIntroduction.class").toURI();
			JarUtils.extractNestedJar(uri.toURL(),p1.toFile());
			
			uri = this.getClass().getClassLoader()
					.getResource("fr/irisa/diversify/spoon/template/ThreadIntroduction.class").toURI();
			JarUtils.extractNestedJar(uri.toURL(),p1.toFile());

			
			uri = this.getClass().getClassLoader()
					.getResource("fr/irisa/diversify/spoon/template/ThreadIntroduction.java").toURI();
			
			
			URL u = JarUtils.extractNestedJar(uri.toURL(),p1.toFile());
			File f1 = new File(u.getFile());
//			CtFolderZip f;


			//System.err.println("toto"  + f1.getParentFile().getAbsolutePath());
			builder.addTemplateSource(f1.getParentFile());
			
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			builder.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		ProcessingManager pm = new QueueProcessingManager(factory);
		PerforableProcessor tp = new PerforableProcessor();
		MemoryLeaksProcessor mp = new MemoryLeaksProcessor();
		ThreadIntroductionProcessor thp = new ThreadIntroductionProcessor();
		
		pm.addProcessor(tp);
		pm.addProcessor(mp);
		pm.addProcessor(thp);
		//TODO Not all;
		TypeReferenceScanner refall = new TypeReferenceScanner();
		refall.scan(Factory.getLauchingFactory().Package().getAllRoots());
		for (CtSimpleType s : Factory.getLauchingFactory().Class().getAll()) {
			String newName = s.getSimpleName() + "Mutant"
					+ Math.abs(new Random().nextInt());
			
			String oldName = s.getQualifiedName();
			s.setSimpleName(newName);
			
			Factory.getLauchingFactory();
			for (CtTypeReference ref : refall.getReferences()) {
				if (ref.getQualifiedName().equals(oldName)){
					
					ref.setSimpleName(newName);
				}
				File file = s.getPosition().getCompilationUnit().getFile();
				File newFile = new File(file.getParent() + File.separatorChar
						+ newName + ".java");
				s.getPosition().getCompilationUnit().setFile(newFile);
			}
			
			
		}

		
		
/*		if (srcgenfolder.exists()) {
			try {
				delete(srcgenfolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}*/
		AbstractProcessor p = new JavaOutputProcessor(srcgenfolder);
		pm.addProcessor(p);
		pm.process();
		factory.getEnvironment().reportEnd();
	}


}
