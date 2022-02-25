package edu.uw.villenlab.isobaricquant;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

public class ProductDataSheet implements Comparable<ProductDataSheet> {

	private int id;
	private String name;
	private int userID;
	private Date creationDate;
	private List<TagInfo> table;
	private double[][] matrix;
	private String encodedTable;
	private InputStream csvInputStream;
	Map<String, Integer> dataSheetTagIndexMap;

	public class TagInfo {

		public String id;
		public double reporterIon;
		public String minus2; // -2
		public String minus1; // -1
		public String monoisotopic;
		public String plus1; // +1
		public String plus2; // +2
	}

	public ProductDataSheet(int id, int userID, String name, Date creationDate) {
		this.id = id;
		this.name = name;
		this.userID = userID;
		this.creationDate = creationDate;
		this.encodedTable = null;
		this.dataSheetTagIndexMap = null;
		this.table = null;
		this.matrix = null;
	}

	public ProductDataSheet(String name, int userID, String data) {
		this.name = name;
		this.userID = userID;
		this.encodedTable = data;
		this.dataSheetTagIndexMap = null;
		this.table = null;
		this.matrix = null;
	}

	public void setContent(String data) {
		this.encodedTable = data;
		this.dataSheetTagIndexMap = null;
		this.table = null;
		this.table = null;
		this.matrix = null;
	}

