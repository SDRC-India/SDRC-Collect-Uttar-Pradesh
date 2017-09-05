package org.sdrc.collect.uphpmis.android.listeners;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author Ratikanta Pradhan (ratikanta@sdrc.co.in)
 *
 */
public interface SubmissionFileDownloadListener {
	void submissionFileDownloadComplete(HashMap<Integer, HashMap<InputStream, List<String>>> result);
}
