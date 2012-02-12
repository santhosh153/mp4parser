/*  
 * Copyright 2008 CoreMedia AG, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an AS IS BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

package com.coremedia.iso.boxes;

import com.coremedia.iso.*;
import com.googlecode.mp4parser.ByteBufferByteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import static com.coremedia.iso.boxes.CastUtils.l2i;


/**
 * A common base structure to contain general metadata. See ISO/IEC 14496-12 Ch. 8.44.1.
 */
public class MetaBox extends AbstractContainerBox {
    private int version = 0;
    private int flags = 0;

    public static final String TYPE = "meta";
    private BoxParser boxParser;

    public MetaBox() {
        super(IsoFile.fourCCtoBytes(TYPE));
    }

    @Override
    public long getContentSize() {
        if (isMp4Box()) {
            // it's a fullbox
            return 4 + super.getContentSize();
        } else {
            // it's an apple metabox
            return super.getContentSize();
        }
    }

    @Override
    public long getNumOfBytesToFirstChild() {
        if (isMp4Box()) {
            // it's a fullbox
            return 12;
        } else {
            // it's an apple metabox
            return 8;
        }
    }

    public void parse(ReadableByteChannel in, ByteBuffer header, long size, BoxParser boxParser) throws IOException {
        content = ChannelHelper.readFully(in, size);
        this.boxParser = boxParser;
    }

    @Override
    public void _parseDetails() {
        int pos = content.position();
        content.get(new byte[4]);
        String isHdlr = IsoTypeReader.read4cc(content);
        if ("hdlr".equals(isHdlr)) {
            //  this is apple bullshit - it's NO FULLBOX
            content.position(pos);
            version = -1;
            flags = -1;
        } else {
            content.position(pos);
            version = IsoTypeReader.readUInt8(content);
            flags = IsoTypeReader.readUInt24(content);
        }
        while (content.remaining() >= 8) {
            try {
                boxes.add(boxParser.parseBox(new ByteBufferByteChannel(content), this));
            } catch (IOException e) {
                throw new RuntimeException("Sebastian needs to fix 7518765283");
            }
        }
        if (content.remaining() > 0) {
            throw new RuntimeException("Sebastian needs to fix it 90732r26537");
        }
    }

    @Override
    protected void getContent(WritableByteChannel wbc) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(l2i(getContentSize()));
        if (isMp4Box()) {
            IsoTypeWriter.writeUInt8(bb, version);
            IsoTypeWriter.writeUInt24(bb, flags);
        }
        for (Box boxe : boxes) {
            boxe.getBox(wbc);
        }
    }


    public boolean isMp4Box() {
        return version != -1 && flags != -1;
    }

    public void setMp4Box(boolean mp4) {
        if (mp4) {
            version = 0;
            flags = 0;
        } else {
            version = -1;
            flags = -1;
        }
    }
}
