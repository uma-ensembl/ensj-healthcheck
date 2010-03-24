/*
 * Copyright (C) 2004 EBI, GRL
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this library; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 */

package org.ensembl.healthcheck.testcase.eg_core;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.DatabaseType;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.testcase.Repair;
import org.ensembl.healthcheck.util.CollectionUtils;
import org.ensembl.healthcheck.util.DBUtils;

/**
 * An EnsEMBL Healthcheck test case which checks that the protein_feature table
 * agrees with the translation table. Modified for use with EnsemblGenomes to
 * support seq_edits
 */

public class EgProteinFeatureTranslation extends AbstractEgCoreTestCase
		implements Repair {

	// hash of lists of protein features to delete
	// key - database name
	private Map<String, List<Integer>> featuresToDelete = CollectionUtils.createHashMap();

	private static int THRESHOLD = 1000; // don't report a problem if there

	// are less results than this

	/**
	 * Create an ProteinFeatureTranslationTestCase that applies to a specific
	 * set of databases.
	 */
	public EgProteinFeatureTranslation() {
		super();
		setFailureText("Large numbers of features longer than the translation indicate something is wrong. A few is probably OK");
		setHintLongRunning(true);
	}

	/**
	 * This test only applies to core and Vega databases.
	 */
	public void types() {
		removeAppliesToType(DatabaseType.OTHERFEATURES);
		removeAppliesToType(DatabaseType.ESTGENE);
		removeAppliesToType(DatabaseType.CDNA);
	}

	/**
	 * Builds a cache of the translation lengths, then compares them with the
	 * values in the protein_features table.
	 * 
	 * @param dbre
	 *            The database to use.
	 * @return Result.
	 */

	public boolean runTest(DatabaseRegistryEntry dbre) {

		boolean result = true;

		// get list of transcripts
		String sql = "SELECT t.transcript_id, e.exon_id, tl.start_exon_id, "
				+ "       tl.translation_id, tl.end_exon_id, tl.seq_start, "
				+ "       tl.seq_end, e.seq_region_start, e.seq_region_end "
				+ "FROM   transcript t, exon_transcript et, exon e, translation tl "
				+ "WHERE  t.transcript_id = et.transcript_id "
				+ "AND    et.exon_id = e.exon_id "
				+ "AND    t.transcript_id = tl.transcript_id "
				+ "ORDER  BY t.transcript_id, et.rank";

		String sqlSeqEdit = "SELECT ta.translation_id,ta.value FROM translation_attrib ta where ta.attrib_type_id=144";

		try {

			Connection con = dbre.getConnection();

			// check that the protein feature table actually has some rows - if
			// not there's
			// no point working out the translation lengths
			if (!tableHasRows(con, "protein_feature")) {
				ReportManager.problem(this, con,
						"protein_feature table is empty");
				return false; // shoud we return true or false in this case?
			}

			// NOTE: By default the MM MySQL JDBC driver reads and stores *all*
			// rows in the
			// ResultSet.
			// Since this TestCase is likely to produce lots of output, we must
			// use the
			// "streaming"
			// mode where only one row of the ResultSet is stored at a time.
			// To do this, the following two lines are both necessary.
			// See the README file for the mm MySQL driver.

			Statement stmt = con.createStatement(
					java.sql.ResultSet.TYPE_FORWARD_ONLY,
					java.sql.ResultSet.CONCUR_READ_ONLY);
			stmt.setFetchSize(1000);

			Map<Integer, Integer> translationLengths = CollectionUtils
					.createHashMap();

			// now calculate and store the translation lengths
			ResultSet rs = stmt.executeQuery(sql);
			rs.setFetchDirection(ResultSet.FETCH_FORWARD);

			boolean inCodingRegion = false;

			while (rs.next()) {

				int currentTranslationID = rs.getInt("translation_id");
				Integer id = new Integer(currentTranslationID);
				// initialise if necessary
				if (translationLengths.get(id) == null) {
					translationLengths.put(id, new Integer(0));
				}

				if (!inCodingRegion) {
					if (rs.getInt("start_exon_id") == rs.getInt("exon_id")) {
						// single-exon-translations
						if (rs.getInt("start_exon_id") == rs
								.getInt("end_exon_id")) {
							int length = (rs.getInt("seq_end") - rs
									.getInt("seq_start")) + 1;
							translationLengths.put(id, new Integer(length));
							continue;
						}
						inCodingRegion = true;
						// subtract seq_start
						int currentLength = ((Integer) translationLengths
								.get(id)).intValue();
						currentLength -= (rs.getInt("seq_start") - 1);
						translationLengths.put(id, new Integer(currentLength));
					}
				} // if !inCoding

				if (inCodingRegion) {
					if (rs.getInt("exon_id") == rs.getInt("end_exon_id")) {
						// add seq_end
						int currentLength = ((Integer) translationLengths
								.get(id)).intValue();
						currentLength += rs.getInt("seq_end");
						translationLengths.put(id, new Integer(currentLength));
						inCodingRegion = false;
					} else {
						int currentLength = ((Integer) translationLengths
								.get(id)).intValue();
						currentLength += (rs.getInt("seq_region_end") - rs
								.getInt("seq_region_start")) + 1;
						translationLengths.put(id, new Integer(currentLength));
						// inCodingRegion = false;

					}
				} // if inCoding

			} // while rs

			rs.close();
			stmt.close();

			// modify according to seqedits
			stmt = con.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
					java.sql.ResultSet.CONCUR_READ_ONLY);
			stmt.setFetchSize(1000);
			rs = stmt.executeQuery(sqlSeqEdit);
			while (rs.next()) {
				Integer translationId = rs.getInt(1);
				String edit = rs.getString(2);
				String[] vals = edit.split(" +");
				if (!vals[0].equals(vals[1])) {
					Integer len = (Integer) translationLengths
							.get(translationId);
					Integer insLen = new Integer(len + (3 * vals[2].length()));
					translationLengths.put(translationId, insLen);
				}
			}
			rs.close();
			stmt.close();

			stmt = null;
			// Re-open the statement to make sure it's GC'd
			stmt = con.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
					java.sql.ResultSet.CONCUR_READ_ONLY);
			stmt.setFetchSize(1000);

			logger
					.fine("Built translation length cache, about to look at protein features");
			// dumpTranslationLengths(con, translationLengths, 100);

			// find protein features where seq_end is > than the length of the
			// translation
			List<Integer> thisDBFeatures = CollectionUtils.createArrayList();
			rs = stmt
					.executeQuery("SELECT pf.protein_feature_id, pf.translation_id, pf.seq_end, a.logic_name, pf.hit_name "
							+ "FROM protein_feature pf join analysis a using (analysis_id)");

			while (rs.next()) {

				Integer translationID = new Integer(rs.getInt("translation_id"));
				Integer proteinFeatureID = new Integer(rs
						.getInt("protein_feature_id"));

				if (translationLengths.get(translationID) != null) {
					// some codons can only be 2 bp ?!?
					int minTranslationLength = (((Integer) translationLengths
							.get(translationID)).intValue() + 2) / 3;
					// int minTranslationLength = ((Integer)
					// translationLengths.get(translationID)).intValue();
					int fl = rs.getInt("seq_end");
					if (fl > minTranslationLength) {
						result = false;
						ReportManager
								.problem(this, con, "Protein feature "
										+ proteinFeatureID + "("
										+ rs.getString(4) + "/"
										+ rs.getString(5) + ") ends at " + fl
										+ " which is beyond the "
										+ minTranslationLength
										+ " length of the translation "
										+ translationID);
						thisDBFeatures.add(proteinFeatureID);
						// System.out.println("proteinFeatureID: " +
						// proteinFeatureID);
					}
				} else {
					ReportManager.problem(this, con, "Protein feature "
							+ proteinFeatureID
							+ " refers to non-existent translation "
							+ translationID);
				}
			}

			featuresToDelete.put(DBUtils.getShortDatabaseName(con),
					thisDBFeatures);
			if (thisDBFeatures.size() > THRESHOLD) {
				ReportManager.problem(this, con, "protein_feature table has "
						+ thisDBFeatures.size()
						+ " features that are longer than the translation");
			} else if (thisDBFeatures.size() == 0) {
				ReportManager
						.correct(this, con,
								"protein_feature_table has no features that are longer than the translation");
			} else {
				ReportManager
						.correct(
								this,
								con,
								"protein_feature_table has "
										+ thisDBFeatures.size()
										+ " features that are longer than the translation; this is less than the threshold of "
										+ THRESHOLD);
			}

			rs.close();
			stmt.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;

	}

	// ------------------------------------------
	// Implementation of Repair interface.

	/**
	 * Delete any protein features that run past the end of the translation.
	 * <strong>CAUTION! </strong>Actually deletes the features from the
	 * protein_feature table.
	 * 
	 * @param dbre
	 *            The database to use.
	 */
	public void repair(DatabaseRegistryEntry dbre) {

		Connection con = dbre.getConnection();
		String sql = setupRepairSQL(con);
		if (sql.length() == 0) {
			System.out.println("No invalid protein features were found in "
					+ DBUtils.getShortDatabaseName(con));
		} else {
			try {
				Statement stmt = con.createStatement();
				System.out.println(DBUtils.getShortDatabaseName(con));
				System.out.println(sql);
				// stmt.execute(sql);
				stmt.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
		}

	}

	/**
	 * Show which protein features would be deleted by the repair method.
	 * 
	 * @param dbre
	 *            The database to use.
	 */
	public void show(DatabaseRegistryEntry dbre) {

		System.out.println("Candidate for repair:");

		Connection con = dbre.getConnection();
		String sql = setupRepairSQL(con);
		if (sql.length() == 0) {
			System.out.println("No invalid protein features were found in "
					+ DBUtils.getShortDatabaseName(con));
		} else {
			System.out.println(DBUtils.getShortDatabaseName(con) + ": " + sql);
		}

	}

	/**
	 * Set up the SQL to delete the offending protein features.
	 * 
	 * @param con
	 *            The database connection to use.
	 * @return The SQL to delete the incorrect protein features, or "" if there
	 *         are no problems.
	 */
	private String setupRepairSQL(Connection con) {

		List<Integer> thisDBFeatures = featuresToDelete.get(DBUtils
				.getShortDatabaseName(con));

		if (thisDBFeatures == null || thisDBFeatures.size() == 0) {
			return "";
		} else {
			return "DELETE FROM protein_feature WHERE protein_feature_id IN ("
					+ StringUtils.join(thisDBFeatures, ",") + ")";
		}
	}
} // ProteinFeatureTranslationTestCase