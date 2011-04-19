package org.tc.perf.util;

import static org.tc.perf.util.SharedConstants.FILE_SEPARATOR;
import static org.tc.perf.util.SharedConstants.KIT;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;

public class FileLoader {

	private static final Logger log = Logger.getLogger(FileLoader.class);
	private final Ehcache cache;

	public FileLoader(final Ehcache cache) {
		this.cache = cache;
	}

	public void uploadKit(final String kitLocation) throws IOException {
		File file = new File(kitLocation);
		if (!file.canRead())
			log.error("Cant read file : " + file.getAbsolutePath());

		log.info("Uploading kit from " + kitLocation);
		byte[] fileInBytes = getBytesFromFile(file);
		cache.put(new Element(KIT, fileInBytes));
		log.info("Uploaded " + kitLocation + " to cache "
				+ cache.getName());
	}

	public String downloadExtractKit(final String location) {
		log.info("Extracting kit at " + location);
		String kitName = null;
		try {
			byte[] kit = (byte[]) cache.get(KIT).getValue();
			TarInputStream tin = new TarInputStream(new GZIPInputStream(new ByteArrayInputStream(kit)));
			TarEntry tarEntry = tin.getNextEntry();
			while (tarEntry != null){
				File destPath = new File(location + File.separatorChar + tarEntry.getName());
				log.debug(tarEntry.getName());
				if(tarEntry.isDirectory()){
					checkAndCreateDirectory(destPath.getAbsolutePath());
					if (kitName == null) {
						// Kit path should be the first directory under the
						// target location
						File f = new File(tarEntry.getName());
						/*
						 * Loop through the directory hierarchy until we reach
						 * the root directory of the tar file. Tack that onto
						 * the target location.
						 */
						while (f != null) {
							kitName = (new File(location, f.getPath()))
							.getAbsolutePath();
							f = f.getParentFile();
						}
					}
				}
				else {
					FileOutputStream fout = new FileOutputStream(destPath);
					tin.copyEntryContents(fout);
					fout.close();
				}
				tarEntry = tin.getNextEntry();
			}
			tin.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Terracotta kit extracted at " + kitName);
		return kitName;
	}

	/**
	 * Upload a single file to the cache.
	 * 
	 * @param file
	 *            A file to upload
	 * @throws IOException
	 *             Error reading the file.
	 */
	public void uploadSingleFile(final File file) throws IOException {
		if (file.exists() && file.canRead()) {
			byte[] fileInBytes = getBytesFromFile(file);
			cache.put(new Element(file.getName(), fileInBytes));
			log.info("Uploaded " + file.getName() + " to cache "
					+ cache.getName());
		} else {
			throw new IllegalArgumentException("Cannot open file for reading: "
					+ file.getAbsolutePath());
		}
	}

	/**
	 * Download a file from the cache to a specified location
	 * 
	 * Reads a byte array from the cache and write it out to a file at the
	 * specified location.
	 * 
	 * @param filename
	 *            Name of the file to be downloaded (this will be used as the
	 *            key for the cache).
	 * @param destDir
	 *            Destination directory to write the file to.
	 * 
	 * @throws IOException
	 *             If there is an error writing to the target file
	 */
	public void download(final String filename, final String destDir)
	throws IOException {
		Element element = cache.get(filename);
		if(element == null){
			throw new FileNotFoundException(String.format(
					"Cannot find %s in the cache.", filename));
		}
		byte[] file = (byte[]) element.getValue();

		File dir = new File(destDir);
		checkAndCreateDirectory(dir.getAbsolutePath());
		FileOutputStream fos = new FileOutputStream(dir.getAbsolutePath() + "/"
				+ filename);
		fos.write(file);
		fos.close();
		log.info(filename + " has been saved to " + dir.getAbsolutePath());

	}

	private static void checkAndCreateDirectory(final String destDir) {
		File dir = new File(destDir);

		if (!dir.isDirectory()) {
			if (dir.mkdirs())
				log.debug("create directories: " + dir.getAbsolutePath());
			else
				log.error("Cannot create local directory : "
						+ dir.getAbsolutePath());
			// TODO: error handling?
		}

	}

	private String[] getFiles(final File dir, final List<Pattern> patterns){
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(final File dir, final String name) {
				for (Pattern p : patterns)
					if (p.matcher(name).matches())
						return Boolean.TRUE;
				return Boolean.FALSE;
			}
		};
		String[] files = dir.list(filter);
		return files;
	}

