/* Copyright (c) 2001-2009, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package org.hsqldb.persist;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.hsqldb.Database;
import org.hsqldb.error.Error;
import org.hsqldb.error.ErrorCode;

/**
 * @author Fred Toussi (fredt@users dot sourceforge.net)
 * @version 1.9.0
 * @since 1.9.0
 */
public class LobStoreInJar implements LobStore {

    int             lobBlockSize = 1024 * 32;
    Database        database;
    DataInputStream file;
    final String    fileName;

    //
    long realPosition;

    public LobStoreInJar(Database database, int lobBlockSize) {

        this.lobBlockSize = lobBlockSize;
        this.database     = database;

        try {
            fileName = database.getPath() + ".lobs";
        } catch (Throwable t) {
            throw Error.error(ErrorCode.DATA_FILE_ERROR, t);
        }
    }

    public byte[] getBlockBytes(int blockAddress, int blockCount) {

        if (file == null) {
            throw Error.error(ErrorCode.FILE_IO_ERROR);
        }

        try {
            long   address   = (long) blockAddress * lobBlockSize;
            int    count     = blockCount * lobBlockSize;
            byte[] dataBytes = new byte[count];

            fileSeek(address);
            file.readFully(dataBytes, 0, count);

            realPosition = address + count;

            return dataBytes;
        } catch (Throwable t) {
            throw Error.error(ErrorCode.DATA_FILE_ERROR, t);
        }
    }

    public void setBlockBytes(byte[] dataBytes, int blockAddress,
                              int blockCount) {}

    public void close() {

        try {
            if (file != null) {
                file.close();
            }
        } catch (Throwable t) {
            throw Error.error(ErrorCode.DATA_FILE_ERROR, t);
        }
    }

    private void resetStream() throws IOException {

        if (file != null) {
            file.close();
        }

        InputStream fis = getClass().getResourceAsStream(fileName);

        if (fis == null) {
            return;
        }

        file         = new DataInputStream(fis);
        realPosition = 0;
    }

    private void fileSeek(long position) throws IOException {

        if (file == null) {
            resetStream();
        }

        long skipPosition = realPosition;

        if (position < skipPosition) {
            resetStream();

            skipPosition = 0;
        }

        while (position > skipPosition) {
            skipPosition += file.skip(position - skipPosition);
        }

        realPosition = position;
    }
}
