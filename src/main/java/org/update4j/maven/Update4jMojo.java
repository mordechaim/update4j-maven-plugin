package org.update4j.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.update4j.Configuration;
import org.update4j.Library;
import org.update4j.binding.ConfigBinding;
import org.update4j.binding.LibraryBinding;
import org.update4j.util.FileUtils;

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

		PrivateKey key = null;
		if (keystore != null) {
			Properties prop = new Properties();
			try (InputStream in = Files.newInputStream(keystore.toPath())) {
				prop.load(in);

				String location = getProperty(prop, "keystore");
				char[] password = prop.getProperty("keystorePassword", "").toCharArray();
				String alias = getProperty(prop, "alias");
				char[] aliasPw = prop.getProperty("aliasPassword", "").toCharArray();

				try {
					File ksFile = new File(location);
					KeyStore ks = KeyStore.getInstance(ksFile, password);

					key = (PrivateKey) ks.getKey(alias, aliasPw);
				} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException
								| ClassCastException e) {
					throw new MojoFailureException("Failed to read keystore: " + location, e);
				}

			} catch (IOException e) {
				throw new MojoFailureException("Failed to load keystore properties files: " + keystore);
			}
		}

		boolean changed = false;
		List<Integer> removes = new ArrayList<>();

		for (int i = 0; i < config.getLibraries().size(); i++) {
			Library lib = config.getLibraries().get(i);
			LibraryBinding lb = configBinding.libraries.get(i);

			if (lib.getPath() == null) {
				getLog().info("Ignored foreign library: " + lb.path);
				continue;
			}

			Path p = base.resolve(config.getBasePath().relativize(lib.getPath()));

			if (Files.notExists(p)) {
				if (removeUnmatched)
					removes.add(i);
				else
					getLog().warn("Could not find library: " + lib.getPath());
			}

			try {
				if (lib.requiresUpdate()) {
					changed = true;

					lb.size = Files.size(p);
					lb.checksum = Long.toHexString(FileUtils.getChecksum(p));

					if (key != null)
						lb.signature = Base64.getEncoder().encodeToString(FileUtils.sign(p, key));
					else
						lb.signature = null;
				}
			} catch (IOException e) {
				throw new MojoFailureException("Error reading library: " + p, e);
			}

		}

		for (int remove : removes) {
			configBinding.libraries.remove(remove);
		}

		if (changed) {
			configBinding.timestamp = Instant.now().toString();
		}

		try (Writer out = Files.newBufferedWriter(configuration.toPath())) {
			configBinding.write(out);
		} catch (IOException e) {
			throw new MojoFailureException("Could not write configuration file: " + configuration, e);
		}
	}

	private String getProperty(Properties prop, String key) throws MojoFailureException {

		String value = prop.getProperty(key);
		if (value == null)
			throw new MojoFailureException("Missing '" + key + "' property in '" + keystore + "'");

		return value;
	}

}
