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

package org.ensembl.healthcheck.testcase;

import java.sql.*;
import java.util.*;
import org.ensembl.healthcheck.*;
import org.ensembl.healthcheck.util.*;

/**
 * An EnsEMBL Healthcheck test case which checks that the protein_feature table
 * agrees with the translation table.
 */

public class ProteinFeatureTranslationTestCase extends EnsTestCase implements Repair {
  
  // hash of lists of protein features to delete
  // key - database name
  Map featuresToDelete;
  
  /**
   * Create an ProteinFeatureTranslationTestCase that applies to a specific set of databases.
   */
  public ProteinFeatureTranslationTestCase() {
    addToGroup("db_constraints");
    featuresToDelete = new HashMap();
  }
  
  /**
   *
   * @return Result.
   */
  
  public TestResult run() {
    
    boolean result = true;
    
    DatabaseConnectionIterator it = getDatabaseConnectionIterator();
    
    // get list of transcripts
    String sql =
    "SELECT t.transcript_id, e.exon_id, tr.start_exon_id, tr.translation_id, tr.end_exon_id, tr.seq_start, tr.seq_end, e.contig_start, e.contig_end, e.sticky_rank " +
    "FROM   transcript t, exon_transcript et, exon e, translation tr " +
    "WHERE  t.transcript_id = et.transcript_id AND et.exon_id = e.exon_id AND t.translation_id = tr.translation_id " +
    "ORDER  BY t.transcript_id, et.rank, e.sticky_rank DESC";
    
    while (it.hasNext()) {
      
      try {
        
        Connection con = (Connection)it.next();
        
        // NOTE: By default the MM MySQL JDBC driver reads and stores *all* rows in the ResultSet.
        // Since this TestCase is likely to produce lots of output, we must use the "streaming"
        // mode where only one row of the ResultSet is stored at a time.
        // To do this, the following two lines are both necessary.
        // See the README file for the mm MySQL driver.
        
        Statement stmt = con.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(Integer.MIN_VALUE);
        
        Map translationLengths = new HashMap();
        
        // now calculate and store the translation lengths
        ResultSet rs = stmt.executeQuery(sql);
        rs.setFetchSize(100);
        rs.setFetchDirection(ResultSet.FETCH_FORWARD);
        
        boolean inCodingRegion = false;
        
        while(rs.next()) {
          
          int currentTranslationID = rs.getInt("translation_id");
          
          Integer id = new Integer(currentTranslationID);
          // initialise if necessary
          if (translationLengths.get(id) == null) {
            translationLengths.put(id, new Integer(0));
          }
          
          if (!inCodingRegion) {
            if (rs.getInt("start_exon_id") == rs.getInt("exon_id")) {
              // single-exon-translations
              if (rs.getInt("start_exon_id") == rs.getInt("end_exon_id")) {
                int length = (rs.getInt("seq_end") - rs.getInt("seq_start")) + 1;
                translationLengths.put(id, new Integer(length));
                continue;
              }
              inCodingRegion = true;
              // subtract seq_start
              int currentLength = ((Integer)translationLengths.get(id)).intValue();
              currentLength -= (rs.getInt("seq_start") - 1);
              translationLengths.put(id, new Integer(currentLength));
            }
          } // if !inCoding
          
          if (inCodingRegion) {
            if (rs.getInt("exon_id") == rs.getInt("end_exon_id")) {
              // add seq_end
              int currentLength = ((Integer)translationLengths.get(id)).intValue();
              currentLength += rs.getInt("seq_end");
              translationLengths.put(id, new Integer(currentLength));
              inCodingRegion = false;
            } else {
              int currentLength = ((Integer)translationLengths.get(id)).intValue();
              currentLength += (rs.getInt("contig_end") - rs.getInt("contig_start")) + 1;
              translationLengths.put(id, new Integer(currentLength));
              //inCodingRegion = false;
            }
          } // if inCoding
          
        } // while rs
        
        rs.close();
        stmt.close();
        stmt = null;
        
        // Re-open the statement to make sure it's GC'd
        stmt = con.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(Integer.MIN_VALUE);
        
        logger.fine("Built translation length cache, about to look at protein features");
        
        //dumpTranslationLengths(con, translationLengths, 100);
        
        // find protein features where seq_end is > than the length of the translation
        List thisDBFeatures = new ArrayList();
        rs = stmt.executeQuery("SELECT protein_feature_id, translation_id, seq_end FROM protein_feature");
        while (rs.next()) {
          Integer id = new Integer(rs.getInt("translation_id"));
          int minTranslationLength = (((Integer)translationLengths.get(id)).intValue() + 2) / 3; // some codons can only be 2 bp
          if (rs.getInt("seq_end")  > minTranslationLength) {
            //ReportManager.problem(this, con, "Protein feature " + rs.getInt("protein_feature_id") + " claims to have length " + rs.getInt("seq_end") + " but translation is of length " + minTranslationLength);
            result = false;
            thisDBFeatures.add(new Integer(rs.getInt("protein_feature_id")));
          }
        }
        
        featuresToDelete.put(DBUtils.getShortDatabaseName(con), thisDBFeatures);
        if (thisDBFeatures.size() > 0) {
          ReportManager.problem(this, con, "protein_feature_table has " + thisDBFeatures.size() + " features that are longer than the translation");
        } else {
          ReportManager.correct(this, con, "protein_feature_table has no features that are longer than the translation");
        }
        
        rs.close();
        stmt.close();
        
      } catch (Exception e) {
        e.printStackTrace();
      }
      
    } // while it
    
    return new TestResult(getShortTestName(), result);
    
  }
  
