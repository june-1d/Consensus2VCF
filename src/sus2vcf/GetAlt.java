package sus2vcf;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class GetAlt {
	private String chr;
	private long pos;
	private String ref;
	private int ac;
	private int[] genotype; // AA, AC, AG, AT, CC, CG, CT, GG, GT, TT, A, C, G, T

	public GetAlt(String data){
		String[] datas = data.split("\t");
		this.chr = datas[0];
		this.pos = Long.parseLong(datas[1]);
		this.ref = datas[2];
		this.ac = Integer.parseInt(datas[3]);
		this.genotype = new int[datas.length - 4];
		for(int i=0; i<datas.length-4; i++){
			this.genotype[i] = Integer.parseInt(datas[i + 4]);
		}
	}

	public boolean checkAlt(){
		for(int i=0;i<genotype.length;i++){
			if(i != homo1() && i != homo2() && genotype[i] > 0)return true;
		}
		return false;
	}

	private int homo1() {
		if(ref.equals("A")){
			return 0;
		}
		else if(ref.equals("C")){
			return 4;
		}
		else if(ref.equals("G")){
			return 7;
		}
		else if(ref.equals("T")){
			return 9;
		}
		return 9999;
	}
	private int homo2() {
		if(ref.equals("A")){
			return 10;
		}
		else if(ref.equals("C")){
			return 11;
		}
		else if(ref.equals("G")){
			return 12;
		}
		else if(ref.equals("T")){
			return 13;
		}
		return 9999;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(chr + "\t" + pos + "\t" + ref + "\t" + ac + "\t");
		String sep = "";
		for(int i=0;i<genotype.length;i++){
			sb.append(sep + genotype[i]);
			sep = ",";
		}
		return sb.toString();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String line = null;

		if (args.length < 1) {
			System.err.println("args: .gntyp file names");
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
					in = new MultiStreamBZip2InputStream(bis);
				}
				else{
					System.err.print("Read:" + filename + ":TEXT:\t");
					in = new BufferedInputStream(fis);
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				
				while ((line = br.readLine()) != null) {
					GetAlt data = new GetAlt(line);
					if(data.checkAlt()){
						System.out.println(data);
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