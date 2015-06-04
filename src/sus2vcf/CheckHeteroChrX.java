package sus2vcf;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class CheckHeteroChrX extends Thread{

	private ArrayList<String> filelist;
	private StringBuilder result = new StringBuilder();

	public CheckHeteroChrX(ArrayList<String> filelist){
		this.filelist = filelist;
	}
	
	public void run(){
		for(String filename: filelist){
			long time = System.currentTimeMillis();
			String[] filenames = filename.split("/");
			String fileType;
			
			try{
				filename = filename.replaceAll("chr1", "chrX");
				FileInputStream fis = new FileInputStream(filename);
				InputStream in;
				if(filename.endsWith(".gz")){
					fileType = "GZIP";
					BufferedInputStream bis = new BufferedInputStream(fis);
					in = new GZIPInputStream(bis);
				}
				else if(filename.endsWith(".zip")){
					fileType = "ZIP";
					BufferedInputStream bis = new BufferedInputStream(fis);
					in = new ZipInputStream(bis);
				}
				else if(filename.endsWith(".bz2")){
					fileType = "BZIP2";
					BufferedInputStream bis = new BufferedInputStream(fis);
					in = new MultiStreamBZip2InputStream(bis);
				}
				else{
					fileType = "TEXT";
					in = new BufferedInputStream(fis);
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				int nLines = 0;
				int homo = 0;
				int hetero = 0;
				String line = new String();
				while ((line = br.readLine()) != null) {
					if(line.startsWith("chrX") && !line.contains("INDEL")){
						String[] data = line.split("\t");
						int pos = Integer.parseInt(data[1]);
						float qual = Float.parseFloat(data[5]);
						String alt = data[4];
						if(pos > 2699520 && pos < 154931044 && qual >= 70.0){
							if(!alt.equals(".")){
								String[] genotype = data[9].split(":");
								if(genotype[0].equals("0/1")){
									hetero ++;
								}
								else if(genotype[0].equals("1/1")){
									homo ++;
								}
							}
						}
					}
					nLines ++;
				}

				br.close();fis.close();
				result.append(filenames[1] + "\t" + hetero + "\t" + homo + "\n");

				System.err.println(Thread.currentThread().getName() + ":" + filenames[1] + ":" + filename + ":" + fileType 
						+ ":" + nLines + "lines:" + ((System.currentTimeMillis() - time)/1000) + "sec.");

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public String getValue(){
		return result.toString();
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayList<CheckHeteroChrX> threads = new ArrayList<CheckHeteroChrX>();
		int numThreads = 30;
		
		if (args.length < 1) {
			System.err.println("arg1: consensus file list");
			System.exit(1);
		}

		try {
			FileInputStream fls = new FileInputStream(args[0]);
			BufferedReader flr = new BufferedReader(new InputStreamReader(fls));
			ArrayList<String> filelist = new ArrayList<String>();
			String filename = new String();
			while ((filename = flr.readLine()) != null) {
				filelist.add(filename);
			}
			flr.close();

			int numFiles = filelist.size() / numThreads;

			ArrayList<String> f = new ArrayList<String>();
			for(String fname : filelist){
				f.add(fname);
				if(f.size() >= numFiles){
					CheckHeteroChrX checkHeteroChrX = new CheckHeteroChrX(f);
					checkHeteroChrX.start();
					threads.add(checkHeteroChrX);
					f = new ArrayList<String>();
				}
//				if(threads.size()>=30){
//					for(CheckHeteroChrX thread: threads){
//						thread.join();
//						System.out.println(thread.getValue());
//					}
//					threads = new ArrayList<CheckHeteroChrX>();
//				}
			}
			if(f.size() >= 0){
				CheckHeteroChrX checkHeteroChrX = new CheckHeteroChrX(f);
				checkHeteroChrX.start();
				threads.add(checkHeteroChrX);
			}
			for(CheckHeteroChrX thread: threads){
				thread.join();
				System.out.print(thread.getValue());
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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