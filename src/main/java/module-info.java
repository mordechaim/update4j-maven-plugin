module org.update4j.maven {
	exports org.update4j.maven;

	requires maven.plugin.annotations;
	requires maven.plugin.api;
	requires org.eclipse.sisu.plexus;
	requires org.update4j;
}