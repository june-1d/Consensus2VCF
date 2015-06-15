package sus2vcf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class AddGntypData {
	public HashMap <String, Gntyp> exACData = new HashMap <String, Gntyp>();
	public HashMap <String, Gntyp> ctrlData = new HashMap <String, Gntyp>();

	public AddGntypData(String exACFileName, String ctrlFileName) throws IOException{
		// read ExAC file
		System.err.println("Read ExAC File : " + exACFileName);

		FileOpen fo = new FileOpen(exACFileName);
		String line = null;
		while ((line = fo.getBufferedReader().readLine()) != null) {
			Gntyp data = new Gntyp(line);
			exACData.put(data.getPos(), data);
		}
		fo.close();

		// read control file
		System.err.println("Read Control File : " + ctrlFileName);
		fo = new FileOpen(ctrlFileName);

		line = null;
		while ((line = fo.getBufferedReader().readLine()) != null) {
			Gntyp data = new Gntyp(line);
			ctrlData.put(data.getPos(), data);
		}
		fo.close();
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
		if (args.length < 6) {
			System.err.println("args: <target file> <ExAC file> <ExAC column(0:ALL,1:EAS,2:EUR)> <CTRL file> <CTRL column(0)> <output file>");
			System.exit(1);
		}
		String targetFileName = args[0];
		String exACFileName = args[1];
		int[] exACColumns = parseInts(args[2].split(","));
		String ctrlFileName = args[3];
		int[] ctrlColumns = parseInts(args[4].split(","));
		String outputFileName = args[5];
		
//		System.err.println(targetFileName + "\t" + exACFileName + "\t" + args[2] + "\t" + ctrlFileName + "\t" + args[4]);
		
		try {
			AddGntypData exacCtrlData = new AddGntypData(exACFileName, ctrlFileName);

			// read target data
			System.err.println("Read Target File : " + exACFileName);
			System.err.println("Output File : " + outputFileName);
			FileOpen fo = new FileOpen(targetFileName);

			FileWriter fw = new FileWriter(new File(outputFileName));
			BufferedWriter bw = new BufferedWriter(fw);
			
			String line = null;
			while ((line = fo.getBufferedReader().readLine()) != null) {
				Gntyp data = new Gntyp(line);
				bw.write(data.getHeader() + "\t" + data.getGntyp(0));
				for(int column : exACColumns){
					if(exacCtrlData.exACData.containsKey(data.getPos())){
						bw.write("\t" + exacCtrlData.exACData.get(data.getPos()).getGntyp(column));
					}
					else{
						bw.write("\tno_entry");
					}
				}
				for(int column : ctrlColumns){
					if(exacCtrlData.ctrlData.containsKey(data.getPos())){
						bw.write("\t" + exacCtrlData.ctrlData.get(data.getPos()).getGntyp(column));
					}
					else{
						bw.write("\tno_entry");
					}
				}
				bw.newLine();
			}
			bw.close();fw.close();
			fo.close();
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
}