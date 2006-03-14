/*
 Copyright (C) 2003 EBI, GRL

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.ensembl.healthcheck.testcase.generic;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;

/**
 * Check for identically-named seq_regions in different co-ordinate systems.
 */

public class SeqRegionCoordSystem extends SingleDatabaseTestCase {

	/**
	 * Create a new SeqRegionCoordSystem testcase.
	 */
	public SeqRegionCoordSystem() {

		addToGroup("id_mapping");
		addToGroup("post_genebuild");
		addToGroup("release");
		setDescription("Check for identically-named seq_regions in different co-ordinate systems.");

	}

	/**
	 * Run the test.
	 * 
	 * @param dbre
	 *          The database to use.
	 * @return true if the test pased.
	 * 
	 */
	public boolean run(DatabaseRegistryEntry dbre) {

		boolean result = true;

		Connection con = dbre.getConnection();

		HashMap coordSystems = new HashMap();

		// build hash of co-ord system IDs to names & versions
		try {
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT coord_system_id, name, version FROM coord_system");
			while (rs.next()) {
				coordSystems.put(new Long(rs.getLong("coord_system_id")), rs.getString("name") + ":" + rs.getString("version"));
			}
		} catch (SQLException se) {
			se.printStackTrace();
		}

		// check each pair in turn
		Long[] coordSystemIDs = (Long[]) coordSystems.keySet().toArray(new Long[coordSystems.size()]);
		for (int i = 0; i < coordSystemIDs.length; i++) {
			for (int j = i+1; j < coordSystemIDs.length; j++) {

				String csI = (String) coordSystems.get(coordSystemIDs[i]);
				String csJ = (String) coordSystems.get(coordSystemIDs[j]);

				int same = getRowCount(con, "SELECT COUNT(*) FROM seq_region s1, seq_region s2 WHERE s1.coord_system_id=" + coordSystemIDs[i]
						+ " AND s2.coord_system_id=" + coordSystemIDs[j] + " AND s1.name = s2.name");

				if (same > 0) {

					ReportManager.problem(this, con, "Co-ordinate systems " + csI + " and " + csJ + " have " + same
							+ " identically-named seq_regions - this may cause problems for ID mapping");
					result = false;

				} else {

					ReportManager.correct(this, con, "Co-ordinate systems " + csI + " and " + csJ + " have no identically-named seq_regions");

				}

			} // j

		} // i
		
		return result;
		
	} // run

} // SeqRegionCoordsystem
