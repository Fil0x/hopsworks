package io.hops.hopsworks.common.hdfs;

import io.hops.metadata.hdfs.entity.EncodingPolicy;
import io.hops.metadata.hdfs.entity.EncodingStatus;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PrivilegedExceptionAction;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.security.UserGroupInformation;
import io.hops.hopsworks.common.util.Settings;

public class DistributedFileSystemOps {

  private static final Logger logger = Logger.getLogger(
          DistributedFileSystemOps.class.getName());

  private static final long MB = 1024l * 1024l;

  private final DistributedFileSystem dfs;
  private Configuration conf;
  private String hadoopConfDir;

  /**
   * Returns a file system with username access.
   * <p>
   * @param ugi
   * @param conf
   */
  public DistributedFileSystemOps(UserGroupInformation ugi, Configuration conf) {
    this.dfs = getDfs(ugi, conf);
    this.conf = conf;
  }

  private DistributedFileSystem getDfs(UserGroupInformation ugi,
          final Configuration conf) {
    FileSystem fs = null;
    try {
      fs = ugi.doAs(new PrivilegedExceptionAction<FileSystem>() {
        @Override
        public FileSystem run() throws IOException {
          return FileSystem.get(FileSystem.getDefaultUri(conf), conf);
        }
      });
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Unable to initialize FileSystem", ex);
    }
    return (DistributedFileSystem) fs;
  }

