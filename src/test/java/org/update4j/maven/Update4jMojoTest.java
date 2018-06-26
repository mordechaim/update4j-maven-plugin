package org.update4j.maven;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class Update4jMojoTest extends AbstractMojoTestCase {

	public void test() throws Exception {
		File pom = getTestFile("/pom.xml");
		assertNotNull(pom);
		assertTrue(pom.exists());

		Update4jMojo myMojo =(Update4jMojo) lookupMojo("sync", pom);
		assertNotNull(myMojo);
		myMojo.execute();
	}

}
