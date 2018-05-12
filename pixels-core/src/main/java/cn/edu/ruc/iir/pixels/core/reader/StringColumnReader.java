package cn.edu.ruc.iir.pixels.core.reader;

import cn.edu.ruc.iir.pixels.core.PixelsProto;
import cn.edu.ruc.iir.pixels.core.TypeDescription;
import cn.edu.ruc.iir.pixels.core.encoding.RunLenIntDecoder;
import cn.edu.ruc.iir.pixels.core.utils.DynamicIntArray;
import cn.edu.ruc.iir.pixels.core.vector.ColumnVector;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;

import java.io.IOException;

/**
 * pixels
 *
 * @author guodong
 */
public class StringColumnReader
        extends ColumnReader
{
    private int originsOffset;
    private int startsOffset;
    private ByteBuf inputBuffer = null;
    private ByteBuf originsBuf = null;
    private int[] orders = null;
    private int[] starts = null;
    private ByteBuf contentBuf = null;
    private RunLenIntDecoder lensDecoder = null;
    private RunLenIntDecoder contentDecoder = null;

    StringColumnReader(TypeDescription type)
    {
        super(type);
    }

    /**
     * Read values from input buffer.
     *
     * @param input    input buffer
     * @param encoding encoding type
     * @param size     number of values to read
     * @param vector   vector to read into
     * @throws IOException
     */
    @Override
    public void read(byte[] input, PixelsProto.ColumnEncoding encoding,
                     int offset, int size, int pixelStride, ColumnVector vector) throws IOException
    {
        if (offset == 0) {
            if (inputBuffer != null) {
                inputBuffer.release();
            }
            inputBuffer = Unpooled.wrappedBuffer(input);
            readContent(input, encoding);
        }
        // if dictionary encoded
        if (encoding.getKind().equals(PixelsProto.ColumnEncoding.Kind.DICTIONARY)) {
            // read original bytes
            for (int i = 0; i < size; i++) {
                int originId = orders[(int) contentDecoder.next()];
                int tmpLen;
                if (originId < starts.length - 1) {
                    tmpLen = starts[originId + 1] - starts[originId];
                }
                else {
                    tmpLen = startsOffset - originsOffset - starts[originId];
                }
                byte[] tmpBytes = new byte[tmpLen];
                originsBuf.getBytes(starts[originId], tmpBytes);
                vector.add(tmpBytes);
            }
        }
        // if un-encoded
        else {
            // read values
            for (int i = 0; i < size; i++) {
                int len = (int) lensDecoder.next();
                byte[] tmpBytes = new byte[len];
                contentBuf.readBytes(tmpBytes);
                vector.add(tmpBytes);
            }
        }
    }

    private void readContent(byte[] input, PixelsProto.ColumnEncoding encoding)
            throws IOException
    {
        if (encoding.getKind().equals(PixelsProto.ColumnEncoding.Kind.DICTIONARY)) {
            // read offsets
            inputBuffer.markReaderIndex();
            inputBuffer.skipBytes(input.length - 3 * Integer.BYTES);
            originsOffset = inputBuffer.readInt();
            startsOffset = inputBuffer.readInt();
            int ordersOffset = inputBuffer.readInt();
            inputBuffer.resetReaderIndex();
            // read buffers
            contentBuf = inputBuffer.slice(0, originsOffset);
            originsBuf = inputBuffer.slice(originsOffset, startsOffset - originsOffset);
            ByteBuf startsBuf = inputBuffer.slice(startsOffset, ordersOffset - startsOffset);
            ByteBuf ordersBuf = inputBuffer.slice(ordersOffset, input.length - ordersOffset);
            int originNum = 0;
            DynamicIntArray startsArray = new DynamicIntArray();
            RunLenIntDecoder startsDecoder = new RunLenIntDecoder(new ByteBufInputStream(startsBuf), false);
            while (startsDecoder.hasNext()) {
                startsArray.add((int) startsDecoder.next());
                originNum++;
            }
            // read starts and orders
            RunLenIntDecoder ordersDecoder = new RunLenIntDecoder(new ByteBufInputStream(ordersBuf), false);
            starts = new int[originNum];
            orders = new int[originNum];
            for (int i = 0; i < originNum && ordersDecoder.hasNext(); i++) {
                starts[i] = startsArray.get(i);
                orders[i] = (int) ordersDecoder.next();
            }
            contentDecoder = new RunLenIntDecoder(new ByteBufInputStream(contentBuf), false);
        }
        else {
            // read lens field offset
            inputBuffer.markReaderIndex();
            inputBuffer.skipBytes(input.length - Integer.BYTES);
            int lensOffset = inputBuffer.readInt();
            inputBuffer.resetReaderIndex();
            // read strings
            contentBuf = inputBuffer.slice(0, lensOffset);
            // read lens field
            ByteBuf lensBuf = inputBuffer.slice(lensOffset, input.length - Integer.BYTES - lensOffset);
            lensDecoder = new RunLenIntDecoder(new ByteBufInputStream(lensBuf), false);
        }
    }
}