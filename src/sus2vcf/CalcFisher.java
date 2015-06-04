package sus2vcf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import pal.statistics.FisherExact;

public class CalcFisher {
	private String[] fileNames;
	private FisherExact fisherTest;

	public static void main(String[] args) {
		CalcFisher calcFisher = new CalcFisher(args);
		calcFisher.calc();
	}

	CalcFisher(String[] fileNames){
		this.fileNames = fileNames;
	}

	void calc(){
		String line = null;

		long time = System.currentTimeMillis();
		fisherTest = new FisherExact(100000);
		// System.err.println("calc log:" + (System.currentTimeMillis() - time)/1000.0);

		if (fileNames.length < 1) {
			System.err.println("args: frq file names");
			System.exit(0);
		}
		for(String filename : fileNames){
			try {
				System.err.println("Read:" + filename);
				// time = System.currentTimeMillis();

				File file = new File(filename);
				BufferedReader br = new BufferedReader(new FileReader(file));

				while ((line = br.readLine()) != null) {
					String[] splitLine = line.split("\t");
					if(splitLine[8].equalsIgnoreCase("ExAC:-"))continue;

					calc(line);
				}

				System.err.println("calc fisher:" + (System.currentTimeMillis() - time)/1000.0);
				br.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	void calc(String line){

		String[] splitLine = line.split("\t");

		// read Allele Counnt Data
		AlleleCountData acData = new AlleleCountData(splitLine[4], splitLine[7]);

		// read ExAC Data
		ExACData exacData = new ExACData(splitLine[8]);

		// loop each alternative allele
		for(int i = 0;i < acData.alt.length;i++){

			System.out.print(line + "\t" + acData.getAlt(i) + "\t");

			// get allele count
			int orderExAC = exacData.getOrder(acData.alt[i]);
				
			// calc Recessive model
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

			// calc Dominant model
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

			// print allele freq.
			System.out.print(refDominant + "\t" + altAcHet + "\t" + altRecessive + "\t");			
			System.out.print(refDominant_ExAC + "\t" + altAcHet_ExAC + "\t" + altRecessive_ExAC + "\t");
			System.out.print(refDominantEAS_ExAC + "\t" + altAcHetEAS_ExAC + "\t" +altRecessiveEAS_ExAC + "\t");

			// print recessive 
			System.out.print(refRecessive + "\t" + altRecessive + "\t");
			System.out.print(refRecessive_ExAC + "\t" + altRecessive_ExAC + "\t" 
					+ fisherTest.getTwoTailedP(refRecessive, altRecessive, refRecessive_ExAC, altRecessive_ExAC) + "\t");
			System.out.print(refRecessiveEAS_ExAC + "\t" + altRecessiveEAS_ExAC + "\t" 
					+ fisherTest.getTwoTailedP(refRecessive, altRecessive, refRecessiveEAS_ExAC, altRecessiveEAS_ExAC) + "\t");
			
			// print dominant
			System.out.print(refDominant + "\t" + altDominant + "\t");
			System.out.print(refDominant_ExAC + "\t" + altDominant_ExAC + "\t" 
					+ fisherTest.getTwoTailedP(refDominant, altDominant, refDominant_ExAC, altDominant_ExAC) + "\t");
			System.out.print(refDominantEAS_ExAC + "\t" + altDominantEAS_ExAC + "\t" 
					+ fisherTest.getTwoTailedP(refDominant, altDominant, refDominantEAS_ExAC, altDominantEAS_ExAC) + "\n");
		}
	}
}
