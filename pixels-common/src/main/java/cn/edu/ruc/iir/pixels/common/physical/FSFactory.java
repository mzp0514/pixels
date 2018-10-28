/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.edu.ruc.iir.pixels.common.physical;

import cn.edu.ruc.iir.pixels.common.exception.FSException;
import com.facebook.presto.spi.HostAddress;
import com.google.common.collect.ImmutableList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.client.HdfsDataInputStream;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class FSFactory
{
    private static Map<String, FSFactory> instances = new HashMap<>();

    public static FSFactory Instance (String hdfsConfigDir) throws FSException
    {
        if (instances.containsKey(hdfsConfigDir))
        {
            return instances.get(hdfsConfigDir);
        }

        FSFactory instance = new FSFactory(hdfsConfigDir);
        instances.put(hdfsConfigDir, instance);

        return instance;
    }

    private FileSystem fileSystem;

    private FSFactory(String hdfsConfigDir) throws FSException
    {
        Configuration hdfsConfig = new Configuration(false);
        File configDir = new File(hdfsConfigDir);
        hdfsConfig.set("fs.hdfs.impl", DistributedFileSystem.class.getName());
        hdfsConfig.set("fs.file.impl", LocalFileSystem.class.getName());
        try
        {
            if (configDir.exists() && configDir.isDirectory())
            {
                File[] hdfsConfigFiles = configDir.listFiles((file, s) -> s.endsWith("core-site.xml") || s.endsWith("hdfs-site.xml"));
                if (hdfsConfigFiles != null && hdfsConfigFiles.length == 2)
                {
                    hdfsConfig.addResource(hdfsConfigFiles[0].toURI().toURL());
                    hdfsConfig.addResource(hdfsConfigFiles[1].toURI().toURL());
                }
            } else
            {
                throw new FSException("can not read hdfs configuration file in pixels connector. hdfs.config.dir=" + hdfsConfigDir);
            }
            this.fileSystem = FileSystem.get(hdfsConfig);
        } catch (IOException e)
        {
            throw new FSException("I/O error occurs when reading HDFS config files.", e);
        }
    }

    public Optional<FileSystem> getFileSystem()
    {
        return Optional.of(this.fileSystem);
    }

    public List<Path> listFiles(Path dirPath) throws FSException
    {
        List<Path> files = new ArrayList<>();
        FileStatus[] fileStatuses = null;
        try
        {
            fileStatuses = this.fileSystem.listStatus(dirPath);
            if (fileStatuses != null)
            {
                for (FileStatus f : fileStatuses)
                {
                    if (f.isFile())
                    {
                        files.add(f.getPath());
                    }
                }
            }
        } catch (IOException e)
        {
            throw new FSException("error occurs when listing files.", e);
        }

        return files;
    }

    public List<Path> listFiles(String dirPath) throws FSException
    {
        return listFiles(new Path(dirPath));
    }

    /**
     * we assume that a file contains only one block.
     * @param file
     * @param start
     * @param len
     * @return
     * @throws FSException
     */
    public List<HostAddress> getBlockLocations(Path file, long start, long len) throws FSException
    {
//        KeyValue keyValue = EtcdUtil.getEtcdKey(file.toString());
//        if (keyValue != null)
//        {
//            ImmutableList.Builder<HostAddress> builder = ImmutableList.builder();
//            builder.add(HostAddress.fromString(keyValue.getValue().toStringUtf8()));
//            return builder.build();
//        }
//        if(file != null)
//        {
//            ImmutableList.Builder<HostAddress> builder = ImmutableList.builder();
//            builder.add(HostAddress.fromString("dbiir02"));
//            builder.add(HostAddress.fromString("dbiir03"));
//            builder.add(HostAddress.fromString("dbiir04"));
//            builder.add(HostAddress.fromString("dbiir05"));
//            builder.add(HostAddress.fromString("dbiir06"));
//            builder.add(HostAddress.fromString("dbiir07"));
//            builder.add(HostAddress.fromString("dbiir08"));
//            builder.add(HostAddress.fromString("dbiir09"));
//            return builder.build();
//        }
        Set<HostAddress> addresses = new HashSet<>();
        BlockLocation[] locations = new BlockLocation[0];
        try
        {
            locations = this.fileSystem.getFileBlockLocations(file, start, len);
        } catch (IOException e)
        {
            throw new FSException("I/O error occurs when getting block locations", e);
        }
        for (BlockLocation location : locations)
        {
            try
            {
                addresses.addAll(toHostAddress(location.getHosts()));
            } catch (IOException e)
            {
                throw new FSException("I/O error occurs when get hosts from block locations.", e);
            }
        }
        return new ArrayList<>(addresses);
    }

    private List<HostAddress> toHostAddress(String[] hosts)
    {
        ImmutableList.Builder<HostAddress> builder = ImmutableList.builder();
        for (String host : hosts)
        {
            builder.add(HostAddress.fromString(host));
            break;
        }
        return builder.build();
    }

    public List<LocatedBlock> listLocatedBlocks(Path path) throws FSException
    {
        FSDataInputStream in = null;
        try
        {
            in = this.fileSystem.open(path);
        } catch (IOException e)
        {
            throw new FSException("I/O error occurs when opening file.", e);
        }
        HdfsDataInputStream hdis = (HdfsDataInputStream) in;
        List<LocatedBlock> allBlocks = null;
        try
        {
            allBlocks = hdis.getAllBlocks();
        } catch (IOException e)
        {
            throw new FSException("I/O error occurs when getting blocks.", e);
        }
        return allBlocks;
    }

    public List<LocatedBlock> listLocatedBlocks(String path) throws FSException
    {
        return listLocatedBlocks(new Path(path));
    }

}