	public void setCSVContent(InputStream in) {
		this.csvInputStream = in;
		this.dataSheetTagIndexMap = null;
		this.table = null;
		this.table = null;
		this.matrix = null;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	private void decodeCSV(InputStream csvInputStream) throws FileNotFoundException, IOException {
		HashMap<String, String> dataSheetMap = new HashMap<>();
		String aux;
		int size = 0;
		int i = 1;
		boolean found = true;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(csvInputStream))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] lTagMap = line.split(",");
				dataSheetMap.put(lTagMap[0], lTagMap[1]);
			}

			table = new ArrayList<>();
			TagInfo tinfo;

			while (found) {
				aux = dataSheetMap.getOrDefault("Tag" + i + "_tag", "");
				if (aux.isEmpty()) {
					found = false;
				} else {
					tinfo = new TagInfo();
					tinfo.id = aux;
					tinfo.minus1 = dataSheetMap.getOrDefault("Tag" + i + "_m1", "0");
					tinfo.minus2 = dataSheetMap.getOrDefault("Tag" + i + "_m2", "0");
					tinfo.plus1 = dataSheetMap.getOrDefault("Tag" + i + "_p1", "0");
					tinfo.plus2 = dataSheetMap.getOrDefault("Tag" + i + "_p2", "0");
					tinfo.monoisotopic = dataSheetMap.getOrDefault("Tag" + i + "_monoisotopic", "100%");
					tinfo.reporterIon = Double.valueOf(dataSheetMap.getOrDefault("Tag" + i + "_reporter_ion", "0.0"));
					table.add(tinfo);
					++i;
					++size;
				}
			}
		}
	}

	private void decode(String dataSheet) {
		Map<String, Integer> dataSheetTagIndexMap = new HashMap<>();
		String aux;
		int size = 0;
		int i = 1;
		boolean found = true;

		// Using configuration file to store/retrieve data
		//ConfigFile cFile = new ConfigFile(-1, "", "", dataSheet);
		HashMap<String, String> dataSheetMap = new HashMap<>();
		String labelTags[] = dataSheet.split("\\r?\\n");
		for (String lTag : labelTags) {
			System.out.println(lTag);
			String lTagMap[] = lTag.split(",");
			dataSheetMap.put(lTagMap[0], lTagMap[1]);
		}

		table = new ArrayList<>();
		TagInfo tinfo;

		while (found) {
			aux = dataSheetMap.getOrDefault("Tag" + i + "_tag", "");
			if (aux.isEmpty()) {
				found = false;
			} else {
				tinfo = new TagInfo();
				tinfo.id = aux;
				tinfo.minus1 = dataSheetMap.getOrDefault("Tag" + i + "_m1", "0");
				tinfo.minus2 = dataSheetMap.getOrDefault("Tag" + i + "_m2", "0");
				tinfo.plus1 = dataSheetMap.getOrDefault("Tag" + i + "_p1", "0");
				tinfo.plus2 = dataSheetMap.getOrDefault("Tag" + i + "_p2", "0");
				tinfo.monoisotopic = dataSheetMap.getOrDefault("Tag" + i + "_monoisotopic", "100%");
				tinfo.reporterIon = Double.valueOf(dataSheetMap.getOrDefault("Tag" + i + "_reporter_ion", "0.0"));
				table.add(tinfo);
				++i;
				++size;
			}
		}

	}

	private void encode() {

		TagInfo tinfo;

		// Using configuration file to store/retrieve data
		//ConfigFile cFile = new ConfigFile(-1, "", "", "");
		StringBuilder encodeDataSheet = new StringBuilder();

		for (int i = 1; i <= table.size(); ++i) {
			tinfo = table.get(i - 1);

			if (encodeDataSheet.length() != 0) {
				encodeDataSheet.append("\\r\\n");
			}

			encodeDataSheet.append("Tag" + i + "_tag," + tinfo.id + "\\r\\n");
			encodeDataSheet.append("Tag" + i + "_reporter_ion," + tinfo.reporterIon + "\\r\\n");
			encodeDataSheet.append("Tag" + i + "_monoisotopic," + tinfo.monoisotopic + "\\r\\n");
			encodeDataSheet.append("Tag" + i + "_m1," + tinfo.minus1 + "\\r\\n");
			encodeDataSheet.append("Tag" + i + "_m2," + tinfo.minus2 + "\\r\\n");
			encodeDataSheet.append("Tag" + i + "_p1," + tinfo.plus1 + "\\r\\n");
			encodeDataSheet.append("Tag" + i + "_p2," + tinfo.plus2);
		}

		encodedTable = encodeDataSheet.toString();
	}

	private void calcMatrix() {
		int size = table.size();
		dataSheetTagIndexMap = new HashMap<>();

		for (int i = 0; i < size; ++i) {
			dataSheetTagIndexMap.put(table.get(i).id, i);
		}

		matrix = new double[size][size];

		TagInfo tinfo;
		double perc;

		for (int i = 0; i < size; ++i) {
			tinfo = table.get(i);
			perc = Double.parseDouble(tinfo.monoisotopic.replace("%", ""));
			// Equation format: obs_int = x + y + z --> 1x + 0.5y + 3.2z = 1253.2
			matrix[i][i] = perc / 100.0;
			setPercentageInMatrix(tinfo.minus2, i);
			setPercentageInMatrix(tinfo.minus1, i);
			setPercentageInMatrix(tinfo.plus1, i);
			setPercentageInMatrix(tinfo.plus2, i);
		}

	}

	private void calcMatrix(String[] tagIDOrder) {
		int size = table.size();
		dataSheetTagIndexMap = new HashMap<>();

		// set tag order in a map to access location easily
		for (int i = 0; i < tagIDOrder.length; ++i) {
			dataSheetTagIndexMap.put(tagIDOrder[i], i);
		}

		// sort current table to the specified order
		List<TagInfo> sortedTable = new ArrayList<>();
		for (int i = 0; i < tagIDOrder.length; ++i) {
			boolean found = false;
			for (int j = 0; !found && j < table.size(); ++j) {
				if (table.get(j).id.equals(tagIDOrder[i])) {
					found = true;
					sortedTable.add(table.get(j));
				}
			}
		}

		matrix = new double[size][size];

		TagInfo tinfo;
		double perc;

		for (int i = 0; i < size; ++i) {
			tinfo = sortedTable.get(i);
			perc = Double.parseDouble(tinfo.monoisotopic.replace("%", ""));
			// Equation format: obs_int = x + y + z --> 1x + 0.5y + 3.2z = 1253.2
			matrix[i][i] = perc / 100.0;
			setPercentageInMatrix(tinfo.minus2, i);
			setPercentageInMatrix(tinfo.minus1, i);
			setPercentageInMatrix(tinfo.plus1, i);
			setPercentageInMatrix(tinfo.plus2, i);
		}

	}

	private void setPercentageInMatrix(String ionPerc, int tagIndex) {
		String aux, percTag[];
		double perc;
		int index;
		if (ionPerc.contains("(")) {
			percTag = ionPerc.split("\\(");
			aux = percTag[1].replace(")", "");
			perc = Double.parseDouble(percTag[0].replace("%", ""));
			index = dataSheetTagIndexMap.get(aux);
			matrix[index][tagIndex] = perc / 100;
		}
	}

	private void printMatrices(double[] coef, double[] res) {
		// print matrices
		System.out.println("---- Matrix");
		for (int i = 0; i < matrix.length; ++i) {
			for (int j = 0; j < matrix[i].length; ++j) {
				System.out.print(matrix[i][j] + ",");
			}
			System.out.println("");
		}
		System.out.println("---- Coef");
		for (int i = 0; i < coef.length; ++i) {
			System.out.println(coef[i]);
		}
		System.out.println("---- Result");
		for (int i = 0; i < res.length; ++i) {
			System.out.println(res[i]);
		}
	}

	public double[][] getMatrix() throws IOException {
		if (table == null) {
			//decode(encodedTable);
			decodeCSV(csvInputStream);
		}
		if (matrix == null) {
			calcMatrix();
		}
		return matrix;
	}

	public void prepareMatrix(String[] tagIDOrder) throws IOException {
		if (table == null) {
			//printEncodedTable();
			//decode(encodedTable);
			decodeCSV(csvInputStream);
			//printDecodedTable();
		}
		if (matrix == null) {
			System.out.println("Preparing matrix wih specific order...");
			calcMatrix(tagIDOrder);
		}
	}

	public void printMatrix(String[] tagIDOrder) {
		printMatrix(tagIDOrder, matrix);
	}

	public void printMatrix(String[] tagIDOrder, double[][] matrix) {
		// print matrices
		System.out.println("---- Matrix");
		for (int i = 0; i < matrix.length; ++i) {
			System.out.print(tagIDOrder[i] + ":");
			for (int j = 0; j < matrix[i].length; ++j) {
				System.out.print(matrix[i][j] + ",");
			}
			System.out.println("");
		}
	}

	private void printMatrix(double[][] matrix) {
		// print matrices
		System.out.println("---- Matrix");
		for (int i = 0; i < matrix.length; ++i) {
			for (int j = 0; j < matrix[i].length; ++j) {
				System.out.print(matrix[i][j] + ",");
			}
			System.out.println("");
		}
	}

	public void printEncodedTable() {
		System.out.println("-------- Encoded table");
		System.out.println(encodedTable);
	}

	public void printDecodedTable() {
		System.out.println("------- Decoded data sheet table");
		for (TagInfo tinfo : table) {
			System.out.println(tinfo.id + "\t" + tinfo.reporterIon + "\t" + tinfo.minus2
				+ "\t" + tinfo.minus1 + "\t" + tinfo.monoisotopic + "\t" + tinfo.plus1 + "\t" + tinfo.plus2);
		}
	}

	public double[] calcIsotopicDistributionIntensities(double[] reportedIntensities) {
		return calcIsotopicDistributionIntensities(reportedIntensities, false);
	}

	public double[] calcIsotopicDistributionIntensities(double[] reportedIntensities, boolean print) {
		double[][] recalcMatrix = new double[matrix.length][matrix[0].length];
		for (int i = 0; i < matrix.length; ++i) {
			if (reportedIntensities[i] != 0) {
				for (int j = 0; j < matrix[i].length; ++j) {
					recalcMatrix[i][j] = matrix[i][j];
				}
			} else {
				for (int j = 0; j < matrix[i].length; ++j) {
					recalcMatrix[i][j] = 0;
				}
			}
			recalcMatrix[i][i] = 1;
		}
		try {
			RealMatrix coefficients = new Array2DRowRealMatrix(recalcMatrix, false);
			DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
			RealVector constants = new ArrayRealVector(reportedIntensities, false);
			RealVector solution = solver.solve(constants);
			if (print) {
				printMatrices(reportedIntensities, solution.toArray());
				printMatrix(recalcMatrix);
			}
			return solution.toArray();
		} catch (Exception ex) {
			printMatrix(recalcMatrix);
			System.out.println(ex.getMessage());
			System.out.println("Matrix could not be solved!, using default masses...");
			return null;
		}
	}

	public String getEncodedData() {
		if (encodedTable == null) {
			encode();
		}
		return encodedTable;
	}

	@Override
	public int compareTo(ProductDataSheet o) {
		return Integer.compare(o.id, id); //Newer first
	}

}
