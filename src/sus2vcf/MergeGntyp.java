package sus2vcf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TreeMap;

public class MergeGntyp {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TreeMap<Long, GntypLine> data = new TreeMap<Long, GntypLine>();
		String line = null;
		String chr = null;

		if (args.length < 1) {
			System.err.println("args: frq2 file names");
			System.exit(0);
		}
		for(String filename:args){
			try {
				long time = System.currentTimeMillis();
				FileOpen fo = new FileOpen(filename);
				System.err.print("Read:" + filename + ":" + fo.getFileType() + ":");
				
				while ((line = fo.getBufferedReader().readLine()) != null) {
					String[] split_line = line.split("\t");
					chr = split_line[0];
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

		System.err.print("Output result:\t");
		long time = System.currentTimeMillis();
		for(Long pos : data.keySet()){	
			System.out.println(chr + "\t" + pos + "\t" + data.get(pos));
		}
		System.err.println((System.currentTimeMillis() - time)/1000.0);
	}
}