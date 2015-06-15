package sus2vcf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class CheckHeteroChrX extends Thread{

	private ArrayList<String> filelist;
	private StringBuilder result = new StringBuilder();

	public CheckHeteroChrX(ArrayList<String> filelist){
		this.filelist = filelist;
	}

	public void run(){
		for(String filename: filelist){
			long time = System.currentTimeMillis();
			String[] filenames = filename.split("/");

			filename = filename.replaceAll("chr1", "chrX");
	
			try {
				FileOpen fo = new FileOpen(filename);
				int nLines = 0;
				int homo = 0;
				int hetero = 0;
				String line = new String();
				while ((line = fo.getBufferedReader().readLine()) != null) {
					if(line.startsWith("chrX") && !line.contains("INDEL")){
						String[] data = line.split("\t");
						int pos = Integer.parseInt(data[1]);
						if(!data[5].equals(".")){
							float qual = Float.parseFloat(data[5]);
							if(pos > 2699520 && pos < 154931044 && qual >= 70.0 && countDP(data[7]) >= 10){
								String alt = data[4];
								if(!alt.equals(".")){
									String[] genotype = data[9].split(":");
									if(genotype[0].equals("0/1")){
										hetero ++;
									}
									else if(genotype[0].equals("1/1")){
										homo ++;
									}
								}
							}
						}
					}
					nLines ++;
				}

				result.append(filenames[1] + "\t" + hetero + "\t" + homo + "\n");
				System.err.println(Thread.currentThread().getName() + ":" + filenames[1] + ":" + filename + ":" + fo.getFileType() 
						+ ":" + nLines + "lines:" + ((System.currentTimeMillis() - time)/1000) + "sec.");
				fo.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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

	public String getValue(){
		return result.toString();
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayList<CheckHeteroChrX> threads = new ArrayList<CheckHeteroChrX>();
		int numThreads = 30;

		if (args.length < 1) {
			System.err.println("arg1: consensus file list");
			System.exit(1);
		}

		System.err.println("Start CheckHeteroChrX ver. 2015061101");

		try {
			FileOpen fo = new FileOpen(args[0]);
			ArrayList<String> filelist = new ArrayList<String>();
			String filename = new String();
			while ((filename = fo.getBufferedReader().readLine()) != null) {
				filelist.add(filename);
			}
			fo.close();

			int numFiles = filelist.size() / numThreads;

			ArrayList<String> f = new ArrayList<String>();
			for(String fname : filelist){
				f.add(fname);
				if(f.size() >= numFiles){
					CheckHeteroChrX checkHeteroChrX = new CheckHeteroChrX(f);
					checkHeteroChrX.start();
					threads.add(checkHeteroChrX);
					f = new ArrayList<String>();
				}
				//				if(threads.size()>=30){
				//					for(CheckHeteroChrX thread: threads){
				//						thread.join();
				//						System.out.println(thread.getValue());
				//					}
				//					threads = new ArrayList<CheckHeteroChrX>();
				//				}
			}
			if(f.size() >= 0){
				CheckHeteroChrX checkHeteroChrX = new CheckHeteroChrX(f);
				checkHeteroChrX.start();
				threads.add(checkHeteroChrX);
			}
			for(CheckHeteroChrX thread: threads){
				thread.join();
				System.out.print(thread.getValue());
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}