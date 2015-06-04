package sus2vcf;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class AddGntypData {
	public HashMap <String, Gntyp> exACData = new HashMap <String, Gntyp>();
	public HashMap <String, Gntyp> ctrlData = new HashMap <String, Gntyp>();

	public AddGntypData(String exACFileName, String ctrlFileName) throws IOException{
		// read ExAC file
		System.err.println("Read ExAC File : " + exACFileName);
		FileInputStream fis = new FileInputStream(exACFileName);
		InputStream in = getInputStream(exACFileName, fis);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String line = null;
		while ((line = br.readLine()) != null) {
			Gntyp data = new Gntyp(line);
			exACData.put(data.getPos(), data);
		}
		br.close();fis.close();

		// read control file
		System.err.println("Read Control File : " + ctrlFileName);
		fis = new FileInputStream(ctrlFileName);
		in = getInputStream(ctrlFileName, fis);
		br = new BufferedReader(new InputStreamReader(in));

		line = null;
		while ((line = br.readLine()) != null) {
			Gntyp data = new Gntyp(line);
			ctrlData.put(data.getPos(), data);
		}
		br.close();fis.close();
	}
	static private InputStream getInputStream(String filename, FileInputStream fis) throws IOException{
		InputStream in;
		if(filename.endsWith(".gz")){
//			System.err.print("Read:" + filename + ":GZIP:\t");
			BufferedInputStream bis = new BufferedInputStream(fis);
			in = new GZIPInputStream(bis);
		}
		else if(filename.endsWith(".zip")){
//			System.err.print("Read:" + filename + ":ZIP:\t");
			BufferedInputStream bis = new BufferedInputStream(fis);
			in = new ZipInputStream(bis);
		}
		else if(filename.endsWith(".bz2")){
//			System.err.print("Read:" + filename + ":BZIP2:\t");
			BufferedInputStream bis = new BufferedInputStream(fis);
			in = new MultiStreamBZip2InputStream(bis);
		}
		else{
//			System.err.print("Read:" + filename + ":TEXT:\t");
			in = new BufferedInputStream(fis);
		}
		return in;
	}
	
	public HashMap <String, Gntyp> getExACData(){
		return exACData;
	}
	public HashMap <String, Gntyp> getCtrlData(){
		return ctrlData;
	}

	/**
	 * @param args targetFileName ExACFileName ExACColumns(0:ALL,1:EAS,2:EUR) CTRLFileName CTRLColumns(0)
	 */
	public static void main(String[] args) {
		if (args.length < 4) {
			System.err.println("args: <target file> <ExAC file> <ExAC column(0:ALL,1:EAS,2:EUR)> <CTRL file> <CTRL column(0)>");
			System.exit(1);
		}
		String targetFileName = args[0];
		String exACFileName = args[1];
		int[] exACColumns = parseInts(args[2].split(","));
		String ctrlFileName = args[3];
		int[] ctrlColumns = parseInts(args[4].split(","));

		System.err.println(targetFileName + "\t" + exACFileName + "\t" + args[2] + "\t" + ctrlFileName + "\t" + args[4]);
		
		try {
			AddGntypData exacCtrlData = new AddGntypData(exACFileName, ctrlFileName);

			// read target data
			System.err.println("Read Target File : " + exACFileName);
			FileInputStream fis;
			fis = new FileInputStream(targetFileName);
			InputStream in = getInputStream(targetFileName, fis);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String line = null;
			while ((line = br.readLine()) != null) {
				Gntyp data = new Gntyp(line);
				System.out.print(data.getHeader() + "\t" + data.getGntyp(0));
				for(int column : exACColumns){
					if(exacCtrlData.exACData.containsKey(data.getPos())){
						System.out.print("\t" + exacCtrlData.exACData.get(data.getPos()).getGntyp(column));
					}
					else{
						System.out.print("\tno_entry");
					}
				}
				for(int column : ctrlColumns){
					if(exacCtrlData.ctrlData.containsKey(data.getPos())){
						System.out.print("\t" + exacCtrlData.ctrlData.get(data.getPos()).getGntyp(column));
					}
					else{
						System.out.print("\tno_entry");
					}
				}
				System.out.println("");
			}
			br.close();fis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static int[] parseInts(String[] str) {
		int[] data = new int[str.length];
		for(int i = 0; i < str.length; i++){
			data[i] = Integer.parseInt(str[i]);
		}
		return data;
	}

	static private class Gntyp{
		String pos;
		String ref;
		String gntyp[];

		private Gntyp(String line) {
			String[] data = line.split("\t");
			int nDatas = (data.length - 3) / 2;
			gntyp = new String[nDatas];
			
			pos = data[0] + "\t" + data[1];
			ref = data[2];

			int count = 0;
			for(int i = 3; i < data.length ; i += 2){
				String[] datas = data[i+1].split(",");
				StringBuilder str = new StringBuilder();
				String sep = "";
				for(int j = 0; j < 10; j++){
					str.append(sep + datas[j]);
					sep = ",";
				}
				gntyp[count++] = str.toString();
			}
		}
		private String getPos() {
			return pos;
		}

		private String getGntyp(int i) {
			return gntyp[i];
		}

		private String getHeader(){
			return pos + "\t" + ref;
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