	/**
	 * Uploads all files matching the specified pattern into the cache
	 * 
	 * Looks through all files in the directory list and collects all files that
	 * match the specified pattern. The selected files are then uploaded to the
	 * cache one by one.
	 * 
	 * @param dirnames
	 *            List of directory paths to search
	 * @param patterns
	 *            String patterns patterns to look for.
	 * 
	 * @return a list of uploaded files
	 * 
	 * @throws IOException
	 *             Error reading any of the files.
	 */
	public List<String> uploadDirectories(final List<File> dirnames,
			final List<Pattern> patterns) throws IOException {
		List<String> fileList = new ArrayList<String>();
		for (File dir : dirnames) {
			String[] files = getFiles(dir,patterns);
			fileList.addAll(Arrays.asList(files));
			if (files == null) {
				log.info("No files found in " + dir);
				continue;
			}
			log.info(String.format("Uploading %s files from directory: %s ",
					files.length, dir.getAbsolutePath()));
			for (String file : files)
				uploadSingleFile(new File(dir, file));
		}
		return fileList;
	}

	/**
	 * Download all specified files.
	 * 
	 * Downloads all the specified files from the cache and writes them out to
	 * the destination directory.
	 * 
	 * @param files
	 *            Names of the files to be downloaded (file names must also be
	 *            keys to the cached file data).
	 * @param destDir
	 *            Target directory where the files will be written.
	 * 
	 * @throws IOException
	 *             Error writing out the file.
	 */
	public void downloadAll(final List<String> files, final String destDir)
	throws IOException {
		for (String file : files) {
			log.info("Downloading " + file);
			download(file, destDir);
		}
	}

	public void gzipAndUpload(final List<File> dirnames,
			final List<Pattern> patterns, final String gzipFileName) {

		File gzip = new File(gzipFileName);
		try {
			log.info(String.format("Gzipping logs into %s ...",gzipFileName));
			TarOutputStream out = new TarOutputStream(new GZIPOutputStream(
					new FileOutputStream(gzip)));
			for (File dir : dirnames) {
				if(!dir.exists()){
					log.warn("log directory doesn't exists. Check name: " + dir.getCanonicalPath());
					continue;
				}
				String[] files = getFiles(dir, patterns);

				for (String file : files) {
					File logFile = new File(dir + FILE_SEPARATOR + file);
					log.info(logFile.getName());
					FileInputStream in = new FileInputStream(logFile);

					TarEntry te = new TarEntry(logFile);
					te.setSize(logFile.length());
					out.putNextEntry(te);
					int count = 0;
					byte[] buf = new byte[1024];
					while ((count = in.read(buf, 0, 1024)) != -1) {
						out.write(buf, 0, count);
					}
					out.closeEntry();
					in.close();
				}
			}
			out.finish();
			out.close();
			log.info("Logs gzipped to " + gzipFileName);
			cache.put(new Element(gzipFileName, getBytesFromFile(gzip)));
			gzip.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static byte[] getBytesFromFile(final File file) throws IOException {
		InputStream is = new FileInputStream(file);

		long length = file.length();
		log.info(file.getAbsoluteFile() + " file size: " + length + " bytes");
		if (length > Integer.MAX_VALUE) {
			log.warn(file.getAbsoluteFile() + " File too large...");
		}

		byte[] bytes = new byte[(int) length];

		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		is.close();
		return bytes;
	}

	public static void main(final String[] args) {
		FileLoader load = new FileLoader(null);
		List<Pattern> patterns = new ArrayList<Pattern>();
		patterns.add(Pattern.compile(".*jar"));

		List<File> dirs = new ArrayList<File>();
		dirs.add(new File("target"));
		dirs.add(new File("target/dependency"));

		load.gzipAndUpload(dirs, patterns, "logs.tar.gz");
	}
}
