package sus2vcf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

public class MergeGntyp {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TreeMap<Long, GntypLine> data = new TreeMap<Long, GntypLine>();
		String line = null;
		String chr = "";
		String pre_chr = "";
		
		if (args.length < 1) {
			System.err.println("args1: ouput file name");
			System.err.println("args2-: frq2 file names");
			System.exit(1);
		}
		File output = new File(args[0]);
		if(output.exists()){
			System.err.println(args[0] + " already exists!!");
			System.exit(1);
		}
		
		// Read Gntyp Files
		for(int i=1; i<args.length; i++){
			String filename = args[i];
			try {
				long time = System.currentTimeMillis();
				FileOpen fo = new FileOpen(filename);
				System.err.print("Read:" + filename + ":" + fo.getFileType() + ":");
				
				while ((line = fo.getBufferedReader().readLine()) != null) {
					if(line.startsWith("#"))continue;
					
					String[] split_line = line.split("\t");

					chr = split_line[0];
					if(!pre_chr.isEmpty() && !pre_chr.equals(chr)){
						System.err.println(pre_chr + " is not " + chr);
						System.exit(1);
					}
					pre_chr = chr;
					
					Long pos = Long.valueOf(split_line[1]);
					if(data.containsKey(pos)){
						data.get(pos).addData(split_line);
					}
					else{
						GntypLine n_data = new GntypLine(split_line);
						data.put(pos, n_data);
					}
				}

				System.err.println((System.currentTimeMillis() - time)/1000.0);
				fo.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Output
		System.err.print("Output result:\t");
		long time = System.currentTimeMillis();
		
		try {
			FileWriter fw = new FileWriter(output);
			BufferedWriter bw = new BufferedWriter(fw);
			for(Long pos : data.keySet()){	
				bw.write(chr + "\t" + pos + "\t" + data.get(pos));
				bw.newLine();
			}
			bw.close();fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.err.println((System.currentTimeMillis() - time)/1000.0);
	}
}