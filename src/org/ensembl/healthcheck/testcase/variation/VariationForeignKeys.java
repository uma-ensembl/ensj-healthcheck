/*
 Copyright (C) 2004 EBI, GRL
 
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

package org.ensembl.healthcheck.testcase.variation;

import java.sql.Connection;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.Team;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;
import org.ensembl.healthcheck.util.DBUtils;

/**
 * An EnsEMBL Healthcheck test case that looks for broken foreign-key realtionships.
 */

public class VariationForeignKeys extends SingleDatabaseTestCase {

	/**
	 * Create an OrphanTestCase that applies to a specific set of databases.
	 */
	public VariationForeignKeys() {

		// addToGroup("release"); removed to speed up cron job
		addToGroup("variation");
		addToGroup("variation-release");
		setDescription("Check for broken foreign-key relationships.");
		setTeamResponsible(Team.VARIATION);

	}

	/**
	 * Look for broken foreign key realtionships.
	 * 
	 * @param dbre
	 *          The database to use.
	 * @return true Ff all foreign key relationships are valid.
	 */
	public boolean run(DatabaseRegistryEntry dbre) {

		boolean result = true;
		int rows = 0;

		Connection con = dbre.getConnection();

		try {
			
			/*
			 * This is allowed allele can have null sample_id 
			 * result &= checkForOrphans(con, "allele", "sample_id", "sample", "sample_id",true);
			 */
			result &= checkForOrphans(con, "allele", "variation_id", "variation", "variation_id", true);
			result &= checkForOrphans(con, "compressed_genotype_region", "sample_id", "individual", "sample_id", true);
			result &= checkForOrphans(con, "failed_allele", "failed_description_id", "failed_description", "failed_description_id", true);
			result &= checkForOrphans(con, "failed_allele", "allele_id", "allele", "allele_id", true);
			result &= checkForOrphans(con, "failed_variation", "failed_description_id", "failed_description", "failed_description_id", true);
			result &= checkForOrphans(con, "failed_variation", "variation_id", "variation", "variation_id", true);
			result &= checkForOrphans(con, "failed_structural_variation", "failed_description_id", "failed_description", "failed_description_id", true);
			result &= checkForOrphans(con, "failed_structural_variation", "structural_variation_id", "structural_variation", "structural_variation_id", true);
			result &= checkForOrphans(con, "individual", "sample_id", "sample", "sample_id", true);
			result &= checkForOrphans(con, "individual_genotype_multiple_bp", "sample_id", "individual_population", "individual_sample_id", true);
			result &= checkForOrphans(con, "individual_genotype_multiple_bp", "sample_id", "sample", "sample_id", true);
			result &= checkForOrphans(con, "individual_population", "individual_sample_id", "sample", "sample_id", true);
			result &= checkForOrphans(con, "phenotype_feature", "phenotype_id", "phenotype", "phenotype_id", true);
			result &= checkForOrphans(con, "phenotype_feature", "source_id", "source", "source_id", true);
			result &= checkForOrphans(con, "phenotype_feature", "study_id", "study", "study_id", true);
			result &= checkForOrphans(con, "phenotype_feature_attrib", "phenotype_feature_id", "phenotype_feature", "phenotype_feature_id", true);
			result &= checkForOrphans(con, "phenotype_feature_attrib", "attrib_type_id", "attrib_type", "attrib_type_id", true);
			result &= checkForOrphans(con, "population", "sample_id", "sample", "sample_id", true);
			result &= checkForOrphans(con, "population_genotype", "sample_id", "sample", "sample_id", true);
			result &= checkForOrphans(con, "population_genotype", "variation_id", "variation", "variation_id", true);
			result &= checkForOrphans(con, "compressed_genotype_var", "variation_id", "variation", "variation_id", true);
			result &= checkForOrphans(con, "read_coverage", "sample_id", "sample", "sample_id", true);
			result &= checkForOrphans(con, "read_coverage", "seq_region_id", "seq_region", "seq_region_id", true);
			result &= checkForOrphans(con, "sample_synonym", "sample_id", "sample", "sample_id", true);
			result &= checkForOrphans(con, "tagged_variation_feature", "sample_id", "sample", "sample_id", true);
			/*
			 * instead check compressed_genotype_single_bp with individual table
			 * result &= checkForOrphans(con, "tmp_individual_genotype_single_bp", "variation_id", "variation", "variation_id", true);
			 * this is true only for ensembl snps
			 * result &= checkForOrphans(con, "tmp_individual_genotype_single_bp", "variation_id", "variation_feature", "variation_id",true);
			 */
			result &= checkForOrphans(con, "transcript_variation", "variation_feature_id", "variation_feature", "variation_feature_id", true);
			result &= checkForOrphans(con, "variation", "source_id", "source", "source_id", true);
			result &= checkForOrphans(con, "variation_feature", "source_id", "source", "source_id", true);
			result &= checkForOrphans(con, "variation_feature", "variation_id", "allele", "variation_id", true);
			result &= checkForOrphans(con, "variation_set_structure", "variation_set_sub", "variation_set", "variation_set_id", true);
			result &= checkForOrphans(con, "variation_set_structure", "variation_set_super", "variation_set", "variation_set_id", true);
			result &= checkForOrphans(con, "variation_set_variation", "variation_id", "variation", "variation_id", true);
			result &= checkForOrphans(con, "variation_set_variation", "variation_set_id", "variation_set", "variation_set_id", true);
			result &= checkForOrphans(con, "variation_synonym", "source_id", "source", "source_id", true);
			result &= checkForOrphans(con, "variation_synonym", "variation_id", "variation", "variation_id", true);
			result &= checkForOrphans(con, "structural_variation_feature", "structural_variation_id", "structural_variation", "structural_variation_id", true);
			result &= checkForOrphans(con, "structural_variation", "study_id", "study", "study_id", true);
			
			// alleles and genotypes
			result &= checkForOrphans(con, "allele", "allele_code_id", "allele_code", "allele_code_id", true);
			result &= checkForOrphans(con, "population_genotype", "genotype_code_id", "genotype_code", "genotype_code_id", true);
			result &= checkForOrphans(con, "genotype_code", "allele_code_id", "allele_code", "allele_code_id", true);
            
            // check phenotype_feature (special case since it can contain links to multiple tables)
            rows = countOrphansWithConstraint(con,"phenotype_feature","object_id","variation","name","type = 'Variation'");
			if (rows > 0) {
				ReportManager.problem(this, con, rows + "entries in phenotype_feature table without entries in variation");
				result = false;
			}
            
            rows = countOrphansWithConstraint(con,"phenotype_feature","object_id","structural_variation","variation_name","type IN ('StructuralVariation','SupportingStructuralVariation')");
			if (rows > 0) {
				ReportManager.problem(this, con, rows + "entries in phenotype_feature table without entries in structural_variation");
				result = false;
			}

			rows = countOrphansWithConstraint(con,"compressed_genotype_region","seq_region_id","variation_feature","seq_region_id","seq_region_start = variation_feature.seq_region_start");
			if (rows > 0) {
				ReportManager.problem(this, con, rows + "entries in Compressed genotype table without variation features");
				result = false;
			}
	
			// Hmmm.. this is not really a foreign key check.. [pontus]
			if (DBUtils.getRowCount(con, "SHOW TABLES like 'tmp_individual%'") > 0) {
				rows = DBUtils.getRowCount(con, "SELECT COUNT(*) FROM tmp_individual_genotype_single_bp where length(allele_1) >1 or length(allele_2) > 1");
				if (rows > 0) {
					ReportManager.problem(this, con, rows + "entries in The tmp_individual_genotype_single_bp table contains alleles with more than 1 bp");
					result = false;
				}
			}
			
		} catch (Exception e) {
			ReportManager.problem(this, con, "HealthCheck generated an exception: " + e.getMessage());
			result = false;
		}
		if (result) {
			// if there were no problems, just inform for the interface to pick the HC
			ReportManager.correct(this, con, "VariationForeignKeys test passed without any problem");
		}
		return result;

	}

} // VariationForeignKeys
