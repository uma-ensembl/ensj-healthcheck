/**
 * File: EgMeta.java
 * Created by: dstaines
 * Created on: Mar 2, 2010
 * CVS:  $$
 */
package org.ensembl.healthcheck.testcase.eg_core;

/**
 * @author dstaines
 * 
 */
public class EgMeta extends AbstractEgMeta {

	/**
	 * @param metaKeys
	 */
	public EgMeta() {
		super(
				resourceToStringList("/org/ensembl/healthcheck/testcase/eg_core/required_meta_keys.txt"));
	}

}