package sus2vcf;

public class ExACData {
	private int AN_Adj_ExAC, AN_EAS_ExAC;
	private String[] altExAC;
	private int[] AC_Adj_ExAC, AC_EAS_ExAC, AC_Hom_ExAC, Hom_EAS_ExAC, AC_Het_ExAC, Het_EAS_ExAC;

	public ExACData(String ExACString){
		setExACData(ExACString.split("[:;]"));
	}

	public void setExACData(String[] ExACValues){
		altExAC = ExACValues[1].split(",");
		AC_Hom_ExAC = new int[altExAC.length];
		Hom_EAS_ExAC = new int[altExAC.length];
		AC_Het_ExAC = new int[altExAC.length];
		Het_EAS_ExAC = new int[altExAC.length];

		for(String ExACValue : ExACValues){
			String[] value = ExACValue.split("=");
			if(value[0].equals("AN_Adj")){
				AN_Adj_ExAC = Integer.parseInt(value[1]);
			}
			else if(value[0].equals("AC_Adj")){
				AC_Adj_ExAC = parseInts(value[1].split(","));
			}
			else if(value[0].equals("AN_EAS")){
				AN_EAS_ExAC = Integer.parseInt(value[1]);
			}
			else if(value[0].equals("AC_EAS")){
				AC_EAS_ExAC = parseInts(value[1].split(","));
			}
			else if(value[0].equals("AC_Het")){
				AC_Het_ExAC = getHetero(parseInts(value[1].split(",")));
			}
			else if(value[0].equals("Het_EAS")){
				Het_EAS_ExAC = getHetero(parseInts(value[1].split(",")));
			}
			else if(value[0].equals("AC_Hom")){
				AC_Hom_ExAC = parseInts(value[1].split(","));
			}
			else if(value[0].equals("Hom_EAS")){
				Hom_EAS_ExAC = parseInts(value[1].split(","));
			}
		}			
	}

	public int getOrder(String alt) {
		for(int i = 0;i < altExAC.length; i++){
			if(altExAC[i].equals(alt)){
				return i;
			}
		}
		return 999;
	}

	public int getAltHom_ExAC(int altOrder) {
		if(altOrder != 999)	return AC_Hom_ExAC[altOrder];
		return 0;
	}
	public int getAltHomEAS_ExAC(int altOrder) {
		if(altOrder != 999)	return Hom_EAS_ExAC[altOrder];
		return 0;
	}
	public int getNSamples_ExAC(){
		return AN_Adj_ExAC / 2;
	}
	
	public int getNSamplesEAS_ExAC(){
		return AN_EAS_ExAC / 2;
	}

	public int getAltHet_ExAC(int altOrder) {
		if(altOrder != 999)	return AC_Het_ExAC[altOrder];
		return 0;
	}

	public int getAltHetEAS_ExAC(int altOrder) {
		if(altOrder != 999)	return Het_EAS_ExAC[altOrder];
		return 0;
	}
	
	private int[] getHetero(int[] x) {
		int[] res = new int[altExAC.length];
		if(altExAC.length == 1){
			res[0] = x[0];
			return res;
		}
		if(altExAC.length == 2){
			res[0] = x[0] + x[2];
			res[1] = x[1] + x[2];
			return res;
		}
		res[0] = x[0] + x[3] + x[5];
		res[1] = x[1] + x[3] + x[4];
		res[2] = x[2] + x[4] + x[5];
		return res;
	}
	
	public int[] parseInts(String[]s){
		int[] x = new int[s.length];
		for(int i=0;i<s.length;i++){
			x[i] = Integer.parseInt(s[i]);
		}
		return x;
	}

	public int getAN(){
		return AN_Adj_ExAC;
	}
	public int getAN_EAS(){
		return AN_EAS_ExAC;
	}
	public int geACRef_ExAC(int orderExAC) {
		if(orderExAC != 999) return AN_Adj_ExAC - sum(AC_Adj_ExAC);
		return 0;
	}
	public int geACAlt_ExAC(int orderExAC) {
		if(orderExAC != 999) return AC_Adj_ExAC[orderExAC];
		return 0;
	}
	public int geACRefEAS_ExAC(int orderExAC) {
		if(orderExAC != 999) return AN_EAS_ExAC - sum(AC_EAS_ExAC);
		return 0;
	}
	public int geACAltEAS_ExAC(int orderExAC) {
		if(orderExAC != 999) return AC_EAS_ExAC[orderExAC];
		return 0;
	}
	
	private int sum(int[] aC_Adj_ExAC2) {
		int res = 0;
		for(int val : aC_Adj_ExAC2){
			res += val;
		}
		return res;
	}
}