  /**
   * Get the contents of the file at the given path.
   * <p/>
   * @param file
   * @return
   * @throws IOException
   */
  public String cat(Path file) throws IOException {
    StringBuilder out = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(dfs.
            open(file)));) {
      String line;
      while ((line = br.readLine()) != null) {
        out.append(line).append("\n");
      }
      return out.toString();
    }
  }

  /**
   * Get the contents of the file at the given path.
   * <p/>
   * @param file
   * @return
   * @throws IOException
   */
  public String cat(String file) throws IOException {
    Path path = new Path(file);
    return cat(path);
  }

  /**
   * Create a new folder on the given path only if the parent folders exist
   * <p/>
   * @param location The path to the new folder, its name included.
   * @param filePermission
   * @return True if successful.
   * <p/>
   * @throws java.io.IOException
   */
  public boolean mkdir(Path location, FsPermission filePermission) throws
          IOException {
    return dfs.mkdir(location, filePermission);
  }

  /**
   * Create a new folder on the given path only if the parent folders exist
   * <p/>
   * @param path
   * @return True if successful.
   * @throws java.io.IOException
   */
  public boolean mkdir(String path) throws
          IOException {
    Path location = new Path(path);
    return dfs.mkdir(location, FsPermission.getDefault());
  }

  /**
   * Create a new directory and its parent directory on the given path.
   * <p/>
   * @param location The path to the new folder, its name included.
   * @param filePermission
   * @return True if successful.
   * <p/>
   * @throws java.io.IOException
   */
  public boolean mkdirs(Path location, FsPermission filePermission) throws
          IOException {
    return dfs.mkdirs(location, filePermission);
  }

  /**
   * Create a new directory and its parent directory on the given path.
   * <p/>
   * @param location The path to the new folder, its name included.
   * @throws java.io.IOException
   */
  public void touchz(Path location) throws IOException {
    dfs.create(location).close();
  }

  /**
   * List the statuses of the files/directories in the given path if the path
   * is a directory.
   *
   * @param location
   * @return FileStatus[]
   * @throws IOException
   */
  public FileStatus[] listStatus(Path location) throws IOException {
    return dfs.listStatus(location);
  }

  /**
   * Returns the status information about the file.
   *
   * @param location
   * @return
   * @throws IOException
   */
  public FileStatus getFileStatus(Path location) throws IOException {
    return dfs.getFileStatus(location);
  }

  /**
   * Delete a file or directory from the file system.
   *
   * @param location The location of file or directory to be removed.
   * @param recursive If true, a directory will be removed with all its
   * children.
   * @return True if the operation is successful.
   * @throws IOException
   */
  public boolean rm(Path location, boolean recursive) throws IOException {
    logger.log(Level.INFO, "Deleting {0} as {1}", new Object[]{location.
      toString(),
      dfs.toString()});
    if (dfs.exists(location)) {
      return dfs.delete(location, recursive);
    }
    return true;
  }

  /**
   * Copy a file from one file system to the other.
   * <p/>
   * @param deleteSource If true, the file at the source path will be deleted
   * after copying.
   * @param source
   * @param destination
   * @throws IOException
   */
  public void copyFromLocal(boolean deleteSource, Path source, Path destination)
          throws IOException {
    dfs.copyFromLocalFile(deleteSource, source, destination);
  }

  /**
   * Copy from HDFS to the local file system.
   * <p/>
   * @param hdfsPath
   * @param localPath
   * @throws IOException
   */
  public void copyToLocal(String hdfsPath, String localPath) throws IOException {
    if (!hdfsPath.startsWith("hdfs:")) {
      hdfsPath = "hdfs://" + hdfsPath;
    }
    if (!localPath.startsWith("file:")) {
      localPath = "file://" + localPath;
    }
    dfs.copyToLocalFile(new Path(hdfsPath), new Path(localPath));
  }

  /**
   * Copy a file from the local path to the HDFS destination.
   * <p/>
   * @param deleteSource If true, deletes the source file after copying.
   * @param src
   * @param destination
   * @throws IOException
   * @throws IllegalArgumentException If the destination path contains an
   * invalid folder name.
   */
  public void copyToHDFSFromLocal(boolean deleteSource, String src,
          String destination)
          throws IOException {
    //Make sure the directories exist
    Path dirs = new Path(Utils.getDirectoryPart(destination));
    mkdirs(dirs, getParentPermission(dirs));
    //Actually copy to HDFS
    Path destp = new Path(destination);
    Path srcp = new Path(src);
    copyFromLocal(deleteSource, srcp, destp);
  }

  /**
   * Move a file in HDFS from one path to another.
   * <p/>
   * @param source
   * @param destination
   * @throws IOException
   */
  public void moveWithinHdfs(Path source, Path destination) throws IOException {
    dfs.rename(source, destination);
  }

  /**
   * Move the file from the source path to the destination path.
   * <p/>
   * @param source
   * @param destination
   * @throws IOException
   * @thows IllegalArgumentException If the destination path contains an invalid
   * folder name.
   */
  public void renameInHdfs(String source, String destination) throws IOException {
    //Check if source and destination are the same
    if (source.equals(destination)) {
      return;
    }
    //If source does not start with hdfs, prepend.
    if (!source.startsWith("hdfs")) {
      source = "hdfs://" + source;
    }

    //Check destination place, create directory.
    String destDir;
    if (!destination.startsWith("hdfs")) {
      destDir = Utils.getDirectoryPart(destination);
      destination = "hdfs://" + destination;
    } else {
      String tmp = destination.substring("hdfs://".length());
      destDir = Utils.getDirectoryPart(tmp);
    }
    Path dest = new Path(destDir);
    if (!dfs.exists(dest)) {
      dfs.mkdirs(dest);
    }
    Path src = new Path(source);
    Path dst = new Path(destination);
    moveWithinHdfs(src, dst);
  }

  /**
   * Check if the path exists in HDFS.
   * <p/>
   * @param path
   * @return
   * @throws IOException
   */
  public boolean exists(String path) throws IOException {
    Path location = new Path(path);
    return dfs.exists(location);
  }

  /**
   * Copy a file within HDFS. Largely taken from Hadoop code.
   * <p/>
   * @param src
   * @param dst
   * @throws IOException
   */
  public void copyInHdfs(Path src, Path dst) throws IOException {
    Path[] srcs = FileUtil.stat2Paths(dfs.globStatus(src), src);
    if (srcs.length > 1 && !dfs.isDirectory(dst)) {
      throw new IOException("When copying multiple files, "
              + "destination should be a directory.");
    }
    for (Path src1 : srcs) {
      FileUtil.copy(dfs, src1, dfs, dst, false, conf);
    }
  }

  /**
   * Creates a file and all parent dirs that does not exist and returns
   * an FSDataOutputStream
   *
   * @param path
   * @return FSDataOutputStream
   * @throws IOException
   */
  public FSDataOutputStream create(String path) throws IOException {
    Path dstPath = new Path(path);
    String dirs = Utils.getDirectoryPart(path);
    Path dirsPath = new Path(dirs);
    if (!exists(dirs)) {
      dfs.mkdirs(dirsPath);
    }
    return dfs.create(dstPath);
  }

  /**
   * Set permission for path.
   * <p>
   * @param path
   * @param permission
   * @throws IOException
   */
  public void setPermission(Path path, FsPermission permission) throws
          IOException {
    dfs.setPermission(path, permission);
  }

  /**
   * Set permission for paths.
   * <p>
   * @param paths
   * @param permission
   * @throws IOException
   */
  public void setPermission(Set<Path> paths, FsPermission permission) throws
          IOException {
    for (Path path : paths) {
      setPermission(path, permission);
    }
  }

  /**
   * Set owner for path.
   * <p>
   * @param path
   * @param username
   * @param groupname
   * @throws IOException
   */
  public void setOwner(Path path, String username, String groupname) throws
          IOException {
    dfs.setOwner(path, username, groupname);
  }

  /**
   *
   * @param src Path to directory we are setting the quota for
   * @param diskspaceQuotaInMB hdfs quota size for disk space
   * @throws IOException
   */
  public void setHdfsSpaceQuotaInMBs(Path src, long diskspaceQuotaInMB) throws
          IOException {
    setHdfsQuota(src, HdfsConstants.QUOTA_DONT_SET, diskspaceQuotaInMB);
  }

  /**
   *
   * @param src
   * @param numberOfFiles
   * @param diskspaceQuotaInMB
   * @throws IOException
   */
  public void setHdfsQuota(Path src, long numberOfFiles, long diskspaceQuotaInMB)
          throws
          IOException {
    dfs.setQuota(src, numberOfFiles, DistributedFileSystemOps.MB
            * diskspaceQuotaInMB);
  }

  /**
   *
   * @param path
   * @return hdfs quota size in GB
   * @throws IOException
   */
  public long getHdfsSpaceQuotaInMbs(Path path) throws IOException {
    return dfs.getContentSummary(path).getSpaceQuota()
            / DistributedFileSystemOps.MB;
  }

  /**
   *
   * @param path
   * @return the number of files allowed to be created
   * @throws IOException
   */
  public long getHdfsNumFilesQuota(Path path) throws IOException {
    return dfs.getContentSummary(path).getQuota();
  }

  /**
   *
   * @param path
   * @return number of bytes stored in this subtree in bytes
   * @throws IOException
   */
  public long getUsedQuotaInMbs(Path path) throws IOException {
    return dfs.getContentSummary(path).getSpaceConsumed()
            / DistributedFileSystemOps.MB;
  }

  public FSDataInputStream open(Path location) throws IOException {
    return this.dfs.open(location);
  }

  public FSDataInputStream open(String location) throws IOException {
    Path path = new Path(location);
    return this.dfs.open(path);
  }

  public Configuration getConf() {
    return conf;
  }

  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  /**
   * Compress a file from the given location
   * <p/>
   * @param p
   * @return
   * @throws IOException
   */
  public boolean compress(String p) throws IOException,
          IllegalStateException {
    Path location = new Path(p);
    //add the erasure coding configuration file
    File erasureCodingConfFile
            = new File(hadoopConfDir, Settings.ERASURE_CODING_CONFIG);
    if (!erasureCodingConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              erasureCodingConfFile);
      throw new IllegalStateException(
              "No erasure coding conf file: " + Settings.ERASURE_CODING_CONFIG);
    }

    this.conf.addResource(new Path(erasureCodingConfFile.getAbsolutePath()));

    DistributedFileSystem localDfs = this.dfs;
    localDfs.setConf(this.conf);

    EncodingPolicy policy = new EncodingPolicy("src", (short) 1);

    String path = location.toUri().getPath();
    localDfs.encodeFile(path, policy);

    EncodingStatus encodingStatus;
    while (!(encodingStatus = localDfs.getEncodingStatus(path)).isEncoded()) {
      try {
        Thread.sleep(1000);
        logger.log(Level.INFO, "ongoing file compression of {0} ", path);
      } catch (InterruptedException e) {
        logger.log(Level.SEVERE, "Wait for encoding thread was interrupted.");
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the parents permission
   * <p>
   * @param path
   * @return
   * @throws IOException
   */
  public FsPermission getParentPermission(Path path) throws IOException {
    Path location = new Path(path.toUri());
    if (dfs.exists(location)) {
      location = location.getParent();
      return dfs.getFileStatus(location).getPermission();
    }
    while (!dfs.exists(location)) {
      location = location.getParent();
    }
    return dfs.getFileStatus(location).getPermission();
  }

  /**
   * Check if the path is a directory.
   * <p/>
   * @param path
   * @return
   */
  public boolean isDir(String path) {
    Path location = new Path(path);
    try {
      return dfs.isDirectory(location);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      return false;
    }
  }

  /**
   * Marks a file/folder in location as metadata enabled
   * <p/>
   * @param location
   * @throws IOException
   */
  public void setMetaEnabled(String location) throws IOException {
    Path path = new Path(location);
    this.dfs.setMetaEnabled(path, true);
  }

  /**
   * Returns the number of blocks of a file in the given path.
   * The path has to resolve to a file.
   * <p/>
   * @param location
   * @return
   * @throws IOException
   */
  public String getFileBlocks(String location) throws IOException {
    Path path = new Path(location);
    if (this.dfs.isFile(path)) {
      FileStatus filestatus = this.dfs.getFileStatus(path);
      //get the size of the file in bytes
      long filesize = filestatus.getLen();
      long noOfBlocks = (long) Math.ceil(filesize / filestatus.getBlockSize());
      logger.log(Level.INFO, "File: {0}, Num of blocks: {1}", new Object[]{path,
        noOfBlocks});
      return "" + noOfBlocks;
    }
    return "-1";
  }

  /**
   * Flush all cache entries related to the specified user.
   *
   * @param user
   * @throws IOException
   */
  public void flushCachedUser(String user) throws IOException {
    dfs.flushCacheUser(user);
  }

  /**
   * Flush all cache entries related to the specified group.
   *
   * @param group
   * @throws IOException
   */
  public void flushCachedGroup(String group) throws IOException {
    dfs.flushCacheGroup(group);
  }

  /**
   * Flush all cache entries related to the specified user and group.
   *
   * @param user
   * @param group
   * @throws IOException
   */
  public void flushCache(String user, String group) throws IOException {
    dfs.flushCache(user, group);
  }

  /**
   * Closes the distributed file system.
   */
  public void close() {
    try {
      dfs.close();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Error while closing file system.", ex);
    }
  }

}