  // ------------------------------------------
  // Implementation of Repair interface.
  
  public void repair() {
    
    DatabaseConnectionIterator connectionIterator = getDatabaseConnectionIterator();
    while (connectionIterator.hasNext()) {
      Connection con = (Connection)connectionIterator.next();
      String sql = setupRepairSQL(con);
      try {
        Statement stmt = con.createStatement();
        System.out.println("would have executed " + sql);
        //stmt.execute(sql);
        stmt.close();
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
  }
  
  public void show() {
    
    System.out.println("Candidates for repair:");
    DatabaseConnectionIterator connectionIterator = getDatabaseConnectionIterator();
    while (connectionIterator.hasNext()) {
      Connection con = (Connection)connectionIterator.next();
      System.out.println(DBUtils.getShortDatabaseName(con) + ": " + setupRepairSQL(con));
    }
    
  }
  
  private String setupRepairSQL(Connection con) {
    
    StringBuffer sql = new StringBuffer("DELETE FROM protein_feaure WHERE protein_feature_id IN (");
    
    List thisDBFeatures = (List)featuresToDelete.get(DBUtils.getShortDatabaseName(con));
    Iterator featureIterator = thisDBFeatures.iterator();
    while (featureIterator.hasNext()) {
      sql.append(((Integer)featureIterator.next()).intValue());
      if (featureIterator.hasNext()) {
        sql.append(",");
      }
    }
    sql.append(")");
    
    
    return sql.toString();
    
  }
  
  // -------------------------------------------------------------------------
  
  private void dumpTranslationLengths(Connection con, Map lengths, int maxID) {
    
    System.out.println("Translation lengths for " + DBUtils.getShortDatabaseName(con));
    
    Set keySet = lengths.keySet();
    List keyList = new ArrayList(keySet);
    Collections.sort(keyList, new IntegerComparator());
    
    Iterator it = keyList.iterator();
    while (it.hasNext()) {
      Integer iid = (Integer)it.next();
      int id = iid.intValue();
      if (id > maxID) {
        break;
      }
      Integer iLength = (Integer)lengths.get(iid);
      int length = iLength.intValue();
      System.out.println("ID: " + id + "\tLength: " + length);
    }
    
  }
  
  // -------------------------------------------------------------------------
  
} // ProteinFeatureTranslationTestCase


