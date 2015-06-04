package sus2vcf;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class ExAC2Gntyp {
	private String chr;
	private long pos;
	private String ref;
	private String[] alt;
	private int[] altFlag;
	private HashMap<String, String> info;

	public ExAC2Gntyp(String line){
		String[] data = line.split("\t");
		this.chr = data[0];
		this.pos = Long.parseLong(data[1]);
		this.ref = data[3];
		this.alt = data[4].split(",");
		this.altFlag = getAltFlag(this.alt);
		this.info = new HashMap<String, String>();
		for(String d : data[7].split(";")){
			if(d.contains("=")){
				String[] value = d.split("=");
				this.info.put(value[0], value[1]);
			}
		}
	}

	/*
	=========================================================
	Population independent annotation (also included in VCF header):

	Adjusted Alt Allele Counts (DP >= 10 & GQ >= 20)
	##INFO=<ID=AC_Adj,Number=A,Type=Integer,Description="Adjusted Allele Counts">
	AC_Adj <= AC

	Number of Heterozygous Individuals (DP >= 10 & GQ >= 20)
	##INFO=<ID=AC_Het,Number=A,Type=Integer,Description="Adjusted Heterozygous Counts">

	Number of Homozygous Alt Allele Individuals (DP >= 10 & GQ >= 20)
	##INFO=<ID=AC_Hom,Number=A,Type=Integer,Description="Adjusted Homozygous Counts">

	For chr1-22:
	sum(AC_Adj) = sum(AC_Het) + 2*sum(AC_Hom) 

	=========================================================
	Adjustments made on chrX and chrY

	Number of Hemizygous Alt Allele Individuals (DP >= 10 & GQ >= 20) Note: ONLY appears on chrX (non-PAR) and chrY
	##INFO=<ID=AC_Hemi,Number=A,Type=Integer,Description="Adjusted Hemizygous Counts">

	AC_Hemi is a count Male alt alleles, where each site (excluding PAR) only has one allele.
	AC_Hom is a count of Female alt alleles
	AC_Het is a count of Female alt alleles

	For chrX (non-PAR)
	sum(AC_Adj) = sum(AC_Hemi) + sum(AC_Het) + 2*sum(AC_Hom) 
	AN_Adj (and all population AN on chrX) = 2*n_Female + n_Male

	For chrY
	sum(AC_Adj) = sum(AC_Hemi) 
	AN_Adj (and all population AN on chrY) = n_Male

	Pseudoautosomal regions (PARs) were taken from http://genome.ucsc.edu/cgi-bin/hgGateway
	X:60001-2699520
	X:154931044-155260560

	===========================================================
	 */

	private String getGenotype(String AN, String AC, String AC_Hom, String AC_Het, String AC_Hemi){
		int[] genotype = new int[15]; // AA, AC, AG, AT, CC, CG, CT, GG, GT, TT, A, C, G, T, other(Allel Count)
		int refCode = getBaseCode(ref);
		int refCount = Integer.parseInt(AN);

		// calculate alternative homo count
		if(!AC_Hom.equals("")){
			int[] homo = parseInts(AC_Hom.split(","));
			for(int i = 0; i < altFlag.length; i++){
				genotype[baseCode2index(altFlag[i] * 2)] = homo[i];
				refCount -= homo[i] * 2;
			}
		}
		
		// calculate hetero count
		if(!AC_Het.equals("")){
			int[] hetero = parseInts(AC_Het.split(","));
			int count = 0;
			for(int i = 0; i < altFlag.length; i++){
				for(int j = 0; j < altFlag.length - i; j++){
					int baseCode = altFlag[j];
					if(i == 0){
						baseCode += refCode;
					}
					else{
						baseCode += altFlag[i + j];
					}
					genotype[baseCode2index(baseCode)] = hetero[count];
					refCount -= hetero[count++] * 2;
				}
			}
		}

		// calculate alternative hemi count
		if(!AC_Hemi.equals("")){
			int[] hemi = parseInts(AC_Hemi.split(","));
			for(int i = 0; i < altFlag.length; i++){
				genotype[baseCode2index(altFlag[i])] = hemi[i];
				refCount -= hemi[i];
			}
		}
		// calculate reference homo count
		genotype[baseCode2index(refCode * 2)] = refCount / 2;

		String sep = "";
		StringBuilder sb = new StringBuilder(AN + "\t");
		for(int i = 0; i < genotype.length - 1; i++){
			sb.append(sep + genotype[i]);
			sep = ",";
		}
		return sb.toString();
	}

	public String getGntypAll(){
		return getGenotype(getValue(info, "AN_Adj"), getValue(info, "AC_Adj"), getValue(info, "AC_Hom"), 
				getValue(info, "AC_Het"), getValue(info, "AC_Hemi"));
	}

	public String getGntypEAS(){
		return getGenotype(getValue(info, "AN_EAS"), getValue(info, "AC_EAS"), getValue(info, "Hom_EAS"),
				getValue(info, "Het_EAS"), getValue(info, "Hemi_EAS"));
	}

	public String getGntypEUR(){
		return getGenotype(getValue(info, "AN_NFE"), getValue(info, "AC_NFE"), getValue(info, "Hom_NFE"),
				getValue(info, "Het_NFE"), getValue(info, "Hemi_NFE"));
	}

	public String getHeader() {
		if(!chr.startsWith("chr")){
			chr = "chr" + chr;
		}
		return chr + "\t" + pos + "\t" + ref + "\t" + join(alt);
	}
	
	private String getValue(HashMap<String, String> map, String key){
		String str = "";
		if(map.containsKey(key)){
			str = map.get(key);
		}
		return str;
	}

	private int[] getAltFlag(String[] alt){
		int[] altFlag = new int[alt.length];

		for(int i=0; i<alt.length; i++){
			altFlag[i] = getBaseCode(alt[i]);
		}
		return altFlag;
	}
	private int getBaseCode(String base){
		if(base.equals("A")){
			return 1;
		}
		else if(base.equals("C")){
			return 10;
		}
		else if(base.equals("G")){
			return 100;
		}
		else if(base.equals("T")){
			return 1000;
		}
		else{
			return 10000;
		}		
	}
	private int baseCode2index(int baseCode){
		int index = 14;
		// AA, AC, AG, AT, CC, CG, CT, GG, GT, TT, A, C, G, T
		switch(baseCode){
		case 2: index = 0; break;
		case 11: index = 1; break;
		case 101: index = 2; break;
		case 1001: index = 3; break;
		case 20: index = 4; break;
		case 110: index = 5; break;
		case 1010: index = 6; break;
		case 200: index = 7; break;
		case 1100: index = 8; break;
		case 2000: index = 9; break;
		case 1: index = 10; break;
		case 10: index = 11; break;
		case 100: index = 12; break;
		case 1000: index = 13; break;
		}
		return index;
	}
	public boolean notIndel(){
		boolean flag = false;
		if(ref.length() == 1){
			flag = true;
		}
		return flag;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(chr + "\t");
		sb.append(pos + "\t");
		sb.append(ref + "\t");

		String sep = "";
		for(String a : alt){
			sb.append(sep + a);
			sep = ",";
		}
		sb.append("\t");

		sep = "";
		for(String k : info.keySet()){
			sb.append(sep + k + "=" + info.get(k));
			sep = ";";
		}

		return sb.toString();		
	}

	private int[] parseInts(String[] s) {
		int[] x = new int[s.length];
		for(int i = 0; i < s.length; i++){
			x[i] = Integer.parseInt(s[i]);
		}
		return x;
	}
	private String join(String[] str){
		StringBuilder res = new StringBuilder();
		String sep = "";
		for(String s : str){
			res.append(sep + s);
			sep = ",";
		}
		return res.toString();
	}

	/**
	 * @param args : ExAC file name
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
					if(!line.startsWith("#")){
						ExAC2Gntyp data = new ExAC2Gntyp(line);
						if(data.notIndel()){
							System.out.println(data.getHeader() + "\t" + data.getGntypAll() + "\t" 
									+ data.getGntypEAS() + "\t" + data.getGntypEUR());
						}
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