package sus2vcf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Gntyp2VCF {

	public static String gntyp2VCF(String data){
		if(data.length() == 0){
			return "";
		}
		String[] datas = data.split("\t");
		String chr = datas[0];
		long pos = Long.parseLong(datas[1]);
		String ref = datas[2];
		int AN = Integer.parseInt(datas[3]);

		if(getCount(datas[4].split(",")) <= 1){
			return "";
		}
		String[] alt = getAlt(ref, datas[4].split(","));

		return chr + "\t" + pos + "\t\t" + ref + "\t" + alt[0] + "\t.\t.\tAN=" + AN + ";GC=" + alt[1];
	}

	private static int getCount(String[] genotypeCount) {
		int count = 0;
		for(String s : genotypeCount){
			count += Integer.parseInt(s);
		}
		return count;
	}

	private static String[] getAlt(String ref, String[] s) {
		StringBuilder alt = new StringBuilder();
		StringBuilder nGCs = new StringBuilder();
		String refStr = null;
		String refCount = null;
		String sep = ",";
		
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>(); 
		int count = 0;
		for(Integer i = 0; i < s.length; i++){
			if(i < 10){
				count += Integer.parseInt(s[i]);
			}

			if(getGntyp(i).equals(ref + ref)){
				refStr = ref + ref;
				refCount = s[i];
			}
			else if(getGntyp(i).equals(ref)){
				if(count == 0){
					refStr = ref;
					refCount = s[i];
				}
				else if(!s[i].equals("0")){
					refStr = refStr + sep + ref;
					refCount = refCount + sep + s[i];					
				}
			}
			else if(!s[i].equals("0")){
				map.put(i, Integer.valueOf(s[i]));
			}			
		}

		alt.append(refStr);
		nGCs.append(refCount);

		ArrayList<Map.Entry<Integer, Integer>> entries = 
				new ArrayList<Map.Entry<Integer, Integer>>(map.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<Integer, Integer>>() {
			@Override
			public int compare(Map.Entry<Integer, Integer> entry1, Map.Entry<Integer, Integer> entry2) {
				return ((Integer)entry2.getValue()).compareTo((Integer)entry1.getValue());
			}
		});

		for(Map.Entry<Integer, Integer> e: entries){
			alt.append(sep + getGntyp(e.getKey().intValue()));
			nGCs.append(sep + e.getValue());
		}

		String[] ret = new String[2];
		ret[0] = alt.toString();
		ret[1] = nGCs.toString();
		return ret;
	}

	private static String getGntyp(int column) {
		// 0:AA, 1:AC, 2:AG, 3:AT, 4:CC, 5:CG, 6:CT, 7:GG, 8:GT, 9:TT, 10:A, 11:C, 12:G, 13:T
		switch (column) {
		case 0:
			return "AA";
		case 1:
			return "AC";
		case 2:
			return "AG";
		case 3:
			return "AT";
		case 4:
			return "CC";
		case 5:
			return "CG";
		case 6:
			return "CT";
		case 7:
			return "GG";
		case 8:
			return "GT";
		case 9:
			return "TT";
		case 10:
			return "A";
		case 11:
			return "C";
		case 12:
			return "G";
		case 13:
			return "T";
		}
		return null;
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

				FileOpen fo = new FileOpen(filename);
				while ((line = fo.getBufferedReader().readLine()) != null) {
					String vcf = Gntyp2VCF.gntyp2VCF(line);
					if(!vcf.isEmpty()){
						System.out.println(vcf);
					}
				}

				System.err.println((System.currentTimeMillis() - time)/1000);
				fo.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}