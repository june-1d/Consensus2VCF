package sus2vcf;

public class AlleleCountData {
	public int AN = 0;
	public String[] alt;
	public int[] AC, AC_Het, AC_Hom;

	public AlleleCountData(String altString, String alleleCountString){
		// get alternative alleles
		alt = altString.split(",");
		setAlleleCountData(alleleCountString);	
	}

	// read allele count data
	public void setAlleleCountData(String alleleCount) {
		AC = new int[alt.length + 1];
		AC_Het = new int[alt.length + 1];
		AC_Hom = new int[alt.length + 1];
		for(String acString : alleleCount.split(";")){
			String[] acValue = acString.split("=");
			if(acValue[0].equals("AN")){
				AN = Integer.parseInt(acValue[1]);
			}
			else if(acValue[0].equals("AC")){
				AC = parseInts(acValue[1].split(","));
			}
			else if(acValue[0].equals("AC_het")){
				AC_Het = parseInts(acValue[1].split(","));
			}
		}
		for(int i=0;i<=alt.length;i++){
			AC_Hom[i] = AC[i] - AC_Het[i];
		}
	}

	public int[] parseInts(String[]s){
		int[] x = new int[s.length];
		for(int i=0;i<s.length;i++){
			x[i] = Integer.parseInt(s[i]);
		}
		return x;
	}

	public int getNSamples() {
		return AN /2;
	}

	public int getNSamples_Hom(int altNo) {
		return AC_Hom[altNo + 1] / 2;
	}

	public int getNSamples_Het(int altNo) {
		return AC_Het[altNo + 1];
	}
	
	public int getAC_Other(int altNo){
		return AN - AC[altNo + 1];
	}
	
	public int getACRef(){
		return AC[0];
	}
	public int getAC(int altNo){
		return AC[altNo + 1];
	}
	public int getAC_Hom(int altNo){
		return AC_Hom[altNo + 1];
	}
	public int getAC_Het(int altNo){
		return AC_Het[altNo + 1];
	}
	public int[] getAC(){
		return AC;
	}
	
	public String getAlt(int altNo){
		return alt[altNo];
	}
}
