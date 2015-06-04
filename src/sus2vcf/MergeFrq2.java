package sus2vcf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

public class MergeFrq2 {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TreeMap<Integer, Frq2Line> data = new TreeMap<Integer, Frq2Line>();
		String line = null;
		String chr = null;

		if (args.length < 1) {
			System.err.println("args: frq2 file names");
			System.exit(0);
		}
		for(String filename:args){
			try {
				System.err.print("Read:" + filename + "\t");
				long time = System.currentTimeMillis();
				
				File file = new File(filename);
				BufferedReader br = new BufferedReader(new FileReader(file));
				
				while ((line = br.readLine()) != null) {
					String[] split_line = line.split("\t");
					chr = split_line[0];
					if(data.containsKey(Integer.valueOf(split_line[1]))){
						data.get(Integer.valueOf(split_line[1])).addData(split_line);
					}
					else{
						Frq2Line n_data = new Frq2Line();
						n_data.addData(split_line);
						data.put(Integer.valueOf(split_line[1]), n_data);
					}
				}

				System.err.println((System.currentTimeMillis() - time)/1000);
				br.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Iterator<Integer> it = data.keySet().iterator();
		while(it.hasNext()){
			Integer pos = it.next();
			System.out.println(chr + "\t" + pos + "\t" + data.get(pos));
		}
	}


	static public class Frq2Line {
		private String ref;
		private int ac;
		private int[] ac_a,ac_c,ac_g,ac_t;

		public Frq2Line(){
			ref = null;
			ac = 0;
			ac_a = init_ac();
			ac_c = init_ac();
			ac_g = init_ac(); 
			ac_t = init_ac();
		}

		public void addData(String[] data){
			ref = data[2];
			ac += Integer.parseInt(data[4]);
			ac_a = split_ac(ac_a, data[5]);
			ac_c = split_ac(ac_c, data[6]);
			ac_g = split_ac(ac_g, data[7]); 
			ac_t = split_ac(ac_t, data[8]);
		}

		private int[] split_ac(int[] ac, String string) {
			String[] temp = string.substring(1, string.length()-1).split(",");
			ac[0] += Integer.parseInt(temp[1]);
			ac[1] += Integer.parseInt(temp[2]);			
			return ac;
		}

		private int[] init_ac() {
			int[] ac = new int[2];
			return ac;
		}

		public String toString(){		
			return ref + "\t\t" + ac + "\t(A," + ac_a[0] + "," + ac_a[1] + ")\t" + "(C," + ac_c[0] + "," + ac_c[1] + ")\t" +  "(G," + ac_g[0] + "," + ac_g[1] + ")\t" + "(T," + ac_t[0] + "," + ac_t[1] + ")";
		}
	}
}