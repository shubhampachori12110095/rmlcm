package com.rapidminer.lcm.io;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import com.rapidminer.lcm.exceptions.WrongDatabasePathException;
import com.rapidminer.lcm.obj.ResultListIOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;

public class RMSQLiteWriter extends Operator {

	private static final String dbname = "database path";
	private static final String tablename = "table name";

	private InputPort input = this.getInputPorts().createPort("input");

	private int tbsize;

	public RMSQLiteWriter(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {

		ResultListIOObject result = input.getData(ResultListIOObject.class);

		Connection c = null;

		String nameofdb = this.getParameter(dbname);

		connectDB(c, nameofdb);

		String nameoftb = this.getParameter(tablename);

		String nameoftbFP = nameoftb + "FP";

		String nameoftbITEM = nameoftb + "ITEM";

		tbsize = this.lenthofLongestPattern(result);

		createTables(c, nameofdb, nameoftbFP, nameoftbITEM);
		insertValues(c, nameofdb, nameoftbFP, nameoftbITEM, result);
		// createTable(c, nameofdb, nameoftb, tbsize);

		// insert(c, nameofdb, nameoftb, result, tbsize);

		// commitAndclose(c);

		// String table = "RESULT";

	}

	public void connectDB(Connection c, String nameofdb) {

		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:" + nameofdb + ".db");
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found!");
			e.printStackTrace();
		} catch (SQLException e) {
			System.err.println("Connection error!");
			e.printStackTrace();
		}
		System.out.println("Opened database successfully!");
	}

	public void createTables(Connection c, String nameofdb, String nameoftbfp,
			String nameoftbitem) throws WrongDatabasePathException {

		Statement stmt = null;
		
		try {
			c = DriverManager.getConnection("jdbc:sqlite:" + nameofdb + ".db");
		} catch (SQLException e1) {
			throw new WrongDatabasePathException("Please check your database path!");
			//e1.printStackTrace();
		}
		
		try {

			stmt = c.createStatement();

			String fbsql = "DROP TABLE IF EXISTS " + nameoftbfp + ";";

			stmt.execute(fbsql);

			String itemsql = "DROP TABLE IF EXISTS " + nameoftbitem + ";";

			stmt.execute(itemsql);

			String fptbsql = "CREATE TABLE IF NOT EXISTS "
					+ nameoftbfp
					+ " (PATTERNID INTEGER PRIMARY KEY  NOT NULL, SUPPORT INT NOT NULL, PATTERNDESCRIPTION TEXT NOT NULL);";

			stmt.execute(fptbsql);

			String itemtbsql = "CREATE TABLE IF NOT EXISTS "
					+ nameoftbitem
					+ "(ITEM INT NOT NULL,PATTERNID INT NOT NULL, FOREIGN KEY(PATTERNID) REFERENCES "
					+ nameoftbfp + "(PATTERNID));";

			// System.out.println(itemtbsql);

			stmt.execute(itemtbsql);

			stmt.close();
			c.close();

		} catch (SQLException e) {
			System.err.println("jdbc get connection error !!!");
			e.printStackTrace();
		}
	}

