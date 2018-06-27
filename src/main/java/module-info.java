module org.update4j.maven {
	exports org.update4j.maven;

	requires junit;
	requires maven.plugin.annotations;
	requires maven.plugin.api;
	requires maven.plugin.testing.harness;
	requires org.eclipse.sisu.plexus;
	requires org.update4j;
}