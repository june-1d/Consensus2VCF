package sus2vcf;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class MergeGntyp {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TreeMap<Integer, GntypLine> data = new TreeMap<Integer, GntypLine>();
		String line = null;
		String chr = null;

		if (args.length < 1) {
			System.err.println("args: frq2 file names");
			System.exit(0);
		}
		for(String filename:args){
			try {
				long time = System.currentTimeMillis();
				
				FileInputStream fis = new FileInputStream(filename);
				InputStream in;
				if(filename.endsWith(".gz")){
					System.err.print("Read:" + filename + ":GZIP:\t");
					BufferedInputStream bis = new BufferedInputStream(fis);
					in = new GZIPInputStream(bis);
				}
				else if(filename.endsWith(".zip")){
					System.err.print("Read:" + filename + ":ZIP:\t");
					BufferedInputStream bis = new BufferedInputStream(fis);
					in = new ZipInputStream(bis);
				}
				else if(filename.endsWith(".bz2")){
					System.err.print("Read:" + filename + ":BZIP2:\t");
					BufferedInputStream bis = new BufferedInputStream(fis);
//					in = new BZip2CompressorInputStream(bis);
					in = new MultiStreamBZip2InputStream(bis);
				}
				else{
					System.err.print("Read:" + filename + ":TEXT:\t");
					in = new BufferedInputStream(fis);
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				
				while ((line = br.readLine()) != null) {
					String[] split_line = line.split("\t");
					chr = split_line[0];
					int pos = Integer.valueOf(split_line[1]);
					if(data.containsKey(pos)){
						data.get(Integer.valueOf(split_line[1])).addData(split_line);
					}
					else{
						GntypLine n_data = new GntypLine(split_line);
						data.put(pos, n_data);
					}
				}

				System.err.println((System.currentTimeMillis() - time)/1000);
				br.close();fis.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.err.print("Output result:\t");
		long time = System.currentTimeMillis();
		Iterator<Integer> it = data.keySet().iterator();
		while(it.hasNext()){
			Integer pos = it.next();
			System.out.println(chr + "\t" + pos + "\t" + data.get(pos));
		}
		System.err.println((System.currentTimeMillis() - time)/1000);
	}


	static public class GntypLine {
		private String ref;
		private int ac;
		private int[] genotype;

		public GntypLine(){
			ref = null;
			ac = 0;
			genotype = new int[14];
		}

		public GntypLine(String[] data){
			ref = null;
			ac = 0;
			genotype = new int[14];
			addData(data);
		}
		
		public void addData(String[] data){
			ref = data[2];
			ac += Integer.parseInt(data[3]);
			for(int i=0; i<14; i++){
				try{
				genotype[i] += Integer.parseInt(data[i+4]);
				}
				catch (Exception e){
					System.err.println(e);
					System.err.println(toString(data));
				}
			}
		}

		private String toString(String[] data) {
			StringBuilder str = new StringBuilder();
			for(int i=0;i<data.length;i++){
				str.append("\t" + data[i]);
			}
			return str.toString();
		}

		public String toString(){
			StringBuilder str = new StringBuilder();
			str.append(ref + "\t" + ac);
			for(int i=0; i<14; i++){
				str.append("\t" + genotype[i]);				
			}
			return str.toString();
		}
	}
	
	/**
	 * Handle multistream BZip2 files.
	 */
	static public class MultiStreamBZip2InputStream extends CompressorInputStream
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