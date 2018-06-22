package cn.edu.ruc.iir.pixels.core.compactor;

import cn.edu.ruc.iir.pixels.common.exception.MetadataException;
import cn.edu.ruc.iir.pixels.common.metadata.MetadataService;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Compact;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Layout;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Order;
import cn.edu.ruc.iir.pixels.core.PixelsReader;
import cn.edu.ruc.iir.pixels.core.PixelsReaderImpl;
import cn.edu.ruc.iir.pixels.core.TestParams;
import cn.edu.ruc.iir.pixels.core.TypeDescription;
import cn.edu.ruc.iir.pixels.core.reader.PixelsReaderOption;
import cn.edu.ruc.iir.pixels.core.reader.PixelsRecordReader;
import cn.edu.ruc.iir.pixels.core.vector.BytesColumnVector;
import cn.edu.ruc.iir.pixels.core.vector.LongColumnVector;
import cn.edu.ruc.iir.pixels.core.vector.VectorizedRowBatch;
import com.alibaba.fastjson.JSON;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class TestPixelsCompactor
{
    @Test
    public void testBasicCompact ()
    {
        String filePath = TestParams.filePath;
        Configuration conf = new Configuration();
        conf.set("fs.hdfs.impl", DistributedFileSystem.class.getName());
        conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());

        try {
            FileSystem fs = FileSystem.get(URI.create(filePath), conf);
            TypeDescription schema = TypeDescription.fromString(TestParams.schemaStr);

            CompactLayout layout = new CompactLayout(3, 2);
            layout.addColumnlet(0, 1);
            layout.addColumnlet(1, 1);
            layout.addColumnlet(0, 0);
            layout.addColumnlet(1, 0);
            layout.addColumnlet(2, 1);
            layout.addColumnlet(2, 0);

            PixelsCompactor pixelsCompactor =
                    PixelsCompactor.newBuilder()
                            .setSchema(schema)
                            .setSourcePaths(TestParams.sourcePaths)
                            .setCompactLayout(layout)
                            .setFS(fs)
                            .setFilePath(new Path(filePath))
                            .setBlockSize(1024*1024*1024)
                            .setReplication((short) 1)
                            .setBlockPadding(false)
                            .build();

            pixelsCompactor.compact();
            pixelsCompactor.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRealCompact () throws MetadataException, IOException
    {
        MetadataService metadataService = new MetadataService("presto00", 18888);
        List<Layout> layouts = metadataService.getLayouts("pixels", "testnull_pixels");
        System.out.println(layouts.size());
        Layout layout = layouts.get(0);
        String orderJson = layout.getOrder();
        String compactJson = layout.getCompact();
        Order order = JSON.parseObject(orderJson, Order.class);
        Compact compact = JSON.parseObject(compactJson, Compact.class);

        String filePath = "hdfs://presto00:9000/pixels/testNull_pixels/201806190954180.pxl";
        Path path = new Path(filePath);
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(filePath), conf);
        /*
        PixelsReader pixelsReader = PixelsReaderImpl.newBuilder()
                .setFS(fs)
                .setPath(path)
                .build();
        List<String> fieldNames = pixelsReader.getFileSchema().getFieldNames();
        Map<String, Integer> nameToIdxMap = new HashMap<>();
        for (int i = 0; i < fieldNames.size(); ++i)
        {
            nameToIdxMap.put(fieldNames.get(i), i);
        }

        //TypeDescription schema = pixelsReader.getFileSchema();
*/
        FileStatus[] statuses = fs.listStatus(new Path("hdfs://presto00:9000/pixels/pixels/testnull_pixels/v_0_order"));
        List<Path> sourcePaths = new ArrayList<>();
        for (int i = 16; i < 32; ++i)
        {
            System.out.println(statuses[i].getPath().toString());
            sourcePaths.add(statuses[i].getPath());
        }

        CompactLayout compactLayout = new CompactLayout(
                compact.getNumRowGroupInBlock(), compact.getNumColumn());
        for (String columnletStr : compact.getColumnletOrder())
        {
            String[] splits = columnletStr.split(":");
            //int rowGroupId = Integer.parseInt(splits[0]);
            //int orderId = Integer.parseInt(splits[1]);
            //int columnId = nameToIdxMap.get(order.getColumnOrder().get(orderId));
            int rowGroupId = Integer.parseInt(splits[0]);
            int columnId = Integer.parseInt(splits[1]);
            compactLayout.addColumnlet(rowGroupId, columnId);
        }

        long start = System.currentTimeMillis();

        PixelsCompactor pixelsCompactor =
                PixelsCompactor.newBuilder()
                        .setSourcePaths(sourcePaths)
                        .setCompactLayout(compactLayout)
                        .setFS(fs)
                        .setFilePath(new Path("hdfs://presto00:9000/pixels/pixels/testnull_pixels/v_0_compact/compact.2.pxl"))
                        .setBlockSize(2l*1024*1024*1024)
                        .setReplication((short) 1)
                        .setBlockPadding(false)
                        .build();

        pixelsCompactor.compact();
        pixelsCompactor.close();

        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    public void testContent () throws IOException
    {
        String filePath = "hdfs://presto00:9000/pixels/pixels/testnull_pixels/compact.3.pxl";
        //String filePath = "hdfs://presto00:9000/pixels/testNull_pixels/201806190954180.pxl";
        Path path = new Path(filePath);
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(filePath), conf);
        PixelsReader reader = PixelsReaderImpl.newBuilder()
                .setFS(fs)
                .setPath(path)
                .build();

        PixelsReaderOption option = new PixelsReaderOption();
        String[] cols = {"Domain", "SamplePercent"};
        option.skipCorruptRecords(true);
        option.tolerantSchemaEvolution(true);
        option.includeCols(cols);

        VectorizedRowBatch rowBatch;
        PixelsRecordReader recordReader = reader.read(option);

        while (true)
        {
            rowBatch = recordReader.readBatch(1000);
            LongColumnVector acv = (LongColumnVector) rowBatch.cols[1];
            BytesColumnVector zcv = (BytesColumnVector) rowBatch.cols[0];
            for (int i = 0; i < acv.vector.length; ++i)
            {
                System.out.println(acv.vector[i]);
            }
            if (rowBatch.endOfFile)
            {
                break;
            }

        }
    }
}
