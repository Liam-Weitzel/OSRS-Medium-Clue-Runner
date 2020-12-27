package package;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class FetchGE {

	private static URLConnection con;
	private static InputStream is;
	private static InputStreamReader isr;
	private static BufferedReader br;

	public static int getCurrentPrice_official(int itemID) {
		try {
			URL url = new URL("https://secure.runescape.com/m=itemdb_oldschool/api/catalogue/detail.json?item="+itemID);
			con = url.openConnection();
			is = con.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line = br.readLine();
			int itemindex = line.indexOf(",\"current\":");
			int begginingofpriceofficial = line.indexOf(",\"price\":", itemindex);
			int endofprice = line.indexOf("}", begginingofpriceofficial);
			int begginningofpricereal = begginingofpriceofficial+9;
			String price = line.substring(begginningofpricereal, endofprice);
			if (price.contains("k")) {
				String price6 = price.substring(1, price.length()-2);
				int p2 = price6.indexOf(".");
				String pricefirstpart = price6.substring(0, p2);
				String pricesecondpart = price6.substring(p2 + 1);
				String pricefinal = pricefirstpart + pricesecondpart + "00";
				return Integer.parseInt(pricefinal);
			} if (price.contains("m")) {
				String price6 = price.substring(1, price.length()-2);
				int p2 = price6.indexOf(".");
				String pricefirstpart = price6.substring(0, p2);
				String pricesecondpart = price6.substring(p2 + 1);
				String pricefinal = pricefirstpart + pricesecondpart + "00000";
				return Integer.parseInt(pricefinal);
			} else if (price.contains(",")) {
				int p = price.indexOf(",");
				String price2 = price.substring(0, p) + price.substring(p + 1);
				String price3 = price2.substring(1, price2.length()-1);
				return Integer.parseInt(price3);
			} else {
				return Integer.parseInt(price);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				} else if (isr != null) {
					isr.close();
				} else if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
	
	public static int getOverallAverage_rsbuddy(int itemID) {
		try {
			URL url = new URL("https://rsbuddy.com/exchange/summary.json");
			con = url.openConnection();
			is = con.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line = br.readLine();
			if (line.indexOf("\"id\":" + itemID + ",\"name\":\"") != -1) {
				int itemindex = line.indexOf("\"id\":" + itemID + ",\"name\":\"");
				int begginingofprice = line.indexOf("\"overall_average\":", itemindex);
				int endofprice = line.indexOf(",", begginingofprice);
				int begginningofpricereal = begginingofprice+18;
				String price = line.substring(begginningofpricereal, endofprice);
				return Integer.parseInt(price);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				} else if (isr != null) {
					isr.close();
				} else if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
	
	public static String getMembers_rsbuddy(int itemID) {
		try {
			URL url = new URL("https://rsbuddy.com/exchange/summary.json");
			con = url.openConnection();
			is = con.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line = br.readLine();
			if (line.indexOf("\"id\":" + itemID + ",\"name\":\"") != -1) {
				int itemindex = line.indexOf("\"id\":" + itemID + ",\"name\":\"");
				int begginingofmembers = line.indexOf("\"members\":", itemindex);
				int endofmembers = line.indexOf(",", begginingofmembers);
				int begginningofmembersreal = begginingofmembers+10;
				String members = line.substring(begginningofmembersreal, endofmembers);
				return members;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				} else if (isr != null) {
					isr.close();
				} else if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static int getSP_rsbuddy(int itemID) {
		try {
			URL url = new URL("https://rsbuddy.com/exchange/summary.json");
			con = url.openConnection();
			is = con.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line = br.readLine();
			if (line.indexOf("\"id\":" + itemID + ",\"name\":\"") != -1) {
				int itemindex = line.indexOf("\"id\":" + itemID + ",\"name\":\"");
				int begginingofsp = line.indexOf("\"sp\":", itemindex);
				int endofsp = line.indexOf(",", begginingofsp);
				int begginningofspreal = begginingofsp+5;
				String sp = line.substring(begginningofspreal, endofsp);
				return Integer.parseInt(sp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				} else if (isr != null) {
					isr.close();
				} else if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
	
	public static int getBuyAverage_rsbuddy(int itemID) {
		try {
			URL url = new URL("https://rsbuddy.com/exchange/summary.json");
			con = url.openConnection();
			is = con.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line = br.readLine();
			if (line.indexOf("\"id\":" + itemID + ",\"name\":\"") != -1) {
				int itemindex = line.indexOf("\"id\":" + itemID + ",\"name\":\"");
				int begginingofbuyaverage = line.indexOf("\"buy_average\":", itemindex);
				int endofbuyaverage = line.indexOf(",", begginingofbuyaverage);
				int begginningofbuyaveragereal = begginingofbuyaverage+14;
				String buyaverage = line.substring(begginningofbuyaveragereal, endofbuyaverage);
				return Integer.parseInt(buyaverage);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				} else if (isr != null) {
					isr.close();
				} else if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
	
	public static int getBuyQuantity_rsbuddy(int itemID) {
		try {
			URL url = new URL("https://rsbuddy.com/exchange/summary.json");
			con = url.openConnection();
			is = con.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line = br.readLine();
			if (line.indexOf("\"id\":" + itemID + ",\"name\":\"") != -1) {
				int itemindex = line.indexOf("\"id\":" + itemID + ",\"name\":\"");
				int begginingofbuyquantity = line.indexOf("\"buy_quantity\":", itemindex);
				int endofbuyquantity = line.indexOf(",", begginingofbuyquantity);
				int begginningofbuyquantityreal = begginingofbuyquantity+15;
				String buyquantity = line.substring(begginningofbuyquantityreal, endofbuyquantity);
				return Integer.parseInt(buyquantity);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				} else if (isr != null) {
					isr.close();
				} else if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
	
	public static int getSellAverage_rsbuddy(int itemID) {
		try {
			URL url = new URL("https://rsbuddy.com/exchange/summary.json");
			con = url.openConnection();
			is = con.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line = br.readLine();
			if (line.indexOf("\"id\":" + itemID + ",\"name\":\"") != -1) {
				int itemindex = line.indexOf("\"id\":" + itemID + ",\"name\":\"");
				int begginingofsellaverage = line.indexOf("\"sell_average\":", itemindex);
				int endofsellaverage = line.indexOf(",", begginingofsellaverage);
				int begginningofsellaveragereal = begginingofsellaverage+15;
				String sellaverage = line.substring(begginningofsellaveragereal, endofsellaverage);
				return Integer.parseInt(sellaverage);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				} else if (isr != null) {
					isr.close();
				} else if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
	
	public static int getSellQuantity_rsbuddy(int itemID) {
		try {
			URL url = new URL("https://rsbuddy.com/exchange/summary.json");
			con = url.openConnection();
			is = con.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line = br.readLine();
			if (line.indexOf("\"id\":" + itemID + ",\"name\":\"") != -1) {
				int itemindex = line.indexOf("\"id\":" + itemID + ",\"name\":\"");
				int begginingofsellquantity = line.indexOf("\"sell_quantity\":", itemindex);
				int endofsellquantity = line.indexOf(",", begginingofsellquantity);
				int begginningofsellquantityreal = begginingofsellquantity+16;
				String sellquantity = line.substring(begginningofsellquantityreal, endofsellquantity);
				return Integer.parseInt(sellquantity);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				} else if (isr != null) {
					isr.close();
				} else if (is != null) {
					is.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 0;
	}
}