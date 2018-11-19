package cn.edu.ruc.iir.pixels.daemon.metadata;

import cn.edu.ruc.iir.pixels.common.metadata.ReqParams;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Column;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Layout;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Schema;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Table;
import cn.edu.ruc.iir.pixels.daemon.metadata.dao.ColumnDao;
import cn.edu.ruc.iir.pixels.daemon.metadata.dao.LayoutDao;
import cn.edu.ruc.iir.pixels.daemon.metadata.dao.SchemaDao;
import cn.edu.ruc.iir.pixels.daemon.metadata.dao.TableDao;
import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * instance of this class should not be reused.
 */
public class MetadataServerHandler extends ChannelInboundHandlerAdapter
{
    private static Logger log = Logger.getLogger(MetadataServerHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
//        if (msg instanceof ReqParams) {
            ReqParams params = (ReqParams) msg;
            // log the received params.
            log.info("request: " + params.toString());

            Object response = this.executeRequest(params);

            //response
            //异步发送应答消息给客户端: 这里并没有把消息直接写入SocketChannel,而是放入发送缓冲数组中
            ChannelFuture future = ctx.writeAndFlush(response);
            // Thread close
            future.addListener(
                    (ChannelFutureListener) channelFuture -> ctx.close()
            );

//        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
    {
        //将发送缓冲区中数据全部写入SocketChannel
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e)
    {
        log.error("error caught in metadata server.", e);
        //释放资源
        ctx.close();
    }

    private Object executeRequest (ReqParams params)
    {
        String res = null;

        Object response = new Object();

        SchemaDao schemaDao = new SchemaDao();
        TableDao tableDao = new TableDao();
        LayoutDao layoutDao = new LayoutDao();
        ColumnDao columnDao = new ColumnDao();

        switch (params.getAction())
        {
            case "getSchemas":
            {
                List<Schema> schemaList = schemaDao.getAll();
                response = schemaList;
                break;
            }
            case "getTables":
            {
                Schema schema = schemaDao.getByName(params.getParam("schemaName"));
                List<Table> tableList = null;
                if(schema != null)
                {
                    tableList = tableDao.getBySchema(schema);
                    response = tableList;
                }
                else {
                    res = "ERROR";
                }
                break;
            }
            case "getLayouts":
            {
                Schema schema = schemaDao.getByName(params.getParam("schemaName"));
                List<Layout> layoutList = null;
                if(schema != null) {
                    Table table = tableDao.getByNameAndSchema(params.getParam("tableName"), schema);
                    if (table != null) {
                        layoutList = layoutDao.getReadableByTable(table, null);
                    }
                }
                if(layoutList != null)
                    response = layoutList;
                else
                    res = "ERROR";
                break;
            }
            case "getLayout":
            {
                Schema schema = schemaDao.getByName(params.getParam("schemaName"));
                List<Layout> layoutList = null;
                if(schema != null) {
                    Table table = tableDao.getByNameAndSchema(params.getParam("tableName"), schema);
                    if (table != null) {
                        layoutList = layoutDao.getReadableByTable(table, params.getParam("version"));
                    }
                }
                if(layoutList != null)
                    response = layoutList;
                else
                    res = "ERROR";
                break;
            }
            case "getColumns":
            {
                Schema schema = schemaDao.getByName(params.getParam("schemaName"));
                List<Column> columnList = null;
                if(schema != null) {
                    Table table = tableDao.getByNameAndSchema(params.getParam("tableName"), schema);
                    if (table != null) {
                        columnList = columnDao.getByTable(table);
                    }
                }
                if(columnList != null)
                    response = columnList;
                else
                    res = "ERROR";
                break;
            }
            case "createSchema":
            {
                Schema schema = new Schema();
                schema.setName(params.getParam("schemaName"));
                schema.setDesc("This schema is created by pixels-daemon");
                if (schemaDao.exists(schema))
                {
                    res = "exists";
                }
                else
                {
                    schemaDao.insert(schema);
                    res = "success";
                }
                break;
            }
            case "dropSchema":
            {
                if (schemaDao.deleteByName(params.getParam("schemaName")))
                {
                    res = "success";
                }
                else
                {
                    res = "no-such";
                }
                break;
            }
            case "createTable":
            {
                Schema schema = schemaDao.getByName(params.getParam("schemaName"));
                Table table = new Table();
                table.setName(params.getParam("tableName"));
                table.setSchema(schema);
                table.setType("user");
                if (tableDao.exists(table))
                {
                    res = "exists";
                }
                else
                {
                    tableDao.insert(table);
                    String columnsJson = params.getParam("columns");
                    List<Column> columns = JSON.parseArray(columnsJson, Column.class);
                    table = tableDao.getByNameAndSchema(table.getName(), schema);
                    if (columns.size() == columnDao.insertBatch(table, columns))
                    {
                        res = "success";
                    }
                    else
                    {
                        tableDao.deleteByNameAndSchema(table.getName(), schema);
                        res = "failed";
                    }
                }
                break;
            }
            case "dropTable":
            {
                Schema schema = schemaDao.getByName(params.getParam("schemaName"));
                if (tableDao.deleteByNameAndSchema(params.getParam("tableName"), schema))
                {
                    res = "success";
                }
                else
                {
                    res = "no-such";
                }
                break;
            }
            case "existTable":
            {
                Schema schema = schemaDao.getByName(params.getParam("schemaName"));
                Table table = new Table();
                table.setId(-1);
                table.setName(params.getParam("tableName"));
                table.setSchema(schema);
                if (tableDao.exists(table))
                {
                    res = "true";
                }
                else
                {
                    res = "false";
                }
                break;
            }
            default:
            {
                res = "default";
                break;
            }
        }

        if(null != res)
        {
            log.info("Server executeRequest" + res);
            response = res;
        }
        return response;
    }
}
