package sus2vcf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import pal.statistics.FisherExact;

public class CalcFisher2 {
	private String sampleListName, callFileName, dpFileName, exacFileName;
	private FisherExact fisherTest;
	private ArrayList<String> sampleList;

	public static void main(String[] args) {
		CalcFisher2 calcFisher = new CalcFisher2(args);
		calcFisher.calc();
	}

	CalcFisher2(String[] fileNames){
		this.sampleListName = fileNames[0];
		this.callFileName = fileNames[1];
		this.dpFileName = fileNames[2];
		this.exacFileName = fileNames[3];		
	}

	void calc(){

		long time = System.currentTimeMillis();
		fisherTest = new FisherExact(100000);
		// System.err.println("calc log:" + (System.currentTimeMillis() - time)/1000.0);

		if (callFileName.isEmpty() || dpFileName.isEmpty() || exacFileName.isEmpty()) {
			System.err.println("args: <call file> <dp file> <ExAC file>");
			System.exit(0);
		}
		try {
			System.err.println("Read:" + sampleListName);
			File sampleListFile = new File(sampleListName);
			BufferedReader br = new BufferedReader(new FileReader(sampleListFile));
			sampleList = new ArrayList<String>();
			String line = null;
			while((line = br.readLine()) != null){
				sampleList.add(line);
			}
			br.close();

			System.err.println("Read:" + callFileName);
			System.err.println("Read:" + dpFileName);
			System.err.println("Read:" + exacFileName);
			File callFile = new File(callFileName);
			File dpFile = new File(dpFileName);
			File exacFile = new File(exacFileName);

			BufferedReader callBr = new BufferedReader(new FileReader(callFile));
			BufferedReader dpBr = new BufferedReader(new FileReader(dpFile));
			BufferedReader exacBr = new BufferedReader(new FileReader(exacFile));

			String callLine = null;
			String dpLine = null;
			String exacLine = null;
			ArrayList<Integer> sampleNumber = new ArrayList<Integer>();
			HashMap<String, String> exacData = new HashMap<String, String>();
			while((exacLine = exacBr.readLine()) != null){
				String[] temp = exacLine.split("\t");
				String pos = temp[0] + "\t" + temp[1];
				exacData.put(pos, exacLine);
			}
			exacBr.close();

			// print header
			printHeader();

			while((callLine = callBr.readLine()) != null && (dpLine = dpBr.readLine()) != null){
				String[] callData = callLine.split("\t");
				String[] dpData = dpLine.split("\t");

				String acData = null;
				if(callData[0].equals("chr")){
					for(int i=4;i<callData.length;i++){
						if(sampleList.contains(callData[i])){
							sampleNumber.add(i);
						}
					}
				}
				else if(!sampleNumber.isEmpty()){
					acData = alleleCount(sampleNumber, callData, dpData);
					//System.err.println(callLine + "\n" + acData);

					String pos = callData[0] + "\t" + callData[1];
					String prefix = pos + "\t" + callData[2] + "\t" + callData[3];
					calc(prefix, acData, exacData.get(pos));
					//String[] alt = acData.split("\t");
					//System.out.println(callData[0] + "\t" + callData[1] + "\t" + callData[2] + "\t" + callData[3] + "\t" + alt[0] + "\t" + ret + "\t" + alt[1] + "\t" + exacData.get(pos));
				}
			}
			callBr.close();
			dpBr.close();

			// time = System.currentTimeMillis();

			//			while ((line = br.readLine()) != null) {
			//				String[] splitLine = line.split("\t");
			//				if(splitLine[8].equalsIgnoreCase("ExAC:-"))continue;
			//
			//				calc(line);
			//			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.err.println("calc fisher:" + (System.currentTimeMillis() - time)/1000.0);
	}

	private String alleleCount(ArrayList<Integer> sampleNumber, String[] callData, String[] dpData) {

		String ref = callData[3];
		ArrayList<String> call = new ArrayList<String>();
		int an = 0;
		// int array for A C G T
		HashMap<String, Integer> ac = new HashMap<String, Integer>();
		HashMap<String, Integer> acHet = new HashMap<String, Integer>();

		for(int i=0; i<sampleNumber.size();i++){
			int dp = Integer.parseInt(dpData[sampleNumber.get(i)]);
			if(dp >= 20){
				an += 2;
				String[] altString = callData[sampleNumber.get(i)].split(",");
				for(String temp : altString){
					// get alt
					if(!call.contains(temp)){
						call.add(temp);
					}

					// get allele count
					if(!ac.containsKey(temp)){
						if(altString.length == 1){
							ac.put(temp, Integer.valueOf(2));
							acHet.put(temp, Integer.valueOf(0));
						}
						else{
							ac.put(temp, Integer.valueOf(1));
							acHet.put(temp, Integer.valueOf(1));
						}

					}
					else{
						Integer acTemp = ac.get(temp) + 1;
						if(altString.length == 1){
							acTemp ++;
						}
						else{
							Integer acHetTemp = acHet.get(temp) + 1;
							acHet.put(temp, acHetTemp);
						}
						ac.put(temp, acTemp);
					}
				}
			}
		}

		StringBuilder altString = new StringBuilder();
		StringBuilder acString = new StringBuilder();
		StringBuilder acHetString = new StringBuilder();
		String sep = "";

		if(!call.contains(ref)){
			call.add(ref);
			ac.put(ref, 0);
			acHet.put(ref, 0);
		}

		for(String temp : call){
			if(temp.equals(ref)){
				acString.insert(0, "AC=" + ac.get(temp));
				acHetString.insert(0, "AC_het=" + acHet.get(temp));
			}
			else{
				altString.append(sep + temp);
				acString.append("," + ac.get(temp));
				acHetString.append("," + acHet.get(temp));
				sep = ",";
			}
		}
		return altString.toString() + "\tAN=" + an + ";" + acString.toString() + ";" + acHetString.toString();
	}

	void calc(String prefix, String acString, String exacString){

		// read Allele Counnt Data
		String[] acDatas = acString.split("\t");

		// read ExAC Data
		String[] exacStrings = exacString.split("\t");

		if(acDatas[0].equals("") || exacStrings[3].equals("ExAC:-")){
			if(acDatas[0].equals(""))acDatas[0] = "-";
			System.out.print(prefix + "\t" + acDatas[0]);
			for(int i=0;i<36;i++)System.out.print("\t-");
			System.out.print("\t" + acDatas[1] + "\t" + exacStrings[3] + "\t" + exacStrings[4] + "\t" + exacStrings[5] + "\n");
		}
		else{
			AlleleCountData acData = new AlleleCountData(acDatas[0], acDatas[1]);

			// read ExAC Data
			ExACData exacData = new ExACData(exacStrings[3]);

			// loop each alternative allele
			for(int i = 0;i < acData.alt.length;i++){

				System.out.print(prefix + "\t" + acDatas[0] + "\t" + acData.getAlt(i) + "\t");
				//System.out.print(line + "\t" + acData.getAlt(i) + "\t");

				// get allele count
				int orderExAC = exacData.getOrder(acData.alt[i]);

				// get target allele count
				int altRecessive = acData.getNSamples_Hom(i);
				int refRecessive = acData.getNSamples() - altRecessive;

				// get ExAC allele count
				int nExACs = exacData.getNSamples_ExAC();
				int nEASExACs = exacData.getNSamplesEAS_ExAC();

				int altRecessive_ExAC = exacData.getAltHom_ExAC(orderExAC);
				int altRecessiveEAS_ExAC = exacData.getAltHomEAS_ExAC(orderExAC);
				int refRecessive_ExAC = nExACs - altRecessive_ExAC;
				int refRecessiveEAS_ExAC = nEASExACs - altRecessiveEAS_ExAC;

				// get target allele count
				int altAcHet = acData.getNSamples_Het(i);
				int altDominant = altAcHet + altRecessive;
				int refDominant = acData.getNSamples() - altDominant;			

				// get ExAC allele count
				int altAcHet_ExAC = exacData.getAltHet_ExAC(orderExAC);
				int altAcHetEAS_ExAC = exacData.getAltHetEAS_ExAC(orderExAC);
				int altDominant_ExAC = altAcHet_ExAC + altRecessive_ExAC;
				int altDominantEAS_ExAC = altAcHetEAS_ExAC + altRecessiveEAS_ExAC;
				int refDominant_ExAC = nExACs - altDominant_ExAC;
				int refDominantEAS_ExAC = nEASExACs - altDominantEAS_ExAC;

				// print Fisher's exact test (recessive) 
				System.out.print(refRecessive + "\t" + altRecessive + "\t");
				System.out.print(refRecessive_ExAC + "\t" + altRecessive_ExAC + "\t" 
						+ fisherTest.getTwoTailedP(refRecessive, altRecessive, refRecessive_ExAC, altRecessive_ExAC) + "\t");
				System.out.print(refRecessiveEAS_ExAC + "\t" + altRecessiveEAS_ExAC + "\t" 
						+ fisherTest.getTwoTailedP(refRecessive, altRecessive, refRecessiveEAS_ExAC, altRecessiveEAS_ExAC) + "\t");

				// print Fisher's exact test (dominant)
				System.out.print(refDominant + "\t" + altDominant + "\t");
				System.out.print(refDominant_ExAC + "\t" + altDominant_ExAC + "\t" 
						+ fisherTest.getTwoTailedP(refDominant, altDominant, refDominant_ExAC, altDominant_ExAC) + "\t");
				System.out.print(refDominantEAS_ExAC + "\t" + altDominantEAS_ExAC + "\t" 
						+ fisherTest.getTwoTailedP(refDominant, altDominant, refDominantEAS_ExAC, altDominantEAS_ExAC) + "\t");
				
				// print Fisher's exact test (allele count)
				int refCountCase = acData.getAC_Other(i);
				int altCountCase = acData.getAC(i);
				int altCountExAC = exacData.geACAlt_ExAC(orderExAC);
				int refCountExAC = exacData.getAN() - altCountExAC;
				int altCountExACEAS = exacData.geACAltEAS_ExAC(orderExAC);
				int refCountExACEAS = exacData.getAN_EAS() - altCountExACEAS;
				System.out.print(refCountCase + "\t" + altCountCase + "\t");				
				System.out.print(refCountExAC + "\t" + altCountExAC + "\t" 
						+ fisherTest.getTwoTailedP(refCountCase, altCountCase, refCountExAC, altCountExAC) + "\t");
				System.out.print(refCountExACEAS + "\t" + altCountExACEAS + "\t" 
						+ fisherTest.getTwoTailedP(refCountCase, altCountCase, refCountExACEAS, altCountExACEAS) + "\t");

				// print genotype (Case)
				System.out.print(acData.getAC_Other(i) + "\t" + acData.getAC_Het(i) + "\t" + acData.getAC_Hom(i) + "\t");
				// print genotype (ExAC)
				System.out.print(refCountExAC + "\t" + exacData.getAltHet_ExAC(orderExAC) + "\t" + (exacData.getAltHom_ExAC(orderExAC) * 2) + "\t");
				double[][] data = {{acData.getAC_Other(i), acData.getAC_Het(i), acData.getAC_Hom(i)}, {refCountExAC, exacData.getAltHet_ExAC(orderExAC), (exacData.getAltHom_ExAC(orderExAC) * 2)}};
				System.out.print(cochranArmitageTrendTest(data) + "\t");
				// print genotype (ExAC(EAS))
				System.out.print(refCountExACEAS + "\t" + exacData.getAltHomEAS_ExAC(orderExAC) + "\t" + (exacData.getAltHomEAS_ExAC(orderExAC) * 2) + "\t");				
				double[][] dataEas = {{acData.getAC_Other(i), acData.getAC_Het(i), acData.getAC_Hom(i)}, {refCountExACEAS, exacData.getAltHetEAS_ExAC(orderExAC), (exacData.getAltHomEAS_ExAC(orderExAC) * 2)}};
				System.out.print(cochranArmitageTrendTest(dataEas) + "\t");
				// print annotation
				System.out.print(acDatas[1] + "\t" + exacStrings[3] + "\t" + exacStrings[4] + "\t" + exacStrings[5] + "\n");
			}
		}
	}

	private void printHeader() {
		System.out.print("\t\t\t\t\t\tRecessive\t\t\t\t\t\t\t\tDominant\t\t\t\t\t\t\t\tAllele Count\t\t\t\t\t\t\t\tGenotype\n");
		System.out.print("chr\tpos\trsID\tref\talt(all)\talt\t");
		System.out.print("ref(case)\talt(case)\tref(ExAC)\talt(ExAC)\tp-value(ExAC)\t");
		System.out.print("ref(ExAC(EAS))\talt(ExAC(EAS))\tp-value(ExAC(EAS))\t");
		System.out.print("ref(case)\talt(case)\tref(ExAC)\talt(ExAC)\tp-value(ExAC)\t");
		System.out.print("ref(ExAC(EAS))\talt(ExAC(EAS))\tp-value(ExAC(EAS))\t");
		System.out.print("ref(case)\talt(case)\tref(ExAC)\talt(ExAC)\tp-value(ExAC)\t");
		System.out.print("ref(ExAC(EAS))\talt(ExAC(EAS))\tp-value(ExAC(EAS))\t");
		System.out.print("ref_hom(case)\talt_het(case)\talt_hom(case)\t");
		System.out.print("ref_hom(ExAC)\talt_het(ExAC)\talt_hom(ExAC)\tadditive_risk(ExAC)\t");
		System.out.print("ref_hom(ExAC(EAS))\talt_hetero(ExAC(EAS))\talt_hom(ExAC(EAS))\tadditive_risk(ExAC(EAS))\t");
		System.out.print("allelec_count\tExAC\tControl(JPN:373)\tannotation\n");
	}

	private double cochranArmitageTrendTest(double[][] data) {
		double N=0;
		double R=0;
		double S=0;
		double[] n = new double[data[0].length];
		for(int i=0;i<data.length;i++){
			for(int j=0;j<data[i].length;j++){
				N+=data[i][j];
				if(i==0){
					R+=data[i][j];
				}else if(i==1){
					S+=data[i][j];
				}
				n[j]+=data[i][j];
			}
		}
		double Y = (N-1)*Math.pow(N*(data[0][1]+2*data[0][2])-R*(n[1]+2*n[2]),2)/(R*S*(N*(n[1]+4*n[2])-Math.pow((n[1]+2*n[2]),2)));
		System.err.print("\n"+N+"\t"+data[0][0]+","+data[0][1]+","+data[0][2]+"\t"+data[1][0]+","+data[1][1]+","+data[1][2]+"\t"+R+"\t"+S+"\t"+Y+"\n");
		return(Y);
	}
}
