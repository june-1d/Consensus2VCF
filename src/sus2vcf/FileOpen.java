package sus2vcf;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class FileOpen{
	FileInputStream fis;
	BufferedReader br;

	public FileOpen(String filename) throws IOException{
		fis = new FileInputStream(filename);
		InputStream in;
		if(filename.endsWith(".gz")){
			System.err.println("Read:" + filename + ":GZIP");
			BufferedInputStream bis = new BufferedInputStream(fis);
			in = new GZIPInputStream(bis);
		}
		else if(filename.endsWith(".zip")){
			System.err.println("Read:" + filename + ":ZIP");
			BufferedInputStream bis = new BufferedInputStream(fis);
			in = new ZipInputStream(bis);
		}
		else if(filename.endsWith(".bz2")){
			System.err.println("Read:" + filename + ":BZIP2");
			BufferedInputStream bis = new BufferedInputStream(fis);
			in = new MultiStreamBZip2InputStream(bis);
		}
		else{
			System.err.println("Read:" + filename + ":TEXT");
			in = new BufferedInputStream(fis);
		}
		br = new BufferedReader(new InputStreamReader(in));
	}

	public BufferedReader getBufferedReader(){
		return br;
	}
	
	public void close() throws IOException{
		fis.close();
		br.close();
	}

	/**
	 * Handle multistream BZip2 files.
	 */
	public class MultiStreamBZip2InputStream extends CompressorInputStream
	{
		private InputStream fInputStream;
		private BZip2CompressorInputStream fBZip2;

		public MultiStreamBZip2InputStream(InputStream in) throws IOException
		{
			fInputStream = in;
			fBZip2 = new BZip2CompressorInputStream(in);
		}

		@Override
		public int read() throws IOException
		{
			int ch = fBZip2.read();
			if (ch == -1) {
				/*
				 * If this is a multistream file, there will be more data that
				 * follows that is a valid compressor input stream. Restart the
				 * decompressor engine on the new segment of the data.
				 */
				if (fInputStream.available() > 0) {
					// Make use of the fact that if we hit EOF, the data for
					// the old compressor was deleted already, so we don't need
					// to close.
					fBZip2 = new BZip2CompressorInputStream(fInputStream);
					ch = fBZip2.read();
				}
			}
			return ch;
		}

		/**
		 * Read the data from read(). This makes sure we funnel through read so
		 * we can do our multistream magic.
		 */
		public int read(byte[] dest, int off, int len) throws IOException
		{
			if ((off < 0) || (len < 0) || (off + len > dest.length)) {
				throw new IndexOutOfBoundsException();
			}

			int i = 1;
			int c = read();
			if (c == -1) return -1;
			dest[off++] = (byte)c;
			while (i < len) {
				c = read();
				if (c == -1) break;
				dest[off++] = (byte)c;
				++i;
			}
			return i;
		}

		public void close() throws IOException
		{
			fBZip2.close();
			fInputStream.close();
		}
	}
}