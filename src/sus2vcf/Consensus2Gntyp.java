package sus2vcf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

public class Consensus2Gntyp{

	private String fileList;
	private String chr;
	private String checkSexFile;
	private HashMap<String, Boolean> checkSex;
	private TreeMap<Long, GntypLine> genotype;

	public Consensus2Gntyp(String fileList, String chr, String checkSexFile){
		this.fileList = fileList;
		this.chr = chr;
		this.checkSexFile = checkSexFile;
		genotype = new TreeMap<Long, GntypLine>();
	}

	public void readSexFile() throws IOException {
		long time = System.currentTimeMillis();

		FileOpen fo = new FileOpen(checkSexFile);
		checkSex = new HashMap<String, Boolean>();
		String line = new String();
		while ((line = fo.getBufferedReader().readLine()) != null) {
			String[] data = line.split("\t");
			String name = getName(data[0]);
			if(Float.parseFloat(data[1]) / (Float.parseFloat(data[1]) + Float.parseFloat(data[2])) < (1.0 / 3.0)){
				checkSex.put(name, true);
				System.err.println(name + "\tmale");
			}
			else{
				checkSex.put(name, false);
				System.err.println(name + "\tfemale");
			}
		}
		fo.close();
		System.err.println("Read " + checkSexFile + ":" + ((System.currentTimeMillis() - time)/1000.0) + "sec.");
	}

	private String getName(String filePath) {
		String[] paths = filePath.split("/");
		String[] fileNames = paths[paths.length - 1].split("\\.");
		return fileNames[0];
	}

	public void makeGntyp() throws IOException{
		FileOpen foFileList = new FileOpen(fileList);
		String filePath = new String();
		while ((filePath = foFileList.getBufferedReader().readLine()) != null) {
			String name = getName(filePath);
			boolean isMale;
			if(checkSex.containsKey(name)){
				isMale = checkSex.get(name);
			}
			else{
				System.err.println("Cannot find checkSex of " + name);
				break;
			}

			if(filePath.contains("chr1")) filePath = filePath.replace("chr1", chr) ;
			if(filePath.contains("chrX")) filePath = filePath.replace("chrX", chr);

			System.err.print(name + ":" + filePath + ":male=" + isMale);
			long time = System.currentTimeMillis();

			long nLines = 0;

			FileOpen foVCFFile = new FileOpen(filePath);
			String line = new String();
			while ((line = foVCFFile.getBufferedReader().readLine()) != null) {
				if(line.startsWith(chr) && !line.contains("INDEL")){
					String[] data = line.split("\t");
					// 0:chr, 1:pos, 2:id, 3:ref, 4:alt, 5:qual, 6:filter, 7:info, 8:format, 9:genotype

					// check QV>=40 and DP>=10
					if(Float.parseFloat(data[5]) >= 40.0 && countDP(data[7]) >= 10){
						Long pos = Long.parseLong(data[1]);

						String genotypeString = new String();

						boolean notPAR = notPAR(data[0], data[1]);
						if(data[0].equals("chrY")){
							if(!isMale)break;
							if(!notPAR)continue;
						}
						if(isMale && notPAR){
							if(data[4].equals(".")){
								genotypeString = data[3];
							}
							else if(data[4].length() == 1){
								if(isHetero(data[9])){
									System.err.println("miscall:" + line);
									continue;
								}
								else if(isHomo(data[9])){
									genotypeString = data[4];
								}
							}
							else{
								System.err.println("miscall:" + line);
								continue;
							}
						}
						else{
							if(data[4].equals(".")){
								genotypeString = data[3] + data[3];
							}
							else if(data[4].length() == 1){
								if(isHetero(data[9])){
									genotypeString = data[3] + data[4];
								}
								else if(isHomo(data[9])){
									genotypeString = data[4] + data[4];
								}
							}
							else{
								String[] alt = data[4].split(",");
								genotypeString = alt[0] + alt[1];							
							}
						}
						if(!genotype.containsKey(pos)){
							genotype.put(pos, new GntypLine(data[3], genotypeString));
						}
						else{
							genotype.get(pos).addData(data[3], genotypeString);
						}
					}	
				}
				nLines ++;
			}
			foVCFFile.close();

			System.err.println(":" + nLines + "lines:" + ((System.currentTimeMillis() - time)/1000.0) + "sec.");

		}
		foFileList.close();
	}

	/**
	 * check !PAR && (chrX || chrY)
	 * @param chr
	 * @param posString
	 * @return notPAR
	 */
	private boolean notPAR(String chr, String posString) {
		//	+---------+----------+----------+---------+-----------+-----------+
		//	| chrom_1 | start_1  | end_1    | chrom_2 | start_2   | end_2     |
		//	+---------+----------+----------+---------+-----------+-----------+
		//	| Y       |    10001 |  2649520 | X       |     60001 |   2699520 |
		//	| Y       | 59034050 | 59373566 | X       | 154931044 | 155270560 |
		//	+---------+----------+----------+---------+-----------+-----------+
		long pos = Long.parseLong(posString);
		if((chr.equals("chrX") && (pos < 60001 || (2699520 < pos && pos < 154931044) || 155270560 < pos))
				|| (chr.equals("chrY") && (pos < 10001 || (2649520 < pos && pos <  59034050) ||  59373566 < pos))){
			return true;
		}
		return false;
	}

	private boolean isHetero(String genotype) {
		if(genotype.startsWith("0/1")) return true;
		return false;
	}
	private boolean isHomo(String genotype) {
		if(genotype.startsWith("1/1")) return true;
		return false;
	}

	private int countDP(String info) {
		String[] info_parts = info.split(";");
		for(String parts : info_parts){
			String[] s = parts.split("=");
			if(s[0].equals("DP")){
				return Integer.parseInt(s[1]);
			}
		}
		return 0;
	}

	public void printGenotype(String fileName) throws IOException {
		FileWriter fw = new FileWriter(new File(fileName));
		BufferedWriter bw = new BufferedWriter(fw);
		for(Long pos : genotype.keySet()){
			bw.write(chr + "\t" + pos + "\t" + genotype.get(pos));
			bw.newLine();
		}
		bw.close();fw.close();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 4) {
			System.err.println("arg1: consensus file list");
			System.err.println("arg2: target chr name");
			System.err.println("arg3: check sex file name");
			System.err.println("arg4: output file name");
			System.exit(1);
		}
		Consensus2Gntyp consensus2Gntyp = new Consensus2Gntyp(args[0], args[1], args[2]);
		try {
			long time = System.currentTimeMillis();
			consensus2Gntyp.readSexFile();
			System.err.println("Finish read check sex:" + ((System.currentTimeMillis() - time)/1000.0) + "sec.");
			consensus2Gntyp.makeGntyp();
			System.err.println("Finish read consensus:" + ((System.currentTimeMillis() - time)/1000.0) + "sec.");
			consensus2Gntyp.printGenotype(args[3]);
			System.err.println("Finish output:" + ((System.currentTimeMillis() - time)/1000.0) + "sec.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
