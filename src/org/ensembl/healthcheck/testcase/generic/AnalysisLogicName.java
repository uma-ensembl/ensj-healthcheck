/*
 * Copyright [1999-2014] Wellcome Trust Sanger Institute and the EMBL-European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.ensembl.healthcheck.testcase.generic;

import java.sql.Connection;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.Team;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;
import org.ensembl.healthcheck.util.DBUtils;
import org.ensembl.healthcheck.util.Utils;

/**
 * An EnsEMBL Healthcheck test case that looks for correct analysis table
 * structure
 */

public class AnalysisLogicName extends SingleDatabaseTestCase {

  /**
   * Create the analysis table.
   */
  public AnalysisLogicName() {

    addToGroup("post_genebuild");
    addToGroup("compara-ancestral");
    addToGroup("pre-compara-handover");
    addToGroup("post-compara-handover");
    addToGroup("post-projection");

    setDescription("Check the analysis data is correct.");
    setTeamResponsible(Team.GENEBUILD);
    setSecondTeamResponsible(Team.RELEASE_COORDINATOR);

  }

  /**
   * Check the data in the analysis table.
   * 
   * @param dbre
   *          The database to use.
   * @return true if all data is there and in the correct format.
   */

  public boolean run(DatabaseRegistryEntry dbre) {

    boolean result = true;
    Connection con = dbre.getConnection();

    result &= checkLowerCase(con);

    return result;
  }


  // ---------------------------------------------------------------------

  /**
   * Check all logic names are lower case
   */

  private boolean checkLowerCase(Connection con) {

    boolean result = true;

    String[] logicNames = DBUtils
        .getColumnValues(con,
            "SELECT logic_name FROM analysis where BINARY logic_name != lower(logic_name) ");
    if (logicNames.length > 0) {
      ReportManager.problem(
          this,
          con,
          "The following logic_names are not lower case: "
              + Utils.arrayToString(logicNames, ","));
        result = false;
    }
    else {
      ReportManager.correct(this, con, "All logic names are lower case");
    }

    return result;
  }

}
