package org.update4j.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.update4j.Configuration;
import org.update4j.Library;
import org.update4j.binding.ConfigBinding;

@Mojo(name = "sync")
public class Update4jMojo extends AbstractMojo {

	@Parameter(required = true)
	private File configuration;

	@Parameter
	private File basePath;

	@Parameter
	private File keystore;

	//	@Parameter(defaultValue = "false")
	//	private boolean addMissing;

	@Parameter(defaultValue = "false")
	private boolean removeUnmatched;

	//	@Parameter
	//	private List<String> fileFilters;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (configuration == null) {
			throw new MojoFailureException("'configuration' field is missing.");
		}

		Configuration config = null;
		try (Reader in = Files.newBufferedReader(configuration.toPath())) {
			config = Configuration.read(in);
		} catch (IOException e) {
			throw new MojoFailureException("Could not read configuration file: " + configuration, e);
		}

		ConfigBinding configBinding = null;
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			config.write(new OutputStreamWriter(bytes));
			configBinding = ConfigBinding.read(new InputStreamReader(new ByteArrayInputStream(bytes.toByteArray())));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		Path base;
		if (basePath == null) {
			base = config.getBasePath();
		} else {
			base = basePath.toPath();
		}

		for (Library lib : config.getLibraries()) {
			Path p = base.resolve(config.getBasePath().relativize(lib.getPath()));

			if (removeUnmatched && Files.notExists(p)) {
				configBinding.libraries
								.removeIf(libBinding -> Long.parseLong(libBinding.checksum, 16) == lib.getChecksum());
			}
		}
	}

}