	public void insertValues(Connection c, String nameofdb, String nameoftbfp,
			String nameoftbitem, ResultListIOObject result) {

		Statement stmt = null;
		try {
			c = DriverManager.getConnection("jdbc:sqlite:" + nameofdb + ".db");

			c.setAutoCommit(false);

			stmt = c.createStatement();

			String sql = null;

			int fpid = 1;

			for (int[] row : result.getResultlist()) {

				int support = row[0];
				int[] pattern = Arrays.copyOfRange(row, 1, row.length - 1);

				String patternString = Arrays.toString(pattern);

				// System.out.println(support + " " + patternString);

				sql = "INSERT INTO " + nameoftbfp
						+ " (PATTERNID,SUPPORT,PATTERNDESCRIPTION) VALUES ("
						+ fpid + "," + support + ",('" + patternString + "'));";

				// System.out.println(sql);

				stmt.execute(sql);

				// int itemid = 1;
				for (int i : pattern) {
					String innersql = "INSERT INTO " + nameoftbitem
							+ " (ITEM,PATTERNID) VALUES (" + i + "," + fpid
							+ ");";
					stmt.execute(innersql);
				}

				fpid++;
			}

			stmt.close();

			c.commit();
			c.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void createTable(Connection c, String nameofdb, String nameoftb,
			int rowsize) {

		Statement stmt = null;

		try {

			// c = DriverManager.getConnection("jdbc:sqlite:" + nameofdb +
			// ".db");
			c = DriverManager.getConnection("jdbc:sqlite:" + nameofdb + ".db");
			// System.out.println("Opened database successfully");

			stmt = c.createStatement();

			StringBuilder items = new StringBuilder();

			for (int i = 0; i < rowsize - 2; i++) {
				if (i != rowsize - 3) {
					items.append("ITEM" + (i + 1) + " INT DEFAULT NULL, ");
				} else {
					items.append("ITEM" + (i + 1) + " INT DEFAULT NULL");
				}
			}

			// DatabaseMetaData dbm = c.getMetaData();
			//
			// ResultSet tables = dbm.getTables(null, null, nameoftb, null);
			//
			// if (tables.next()) {
			// // nameoftb = "new_" + nameoftb;
			// String dorp = "DROP TABLE " + nameoftb;
			// stmt.executeUpdate(dorp);
			// }

			String dorp = "DROP TABLE IF EXISTS " + nameoftb + ";";
			stmt.executeUpdate(dorp);

			String sql = "CREATE TABLE IF NOT EXISTS " + nameoftb
					+ " (ID INTEGER PRIMARY KEY  NOT NULL, "
					+ "SUPPORT INT NOT NULL, " + items.toString() + ");";

			// System.out.println("-- :" + sql);

			stmt.executeUpdate(sql);
			stmt.close();
			c.close();

			System.out.println("Create Table OK");
		} catch (SQLException e) {
			System.err.println("Create statement error!");
			e.printStackTrace();
		}

	}

	public void insert(Connection c, String nameofdb, String nameoftb,
			ResultListIOObject result, int rowsize) {

		Statement stmt = null;
		try {
			c = DriverManager.getConnection("jdbc:sqlite:" + nameofdb + ".db");
			c.setAutoCommit(false);

			stmt = c.createStatement();

			String sql = null;

			StringBuilder items = new StringBuilder();

			for (int i = 0; i < rowsize - 2; i++) {
				if (i != rowsize - 3) {
					items.append("ITEM" + (i + 1) + ",");
				} else {
					items.append("ITEM" + (i + 1));
				}
			}

			int id = 0;

			StringBuilder sbvalues = new StringBuilder();

			String[] values = new String[rowsize - 1];
			Arrays.fill(values, null);

			for (int[] list : result.getResultlist()) {

				// for (int i = 0; i < list.length-1; i++) {
				// System.out.print("** " + list[i] + " ");
				// }

				for (int j = 0; j < values.length; j++) {

					if (j != values.length - 1) {
						if (j < list.length - 1) {
							sbvalues.append(String.valueOf(list[j]));
							sbvalues.append(",");
						} else {
							sbvalues.append(values[j]);
							sbvalues.append(",");
						}
					} else {
						if (j == list.length - 2) {
							sbvalues.append(list[j]);
						} else {
							sbvalues.append(values[j]);
						}
					}
				}

				sql = "INSERT INTO " + nameoftb + "(ID,SUPPORT,"
						+ items.toString() + ") " + "VALUES (" + id + ","
						+ sbvalues.toString() + ");";

				Arrays.fill(values, null);
				sbvalues.setLength(0);
				// System.out.println("++ " + sql);
				stmt.executeUpdate(sql);

				id = id + 1;
			}

			stmt.close();

			c.commit();
			c.close();
		} catch (SQLException e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			// System.exit(0);
			System.err.println("set auto-commit false failed!");
			e.printStackTrace();
		}

	}

	public int lenthofLongestPattern(ResultListIOObject result) {
		int length = 0;
		for (int[] item : result.getResultlist()) {
			if (item.length > length) {
				length = item.length;
			}
		}
		return length;
	}

	public void commitAndclose(Connection c) {
		try {

			c.commit();
			c.close();
		} catch (SQLException e) {
			System.err.println("commit and close failed!");
			e.printStackTrace();
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(new ParameterTypeString(dbname, "Name of Database",
				"D:\\database", false));

		types.add(new ParameterTypeString(tablename, "Name of table", "tbl",
				false));

		return types;
	}
}
