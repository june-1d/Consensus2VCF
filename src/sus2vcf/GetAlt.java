package sus2vcf;

import java.io.FileNotFoundException;
import java.io.IOException;

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
	
				FileOpen fo = new FileOpen(filename);
				
				while ((line = fo.getBufferedReader().readLine()) != null) {
					GetAlt data = new GetAlt(line);
					if(data.checkAlt()){
						System.out.println(data);
					}
				}

				System.err.println((System.currentTimeMillis() - time)/1000);
				fo.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}