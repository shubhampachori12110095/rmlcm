package com.rapidminer.lcm.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv.CSVImportWizard.CSVDataReaderWizardCreator;
import com.rapidminer.lcm.internals.transactions.RMTransaction;
import com.rapidminer.lcm.internals.transactions.RMTransactions;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;

public class RMReader extends Operator {

	private static final String FILE_LOCATION = "file";

	private static final String testFile = "test";

	private static final String useRegex = "Special Separator";
	private static final String regex = "regex";

	// private InputPort prinput = this.getInputPorts().createPort("in");
	private OutputPort proutput = this.getOutputPorts().createPort("out");
	private OutputPort stdoutput = this.getOutputPorts().createPort("data");

	private RMTransactions transactions;
	private RMTransaction transaction;

	private List<Integer> lengths;

	// private ArrayList<E>

	public RMReader(OperatorDescription description) {
		super(description);
	}

	public void readFile() {
		// transactions = new ArrayList<RMTransaction>();
		String fileLocation = null;
		lengths = new ArrayList<Integer>();
		// try {
		// File newfile = this.getParameterAsFile(FILE_LOCATION);
		// System.out.println(fileLocation + " file location");
		// } catch (UndefinedParameterError e1) {
		// System.out.println("Undefined parameter!");
		// e1.printStackTrace();
		// } catch (UserError e) {
		// System.err.println("no such file error!");
		// e.printStackTrace();
		// }

		// File file = new File(fileLocation);
		// File file =this.getParameterAsFile(FILE_LOCATION);
		// BufferedInputStream bufferInput = null;
		BufferedInputStream bufferInput = null;
		try {
			File file = this.getParameterAsFile(FILE_LOCATION);
			
			bufferInput = new BufferedInputStream(new FileInputStream(file),
					10 * 1024 * 1024);
		} catch (FileNotFoundException e) {
			System.err.println("no such file!");
			e.printStackTrace();
		} catch (UserError e) {
			System.err.println("please check your file type!");
			e.printStackTrace();
		}
		BufferedReader input = new BufferedReader(new InputStreamReader(
				bufferInput));

		String line;
		transactions = new RMTransactions();

		try {

			boolean startUseRegex = this.getParameterAsBoolean(useRegex);

			String lineRegex = "\\s";

			if (startUseRegex) {
				while ((line = input.readLine()) != null) {
					// Pattern pattern = Pattern.compile();
					if (lineRegex.isEmpty() || lineRegex.equals(null)
							|| lineRegex == null) {
						lineRegex = "\\s";
					} else {
						lineRegex = this.getParameterAsString(regex);
					}
					String[] newline = this.splitTransaction(line, lineRegex);
					// String[] newline = line.split("\\s");
					transaction = new RMTransaction(newline);
					lengths.add(transaction.size());
					transactions.add(transaction);
				}
			} else {
				while ((line = input.readLine()) != null) {
					String[] newline = line.split("\\s");
					transaction = new RMTransaction(newline);
					lengths.add(transaction.size());
					transactions.add(transaction);
				}
				// System.out.println("____________________________");
			}

		} catch (IOException e) {
			System.err.println("can't read this line!");
			e.printStackTrace();
		} catch (UndefinedParameterError e) {
			System.err.println("parameter undefined!");
			e.printStackTrace();
		}
	}

	@Override
	public void doWork() throws OperatorException {
		this.readFile();
		stdoutput.deliver(this.showOriginalData(this.transactions));
		proutput.deliver(this.transactions);
	}

	/**
	 * @param transactions
	 */
	public ExampleSet showOriginalData(RMTransactions transactions) {
		// List<Attribute> attributes = new LinkedList<Attribute>();
		Attribute[] attributes = new Attribute[this
				.getLengthOfLongestTransaction(transactions)];

		for (int i = 0; i < this.getLengthOfLongestTransaction(transactions); i++) {
			attributes[i] = AttributeFactory.createAttribute("att" + i,
					Ontology.INTEGER);
			// attributes.add(AttributeFactory.createAttribute("att" + i,
			// Ontology.INTEGER));
		}

		// create table
		MemoryExampleTable table = new MemoryExampleTable(attributes);

		DataRowFactory ROW_FACTORY = new DataRowFactory(0, '.');
		// fill table (here : only integer values )
		for (int i = 0; i < transactions.getTransactions().size(); i++) {
			Integer[] data = new Integer[attributes.length];
			Arrays.fill(data, null);
			for (int j = 0; j < transactions.getTransactions().get(i).size(); j++) {
				data[j] = Integer.valueOf(transactions.getTransactions().get(i)
						.get(j));
			}
			DataRow dataRow = ROW_FACTORY.create(data, attributes);
			table.addDataRow(dataRow);
		}
		ExampleSet resultExampleSet = table.createExampleSet();

		return resultExampleSet;
	}

	public int getLengthOfLongestTransaction(RMTransactions transactions) {
		Collections.sort(this.lengths);
		return lengths.get(lengths.size() - 1);
	}

	public String[] splitTransaction(String dataLine, String regex) {
		String transactionLine[];
		transactionLine = dataLine.split(regex);
		return transactionLine;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		// ParameterType type = new ParameterTypeConfiguration(
		// CSVDataReaderWizardCreator.class, this);
		// type.setExpert(false);
		// types.add(type);
		//
		// types.add(new ParameterTypeFile(testFile, "test", "txt", false));

		types.add(new ParameterTypeBoolean(useRegex,
				"Use sepecial regex of separator", false));

		ParameterType regexMatcher = new ParameterTypeString(regex,
				"the regex of separators which you want to match", null, false);

		types.add(new ParameterTypeFile(
				FILE_LOCATION,
				"this parameter defines the location of file which you want to read.",
				"txt", false));

		regexMatcher.registerDependencyCondition(new BooleanParameterCondition(
				this, useRegex, true, true));

		types.add(regexMatcher);

		return types;
	}
}
